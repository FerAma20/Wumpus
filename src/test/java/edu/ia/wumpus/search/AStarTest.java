/*
 * ============================================================
 *  AStarTest.java
 *  Verifica que A* encuentre rutas óptimas (igual longitud
 *  que BFS) y que típicamente expanda MENOS nodos en un grid
 *  abierto, gracias a la heurística Manhattan.
 * ============================================================
 */
package edu.ia.wumpus.search;

import edu.ia.wumpus.core.Cell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AStarTest {

    @Test
    void findsOptimalPathOnOpenGrid() {
        SearchAlgorithm astar = new AStar();
        SearchResult r = astar.search(new Cell(0, 0), new Cell(3, 3), c -> true, 4);
        assertTrue(r.found());
        assertEquals(7, r.path().size()); // óptima
    }

    @Test
    void aStarExpandsFewerNodesThanBfsOnOpenGrid() {
        SearchAlgorithm bfs = new BFS();
        SearchAlgorithm astar = new AStar();
        SearchResult rb = bfs.search(new Cell(0, 0),   new Cell(5, 5), c -> true, 6);
        SearchResult ra = astar.search(new Cell(0, 0), new Cell(5, 5), c -> true, 6);
        assertTrue(rb.found() && ra.found());
        assertEquals(rb.path().size(), ra.path().size(),
                "BFS y A* deben encontrar rutas de igual longitud");
        assertTrue(ra.expanded() <= rb.expanded(),
                "A* debería expandir <= que BFS en un grid abierto");
    }

    @Test
    void noRouteReturnsEmptyPath() {
        SearchAlgorithm astar = new AStar();
        // Encierra (0,0) con bloqueos en sus dos vecinas
        SearchResult r = astar.search(new Cell(0, 0), new Cell(3, 3),
                c -> !(c.equals(new Cell(1, 0)) || c.equals(new Cell(0, 1))), 4);
        assertFalse(r.found());
    }
}
