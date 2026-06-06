/*
 * ============================================================
 *  AStar.java
 *  --------------------------------------------------------
 *  Búsqueda A*: algoritmo de búsqueda INFORMADA que combina
 *      g(n) — coste real desde el inicio
 *      h(n) — heurística admisible al objetivo
 *      f(n) = g(n) + h(n)
 *
 *  Heurística usada: distancia Manhattan |Δx| + |Δy|.
 *  En grids 4-conectados con coste uniforme es admisible y
 *  consistente → A* devuelve la ruta óptima expandiendo el
 *  mínimo número teórico de nodos.
 *
 *  Aplicación en este proyecto:
 *      - Retorno óptimo del agente a (1,1) tras tomar el oro.
 *      - Alternativa más eficiente que BFS para exploración
 *        dirigida a celdas concretas.
 * ============================================================
 */
package edu.ia.wumpus.search;

import edu.ia.wumpus.core.Cell;
import edu.ia.wumpus.core.Direction;

import java.util.*;
import java.util.function.Predicate;

public final class AStar implements SearchAlgorithm {

    @Override
    public String name() { return "A*"; }

    @Override
    public SearchResult search(Cell start, Cell goal, Predicate<Cell> passable, int size) {
        if (start.equals(goal)) {
            return new SearchResult(List.of(start), 1);
        }

        Map<Cell, Cell> parent = new HashMap<>();
        Map<Cell, Integer> gScore = new HashMap<>();
        gScore.put(start, 0);

        // PriorityQueue ordenada por f(n) ascendente; desempata por g(n).
        PriorityQueue<Node> open = new PriorityQueue<>(
                Comparator.<Node>comparingInt(n -> n.f).thenComparingInt(n -> n.g));
        open.add(new Node(start, 0, start.manhattanTo(goal)));
        int expanded = 0;

        while (!open.isEmpty()) {
            Node cur = open.poll();
            expanded++;
            if (cur.cell.equals(goal)) {
                return new SearchResult(reconstruct(parent, start, goal), expanded);
            }
            for (Direction d : Direction.values()) {
                Cell n = d.stepFrom(cur.cell);
                if (!n.inside(size)) continue;
                if (!n.equals(goal) && !passable.test(n)) continue;

                int tentativeG = cur.g + 1;
                if (tentativeG < gScore.getOrDefault(n, Integer.MAX_VALUE)) {
                    parent.put(n, cur.cell);
                    gScore.put(n, tentativeG);
                    int h = n.manhattanTo(goal);
                    open.add(new Node(n, tentativeG, tentativeG + h));
                }
            }
        }
        return new SearchResult(List.of(), expanded);
    }

    /** Nodo interno de la frontera; encapsula celda, g y f para no recalcular. */
    private static final class Node {
        final Cell cell;
        final int g;
        final int f;
        Node(Cell cell, int g, int f) { this.cell = cell; this.g = g; this.f = f; }
    }

    /** Reconstruye el camino siguiendo parent[child]=padre desde goal hasta start. */
    private static List<Cell> reconstruct(Map<Cell, Cell> parent, Cell start, Cell goal) {
        LinkedList<Cell> path = new LinkedList<>();
        Cell cur = goal;
        while (cur != null && !cur.equals(start)) {
            path.addFirst(cur);
            cur = parent.get(cur);
        }
        path.addFirst(start);
        return path;
    }
}
