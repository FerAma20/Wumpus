/*
 * ============================================================
 *  CellTest.java
 *  Pruebas unitarias del record Cell:
 *    - límites de tablero (inside)
 *    - distancia Manhattan
 *    - igualdad por valor (es un record)
 * ============================================================
 */
package edu.ia.wumpus.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CellTest {

    @Test
    void inside_acceptsCellsWithinBoard() {
        assertTrue(new Cell(0, 0).inside(4));
        assertTrue(new Cell(3, 3).inside(4));
    }

    @Test
    void inside_rejectsCellsOutOfBoard() {
        assertFalse(new Cell(-1, 0).inside(4));
        assertFalse(new Cell(4, 0).inside(4));
        assertFalse(new Cell(0, 4).inside(4));
    }

    @Test
    void manhattan_isSymmetricAndZeroForSameCell() {
        Cell a = new Cell(1, 2);
        Cell b = new Cell(4, 6);
        assertEquals(0, a.manhattanTo(a));
        assertEquals(a.manhattanTo(b), b.manhattanTo(a));
        assertEquals(7, a.manhattanTo(b));
    }

    @Test
    void records_areValueEqual() {
        assertEquals(new Cell(2, 3), new Cell(2, 3));
        assertEquals(new Cell(2, 3).hashCode(), new Cell(2, 3).hashCode());
    }
}
