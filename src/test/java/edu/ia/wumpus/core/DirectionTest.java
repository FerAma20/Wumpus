/*
 * ============================================================
 *  DirectionTest.java
 *  Verifica los giros y el avance unitario en cada orientación.
 * ============================================================
 */
package edu.ia.wumpus.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DirectionTest {

    @Test
    void turnLeft_rotatesCounterClockwise() {
        assertEquals(Direction.WEST,  Direction.NORTH.turnLeft());
        assertEquals(Direction.SOUTH, Direction.WEST.turnLeft());
        assertEquals(Direction.EAST,  Direction.SOUTH.turnLeft());
        assertEquals(Direction.NORTH, Direction.EAST.turnLeft());
    }

    @Test
    void turnRight_rotatesClockwise() {
        assertEquals(Direction.EAST,  Direction.NORTH.turnRight());
        assertEquals(Direction.SOUTH, Direction.EAST.turnRight());
        assertEquals(Direction.WEST,  Direction.SOUTH.turnRight());
        assertEquals(Direction.NORTH, Direction.WEST.turnRight());
    }

    @Test
    void stepFrom_appliesUnitVector() {
        Cell origin = new Cell(2, 2);
        assertEquals(new Cell(2, 3), Direction.NORTH.stepFrom(origin));
        assertEquals(new Cell(3, 2), Direction.EAST.stepFrom(origin));
        assertEquals(new Cell(2, 1), Direction.SOUTH.stepFrom(origin));
        assertEquals(new Cell(1, 2), Direction.WEST.stepFrom(origin));
    }

    @Test
    void fullCircleOfRightTurnsReturnsToOriginal() {
        Direction d = Direction.NORTH;
        for (int i = 0; i < 4; i++) d = d.turnRight();
        assertEquals(Direction.NORTH, d);
    }
}
