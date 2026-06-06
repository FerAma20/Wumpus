/*
 * ============================================================
 *  Planner.java
 *  --------------------------------------------------------
 *  Componente que TRADUCE el estado del agente + KB en un PLAN
 *  (lista de celdas a recorrer). Funciona en 3 fases:
 *
 *      1. Decidir el OBJETIVO según la situación:
 *         - Si tiene oro → salir por (0,0)
 *         - Si hay segura no visitada → explorarla
 *         - Si no hay seguras → arriesgar en la celda menos peligrosa
 *
 *      2. Encontrar un CAMINO con el SearchAlgorithm inyectado
 *         (BFS, A*…). Sólo transitan celdas SEGURAS.
 *
 *      3. Si arriesga, permitir que el goal sea no-seguro
 *         (sólo el destino, nunca un paso intermedio).
 *
 *  El plan resultante es CONSUMIDO PASO A PASO por el Agent —
 *  esto permite que la KB se actualice tras cada percepción y
 *  potencialmente cancele/replanifique en el siguiente turno.
 * ============================================================
 */
package edu.ia.wumpus.search;

import edu.ia.wumpus.core.Cell;
import edu.ia.wumpus.knowledge.KnowledgeBase;

import java.util.*;

public final class Planner {

    public enum Intent { EXPLORE, RETURN_HOME, TAKE_RISK, STUCK }

    /** Resultado completo de un ciclo de planificación. */
    public record Plan(Intent intent, Cell goal, List<Cell> path, int expanded,
                       String algo, List<Cell> explored) {
        public boolean hasPath() { return path != null && path.size() >= 2; }
    }

    private final SearchAlgorithm searchAlgo;

    public Planner(SearchAlgorithm searchAlgo) {
        this.searchAlgo = searchAlgo;
    }

    /**
     * Planifica el siguiente movimiento.
     * @param current   posición actual del agente
     * @param kb        base de conocimiento (sólo lectura)
     * @param hasGold   true si el agente ya recogió el oro
     */
    public Plan plan(Cell current, KnowledgeBase kb, boolean hasGold) {
        int size = kb.getSize();

        // 1. Caso "tengo oro" → ir a (0,0)
        if (hasGold) {
            Cell home = new Cell(0, 0);
            SearchResult r = searchAlgo.search(current, home, kb::isSafe, size);
            return new Plan(Intent.RETURN_HOME, home, r.path(), r.expanded(), searchAlgo.name(), r.explored());
        }

        // 2. Caso "hay segura no visitada" → ir a la más cercana en Manhattan
        Cell target = nearestSafeUnvisited(current, kb);
        if (target != null) {
            SearchResult r = searchAlgo.search(current, target, kb::isSafe, size);
            return new Plan(Intent.EXPLORE, target, r.path(), r.expanded(), searchAlgo.name(), r.explored());
        }

        // 3. Caso "asumir riesgo": elegir la celda no visitada con menor peligro
        Cell risky = leastRiskyUnvisited(current, kb);
        if (risky != null) {
            // Permitir el riesgo SOLO como destino, no como paso intermedio
            final Cell riskyFinal = risky;
            SearchResult r = searchAlgo.search(current, risky,
                    c -> kb.isSafe(c) || c.equals(riskyFinal), size);
            return new Plan(Intent.TAKE_RISK, risky, r.path(), r.expanded(), searchAlgo.name(), r.explored());
        }

        // 4. No queda nada por hacer
        return new Plan(Intent.STUCK, current, List.of(current), 0, searchAlgo.name(), List.of());
    }

    /** Celda segura no visitada más cercana en Manhattan, o null si no hay. */
    private Cell nearestSafeUnvisited(Cell from, KnowledgeBase kb) {
        Cell best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Cell c : kb.getSafe()) {
            if (kb.isVisited(c)) continue;
            int d = from.manhattanTo(c);
            if (d < bestDist) { bestDist = d; best = c; }
        }
        return best;
    }

    /**
     * Si no hay ruta segura, escoger la celda no visitada con MENOR riesgo:
     *   score = 10 * (#peligros posibles) + distancia Manhattan
     * Excluye celdas DEMOSTRADAS como hoyo/Wumpus.
     */
    private Cell leastRiskyUnvisited(Cell from, KnowledgeBase kb) {
        int size = kb.getSize();
        Cell best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                Cell c = new Cell(x, y);
                if (kb.isVisited(c)) continue;
                if (kb.isPit(c) || kb.isWumpus(c)) continue;
                boolean isMaybe = kb.isMaybePit(c) || kb.isMaybeWumpus(c);
                if (!isMaybe) continue;       // sin evidencia para ninguno → ya estaría en safe
                int danger = (kb.isMaybePit(c) ? 1 : 0) + (kb.isMaybeWumpus(c) ? 1 : 0);
                int score = danger * 10 + from.manhattanTo(c);
                if (score < bestScore) { bestScore = score; best = c; }
            }
        }
        return best;
    }
}
