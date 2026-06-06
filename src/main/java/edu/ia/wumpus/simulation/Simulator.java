/*
 * ============================================================
 *  Simulator.java
 *  --------------------------------------------------------
 *  Orquesta el ciclo de simulación:
 *
 *      while (running):
 *          percept   = world.getPercepts(agent.pos)
 *          agent.perceive(percept)              ─┐
 *          plan      = agent.think()             ├── lógica del agente
 *          action    = elegirAcción(plan)        ─┘
 *          aplicar(action) sobre world + agent  ─── física del mundo
 *          actualizar score, comprobar fin
 *
 *  En este proyecto el "plan" entregado por el agente devuelve
 *  un camino de celdas. Como mantenemos un modelo de orientación
 *  realista (NORTH/EAST/SOUTH/WEST), traducimos cada paso del
 *  camino a la secuencia atómica TURN_*..FORWARD que lo realiza.
 *
 *  Toda la "verdad" (caídas en hoyo, muerte, oro recogido)
 *  ocurre AQUÍ, no en el agente. Eso permite tests
 *  deterministas y mantiene la KB del agente honesta.
 * ============================================================
 */
package edu.ia.wumpus.simulation;

import edu.ia.wumpus.agent.Agent;
import edu.ia.wumpus.agent.Scorer;
import edu.ia.wumpus.core.*;
import edu.ia.wumpus.environment.World;
import edu.ia.wumpus.search.Planner;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class Simulator {

    private final World world;
    private final Agent agent;
    private final Scorer scorer = new Scorer();
    private final int maxTurns;
    private final Consumer<String> logger;   // por defecto System.out::println — inyectable para tests

    private int turn = 0;
    private GameResult result = GameResult.RUNNING;
    private edu.ia.wumpus.search.Planner.Plan lastPlan; // último plan calculado (para resaltar la ruta en la UI)

    public Simulator(World world, Agent agent) {
        this(world, agent, 500, System.out::println);
    }

    public Simulator(World world, Agent agent, int maxTurns, Consumer<String> logger) {
        this.world = world;
        this.agent = agent;
        this.maxTurns = maxTurns;
        this.logger = logger;
    }

    public int getTurn()         { return turn; }
    public int getScore()        { return scorer.get(); }
    public GameResult getResult() { return result; }
    public World getWorld()      { return world; }
    public Agent getAgent()      { return agent; }
    /** Último plan calculado por el agente — usado por la UI para dibujar la ruta. */
    public edu.ia.wumpus.search.Planner.Plan getLastPlan() { return lastPlan; }

    /** Ejecuta turnos hasta que el agente termine (gane, muera, se bloquee o se acabe el tiempo). */
    public GameResult runToCompletion() {
        while (result == GameResult.RUNNING) step();
        return result;
    }

    /** Avanza un solo turno. */
    public GameResult step() {
        if (result != GameResult.RUNNING) return result;
        turn++;

        // 1. Percepción
        Percept p = world.getPercepts(agent.getPos());
        log("T%02d  %s en %s mirando %s  %s",
                turn, "Agente", agent.getPos().human(), agent.getDir(), p.compact());
        agent.perceive(p);

        // 2. Acciones implícitas: agarrar oro / trepar
        if (p.glitter() && !agent.hasGold()) {
            agent.pickGold();
            world.grabGold();
            scorer.onAction();
            scorer.onGoldGrabbed();
            log("       Acción: AGARRAR — oro obtenido (+1000)");
            return result;
        }
        if (agent.hasGold() && agent.getPos().equals(new Cell(0, 0))) {
            agent.escape();
            scorer.onAction();
            result = GameResult.WIN;
            log("       Acción: TREPAR en (1,1) — VICTORIA");
            return result;
        }

        // 3. Planificar
        Planner.Plan plan = agent.think();
        this.lastPlan = plan;
        if (plan.intent() == Planner.Intent.STUCK || !plan.hasPath()) {
            result = GameResult.STUCK;
            log("       Sin plan factible — BLOQUEADO. Algoritmo: %s", plan.algo());
            return result;
        }
        log("       Plan[%s, %s → %s] expanded=%d", plan.algo(), plan.intent(), plan.goal().human(), plan.expanded());

        // 4. Ejecutar UN paso del camino (sigueint cell del path)
        Cell next = plan.path().get(1);
        Direction needed = directionTo(agent.getPos(), next);
        // 4a. Girar tantos turnos como sea necesario
        while (agent.getDir() != needed) {
            // Elegir el giro más corto (si ya está al revés, gira derecha — arbitrario, da igual)
            if (agent.getDir().turnLeft() == needed) {
                agent.turnLeft();  scorer.onAction();
                log("       Acción: GIRAR_IZQ → mira %s", agent.getDir());
            } else {
                agent.turnRight(); scorer.onAction();
                log("       Acción: GIRAR_DER → mira %s", agent.getDir());
            }
        }
        // 4b. Avanzar
        agent.moveTo(next);
        scorer.onAction();
        log("       Acción: AVANZAR a %s", next.human());

        // 5. Consecuencias del entorno
        if (world.isDeadly(next)) {
            agent.die();
            scorer.onDeath();
            result = GameResult.DEAD;
            String reason = world.getPits().contains(next) ? "HOYO" : "WUMPUS";
            log("       ✗ Muerte por %s en %s (−1000)", reason, next.human());
            return result;
        }

        if (turn >= maxTurns) {
            result = GameResult.MAX_TURNS;
            log("       Límite de turnos alcanzado");
        }
        return result;
    }

    // ============================================================
    //  MODO MANUAL — el USUARIO decide adónde mover al agente.
    //  Cada clic ejecuta exactamente las mismas validaciones y
    //  procesos del modo automático (percibir → inferir → puntuar
    //  → comprobar muerte/oro), pero el DESTINO lo elige la persona.
    // ============================================================

    /** Resultado de un intento de movimiento manual — para dar feedback en la UI. */
    public enum MoveStatus {
        OK,            // movimiento válido, agente sigue vivo
        GOLD,          // se movió y además recogió el oro
        NOT_ADJACENT,  // la celda no es vecina de la posición actual → ignorado
        OUT_OF_BOUNDS, // fuera del tablero → ignorado
        DIED,          // pisó hoyo o Wumpus → fin de partida
        GAME_OVER      // la partida ya había terminado
    }

    /**
     * Percibe la celda ACTUAL sin moverse. Se llama una vez al iniciar
     * el modo manual para que el agente "vea" su casilla de salida (1,1)
     * y la KB marque de inmediato las vecinas seguras.
     */
    public void perceiveCurrent() {
        Percept p = world.getPercepts(agent.getPos());
        log("Inicio  Agente en %s  %s", agent.getPos().human(), p.compact());
        agent.perceive(p);
        // Si por casualidad el oro estuviera en la salida
        if (p.glitter() && !agent.hasGold()) {
            agent.pickGold();
            world.grabGold();
            scorer.onGoldGrabbed();
            log("        AGARRAR — oro obtenido (+1000)");
        }
    }

    /**
     * Intenta mover el agente a la celda `target` elegida por el usuario.
     * Validaciones:
     *   - la partida debe seguir en curso
     *   - `target` debe estar dentro del tablero
     *   - `target` debe ser ADYACENTE (4-conectada) a la posición actual
     * Proceso (idéntico al modo automático):
     *   girar+avanzar (puntúa cada acción) → comprobar muerte →
     *   percibir nueva celda → inferir → recoger oro si hay resplandor.
     */
    public MoveStatus manualMove(Cell target) {
        if (result != GameResult.RUNNING) return MoveStatus.GAME_OVER;
        if (!target.inside(world.getSize())) return MoveStatus.OUT_OF_BOUNDS;
        if (agent.getPos().manhattanTo(target) != 1) return MoveStatus.NOT_ADJACENT;

        turn++;

        // 1. Girar hacia el destino (cada giro cuesta una acción) y avanzar
        Direction needed = directionTo(agent.getPos(), target);
        while (agent.getDir() != needed) {
            if (agent.getDir().turnLeft() == needed) { agent.turnLeft();  }
            else                                      { agent.turnRight(); }
            scorer.onAction();
        }
        agent.moveTo(target);
        scorer.onAction();
        log("T%02d  AVANZAR a %s", turn, target.human());

        // 2. ¿Consecuencia fatal? (el usuario asumió el riesgo)
        if (world.isDeadly(target)) {
            agent.die();
            scorer.onDeath();
            result = GameResult.DEAD;
            String reason = world.getPits().contains(target) ? "HOYO" : "WUMPUS";
            log("     ✗ Muerte por %s en %s (−1000)", reason, target.human());
            return MoveStatus.DIED;
        }

        // 3. Percepción + inferencia sobre la nueva celda
        Percept p = world.getPercepts(target);
        log("     Percibido %s  %s", target.human(), p.compact());
        agent.perceive(p);

        // 4. Oro
        if (p.glitter() && !agent.hasGold()) {
            agent.pickGold();
            world.grabGold();
            scorer.onGoldGrabbed();
            log("     AGARRAR — oro obtenido (+1000)");
            return MoveStatus.GOLD;
        }
        return MoveStatus.OK;
    }

    /**
     * El usuario ordena TREPAR para salir. Sólo válido en la casilla de
     * salida (1,1). Si lleva el oro consigo → VICTORIA; si no, igualmente
     * sale pero sin recompensa (partida terminada).
     * @return true si la acción fue válida (estaba en la salida).
     */
    public boolean manualClimb() {
        if (result != GameResult.RUNNING) return false;
        if (!agent.getPos().equals(new Cell(0, 0))) return false;
        turn++;
        scorer.onAction();
        agent.escape();
        result = GameResult.WIN;
        log("T%02d  TREPAR en (1,1) — %s", turn,
                agent.hasGold() ? "VICTORIA con el oro" : "salida sin oro");
        return true;
    }

    /** Calcula la dirección unitaria para ir de `from` a `to` (deben ser adyacentes). */
    private static Direction directionTo(Cell from, Cell to) {
        int dx = to.x() - from.x();
        int dy = to.y() - from.y();
        if (dx == 1  && dy == 0) return Direction.EAST;
        if (dx == -1 && dy == 0) return Direction.WEST;
        if (dx == 0  && dy == 1) return Direction.NORTH;
        if (dx == 0  && dy == -1) return Direction.SOUTH;
        throw new IllegalArgumentException("Celdas no adyacentes: " + from + " → " + to);
    }

    /** printf-style hacia el logger inyectado. */
    private void log(String fmt, Object... args) {
        logger.accept(String.format(fmt, args));
    }
}
