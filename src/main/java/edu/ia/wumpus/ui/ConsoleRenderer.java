/*
 * ============================================================
 *  ConsoleRenderer.java
 *  --------------------------------------------------------
 *  Pinta el estado actual del mundo y la KB del agente como
 *  ASCII art para consola. Pensado para ejecutar `Main` y
 *  ver la simulación turno a turno sin necesidad de UI gráfica.
 *
 *  LEYENDA por celda (4 caracteres):
 *    A       agente
 *    .       casilla visitada
 *    s       casilla segura inferida, no visitada
 *    ?       sospechosa
 *    P       hoyo confirmado
 *    W       Wumpus confirmado
 *    *       oro (sólo en modo reveal)
 *    +       agente con oro
 *    ' '     desconocida
 *
 *  El modo "reveal" (modo dios) imprime un segundo tablero
 *  con la verdad subyacente del mundo.
 * ============================================================
 */
package edu.ia.wumpus.ui;

import edu.ia.wumpus.agent.Agent;
import edu.ia.wumpus.core.Cell;
import edu.ia.wumpus.environment.World;
import edu.ia.wumpus.knowledge.KnowledgeBase;

public final class ConsoleRenderer {

    /** Imprime el tablero tal como el agente lo CONOCE (sólo KB). */
    public static void renderAgentView(Agent agent) {
        KnowledgeBase kb = agent.getKB();
        int n = kb.getSize();
        StringBuilder sb = new StringBuilder();
        sb.append("Agente: ").append(agent.getPos().human())
          .append("  mira ").append(agent.getDir())
          .append(agent.hasGold() ? "  [+ORO]" : "")
          .append('\n');
        for (int y = n - 1; y >= 0; y--) {
            sb.append(String.format(" %2d │", y + 1));
            for (int x = 0; x < n; x++) {
                Cell c = new Cell(x, y);
                sb.append(' ').append(glyphAgentView(c, kb, agent)).append(' ');
            }
            sb.append("\n");
        }
        sb.append("    └").append("───".repeat(n)).append('\n');
        sb.append("      ");
        for (int x = 0; x < n; x++) sb.append(String.format("%2d ", x + 1));
        sb.append('\n');
        System.out.println(sb);
    }

    /** Imprime el tablero con la verdad del mundo (sólo para depuración/demo). */
    public static void renderTruth(World world, Agent agent) {
        int n = world.getSize();
        StringBuilder sb = new StringBuilder("Verdad del mundo:\n");
        for (int y = n - 1; y >= 0; y--) {
            sb.append(String.format(" %2d │", y + 1));
            for (int x = 0; x < n; x++) {
                Cell c = new Cell(x, y);
                sb.append(' ').append(glyphTruth(c, world, agent)).append(' ');
            }
            sb.append('\n');
        }
        sb.append("    └").append("───".repeat(n)).append('\n');
        sb.append("      ");
        for (int x = 0; x < n; x++) sb.append(String.format("%2d ", x + 1));
        sb.append('\n');
        System.out.println(sb);
    }

    /** Devuelve el glifo correspondiente al estado de la KB en `c`. */
    private static char glyphAgentView(Cell c, KnowledgeBase kb, Agent agent) {
        if (agent.getPos().equals(c)) return agent.hasGold() ? '+' : 'A';
        if (kb.isVisited(c))          return '.';
        if (kb.isPit(c))              return 'P';
        if (kb.isWumpus(c))           return 'W';
        if (kb.isSafe(c))             return 's';
        if (kb.isMaybePit(c) || kb.isMaybeWumpus(c)) return '?';
        return ' ';
    }

    /** Devuelve el glifo correspondiente a la realidad del mundo en `c`. */
    private static char glyphTruth(Cell c, World world, Agent agent) {
        if (agent.getPos().equals(c)) return agent.hasGold() ? '+' : 'A';
        if (world.getPits().contains(c)) return 'P';
        if (world.isWumpusAlive() && c.equals(world.getWumpus())) return 'W';
        if (world.getGold() != null && c.equals(world.getGold()))  return '*';
        return '.';
    }
}
