/*
 * ============================================================
 *  Agent.java
 *  --------------------------------------------------------
 *  Agente lógico que recorre el Mundo de Wumpus.
 *
 *  ESTADO interno:
 *      - posición y orientación
 *      - KB (Base de Conocimiento)
 *      - banderas: tiene oro, flecha disponible, vivo, escapó
 *
 *  CICLO DE TURNO (método step):
 *      1. Percibir el entorno
 *      2. tell(KB, percepción) + correr InferenceEngine
 *      3. Si glitter en la celda actual → GRAB
 *      4. Si tiene oro y está en (0,0) → CLIMB
 *      5. Planear (Planner) con el SearchAlgorithm activo
 *      6. Ejecutar UN paso del plan; el simulador
 *         materializa la consecuencia real
 *
 *  El agente NO toca el World — toda interacción pasa por
 *  Simulator. Esto separa "lo que el agente cree" (KB) de
 *  "lo que ocurre" (entorno) — y permite testear el agente
 *  contra mundos sintéticos sin física real.
 * ============================================================
 */
package edu.ia.wumpus.agent;

import edu.ia.wumpus.core.*;
import edu.ia.wumpus.environment.World;
import edu.ia.wumpus.knowledge.InferenceEngine;
import edu.ia.wumpus.knowledge.KnowledgeBase;
import edu.ia.wumpus.search.Planner;
import edu.ia.wumpus.search.SearchAlgorithm;

public final class Agent {

    private Cell pos = new Cell(0, 0);
    private Direction dir = Direction.EAST;
    private boolean hasGold = false;
    private boolean hasArrow = true;
    private boolean alive = true;
    private boolean escaped = false;

    private final KnowledgeBase kb;
    private final InferenceEngine inference;
    private final Planner planner;

    public Agent(int size, SearchAlgorithm searchAlgo, World worldForNeighbors) {
        this.kb = new KnowledgeBase(size);
        this.inference = new InferenceEngine(kb, worldForNeighbors);
        this.planner = new Planner(searchAlgo);
    }

    // ---------- getters / setters ----------
    public Cell getPos()           { return pos; }
    public Direction getDir()      { return dir; }
    public boolean hasGold()       { return hasGold; }
    public boolean hasArrow()      { return hasArrow; }
    public boolean isAlive()       { return alive; }
    public boolean hasEscaped()    { return escaped; }
    public KnowledgeBase getKB()   { return kb; }

    public void moveTo(Cell c)            { this.pos = c; }
    public void turnLeft()                { this.dir = dir.turnLeft(); }
    public void turnRight()               { this.dir = dir.turnRight(); }
    public void pickGold()                { this.hasGold = true; }
    public void consumeArrow()            { this.hasArrow = false; }
    public void die()                     { this.alive = false; }
    public void escape()                  { this.escaped = true; }

    /**
     * Procesa una percepción: la incorpora a la KB y corre inferencia
     * hasta punto fijo. Después de este método la KB refleja TODO lo
     * que el agente puede deducir lógicamente dado lo visto hasta ahora.
     */
    public void perceive(Percept p) {
        kb.tell(pos, p, neighborsOf(pos));
        // Si escuchamos un grito, el Wumpus está muerto
        if (p.scream()) kb.wumpusKilled();
        inference.run();
    }

    /**
     * Decide qué hacer DADO el estado actual de la KB.
     * Se llama después de perceive(). Devuelve el plan completo;
     * el Simulator se encarga de materializar sólo el primer paso.
     */
    public Planner.Plan think() {
        return planner.plan(pos, kb, hasGold);
    }

    /** Helper para el motor de inferencia: vecinas de una celda dentro del tablero. */
    private Iterable<Cell> neighborsOf(Cell c) {
        java.util.List<Cell> out = new java.util.ArrayList<>(4);
        for (Direction d : Direction.values()) {
            Cell n = d.stepFrom(c);
            if (n.inside(kb.getSize())) out.add(n);
        }
        return out;
    }
}
