/*
 * ============================================================
 *  BFSTest.java
 *  Verifica que BFS encuentre la ruta más corta en un grid
 *  con celdas bloqueadas, y que devuelva camino vacío cuando
 *  no exista solución.
 * ============================================================
 */
package edu.ia.wumpus.search;

import edu.ia.wumpus.core.Cell;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class BFSTest {

    @Test
    void findsShortestPathOnOpenGrid() {
        SearchAlgorithm bfs = new BFS();
        SearchResult r = bfs.search(new Cell(0, 0), new Cell(3, 3), c -> true, 4);
        assertTrue(r.found());
        // Manhattan = 6 → la ruta debe tener 7 nodos (start incluido)
        assertEquals(7, r.path().size());
    }

    @Test
    void avoidsBlockedCells() {
        SearchAlgorithm bfs = new BFS();
        Set<Cell> blocked = Set.of(new Cell(1, 0), new Cell(1, 1));
        Predicate<Cell> passable = c -> !blocked.contains(c);
        SearchResult r = bfs.search(new Cell(0, 0), new Cell(3, 0), passable, 4);
        assertTrue(r.found());
        for (Cell c : r.path()) assertFalse(blocked.contains(c));
    }

    @Test
    void returnsEmptyPathWhenNoRoute() {
        SearchAlgorithm bfs = new BFS();
        // Bloquear las dos únicas salidas de (0,0)
        Set<Cell> blocked = Set.of(new Cell(1, 0), new Cell(0, 1));
        SearchResult r = bfs.search(new Cell(0, 0), new Cell(3, 3),
                c -> !blocked.contains(c), 4);
        assertFalse(r.found());
    }

    @Test
    void startEqualsGoal_returnsSingleCellPath() {
        SearchAlgorithm bfs = new BFS();
        SearchResult r = bfs.search(new Cell(2, 2), new Cell(2, 2), c -> true, 4);
        assertTrue(r.found());
        assertEquals(1, r.path().size());
    }
}
