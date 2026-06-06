/*
 * ============================================================
 *  BFS.java
 *  --------------------------------------------------------
 *  Búsqueda en Anchura (Breadth-First Search). Algoritmo de
 *  búsqueda NO INFORMADA: explora por capas, garantizando la
 *  ruta más corta en número de pasos cuando el costo es
 *  uniforme — que es exactamente nuestro caso (cada paso = 1).
 *
 *  Complejidad: O(b^d) en tiempo y espacio.
 *  Aplicación en este proyecto:
 *      - Selección de la siguiente celda segura no visitada
 *        más cercana al agente durante la exploración.
 * ============================================================
 */
package edu.ia.wumpus.search;

import edu.ia.wumpus.core.Cell;
import edu.ia.wumpus.core.Direction;

import java.util.*;
import java.util.function.Predicate;

public final class BFS implements SearchAlgorithm {

    @Override
    public String name() { return "BFS"; }

    @Override
    public SearchResult search(Cell start, Cell goal, Predicate<Cell> passable, int size) {
        if (start.equals(goal)) {
            return new SearchResult(List.of(start), 1, List.of(start));
        }
        Map<Cell, Cell> parent = new HashMap<>();
        parent.put(start, null);
        Deque<Cell> queue = new ArrayDeque<>();
        queue.add(start);
        List<Cell> explored = new ArrayList<>();  // celdas alcanzadas (para visualizar la búsqueda)
        explored.add(start);
        int expanded = 0;

        while (!queue.isEmpty()) {
            Cell cur = queue.poll();
            expanded++;
            if (cur.equals(goal)) {
                return new SearchResult(reconstruct(parent, goal), expanded, explored);
            }
            for (Direction d : Direction.values()) {
                Cell n = d.stepFrom(cur);
                if (!n.inside(size)) continue;
                if (parent.containsKey(n)) continue;
                // El destino siempre es alcanzable; lo demás debe ser "passable".
                if (!n.equals(goal) && !passable.test(n)) continue;
                parent.put(n, cur);
                queue.add(n);
                explored.add(n);
            }
        }
        return new SearchResult(List.of(), expanded, explored);
    }

    /** Reconstruye el camino siguiendo el mapa parent[child]=padre desde goal hasta start. */
    private static List<Cell> reconstruct(Map<Cell, Cell> parent, Cell goal) {
        LinkedList<Cell> path = new LinkedList<>();
        Cell cur = goal;
        while (cur != null) {
            path.addFirst(cur);
            cur = parent.get(cur);
        }
        return path;
    }
}
