/*
 * ============================================================
 *  WorldGeneratorTest.java
 *  - Mundos con la misma semilla son idénticos (determinismo).
 *  - Mundos generados respetan las casillas protegidas alrededor
 *    de (0,0).
 * ============================================================
 */
package edu.ia.wumpus.environment;

import edu.ia.wumpus.core.Cell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldGeneratorTest {

    @Test
    void sameSeedProducesSameWorld() {
        World a = new WorldGenerator(6, 0.2, 12345L).generate();
        World b = new WorldGenerator(6, 0.2, 12345L).generate();
        assertEquals(a.getPits(),    b.getPits());
        assertEquals(a.getWumpus(),  b.getWumpus());
        assertEquals(a.getGold(),    b.getGold());
    }

    @Test
    void startAndAdjacentCellsNeverContainHazards() {
        for (long seed = 0; seed < 30; seed++) {
            World w = new WorldGenerator(4, 0.4, seed).generate();
            assertFalse(w.getPits().contains(new Cell(0, 0)));
            assertFalse(w.getPits().contains(new Cell(1, 0)));
            assertFalse(w.getPits().contains(new Cell(0, 1)));
            assertNotEquals(new Cell(0, 0), w.getWumpus());
            assertNotEquals(new Cell(1, 0), w.getWumpus());
            assertNotEquals(new Cell(0, 1), w.getWumpus());
        }
    }

    @Test
    void wumpusAndGoldNeverShareCell() {
        for (long seed = 0; seed < 30; seed++) {
            World w = new WorldGenerator(6, 0.2, seed).generate();
            assertNotEquals(w.getWumpus(), w.getGold());
        }
    }
}
