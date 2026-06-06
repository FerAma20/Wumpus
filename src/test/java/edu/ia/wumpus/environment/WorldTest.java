/*
 * ============================================================
 *  WorldTest.java
 *  Pruebas del entorno: percepciones correctas, vecindarios,
 *  letalidad y efecto del disparo.
 * ============================================================
 */
package edu.ia.wumpus.environment;

import edu.ia.wumpus.core.Cell;
import edu.ia.wumpus.core.Direction;
import edu.ia.wumpus.core.Percept;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WorldTest {

    /** Mundo 4x4 manual:
     *    Hoyos en (2,0) y (1,2). Wumpus en (0,2). Oro en (2,2).
     */
    private World fixture() {
        return new World(4,
                Set.of(new Cell(2, 0), new Cell(1, 2)),
                new Cell(0, 2),
                new Cell(2, 2));
    }

    @Test
    void neighbors_atCornerHasTwo() {
        World w = fixture();
        assertEquals(2, w.neighbors(new Cell(0, 0)).size());
        assertEquals(2, w.neighbors(new Cell(3, 3)).size());
    }

    @Test
    void neighbors_atInteriorHasFour() {
        World w = fixture();
        assertEquals(4, w.neighbors(new Cell(1, 1)).size());
    }

    @Test
    void percepts_atStart_breezeFromPitNeighbor() {
        // (0,0) vecinas: (1,0) y (0,1). El hoyo en (2,0) NO es vecino → no hay brisa aquí.
        World w = fixture();
        Percept p = w.getPercepts(new Cell(0, 0));
        assertFalse(p.breeze());
        assertFalse(p.stench());
        assertFalse(p.glitter());
    }

    @Test
    void percepts_breezeDetectedNextToPit() {
        // (1,0) vecinas: (0,0),(2,0),(1,1). (2,0) es hoyo → brisa.
        World w = fixture();
        Percept p = w.getPercepts(new Cell(1, 0));
        assertTrue(p.breeze());
    }

    @Test
    void percepts_stenchDetectedNextToWumpus() {
        // (0,1) vecinas: (0,0),(1,1),(0,2). (0,2) es Wumpus → hedor.
        World w = fixture();
        Percept p = w.getPercepts(new Cell(0, 1));
        assertTrue(p.stench());
    }

    @Test
    void percepts_glitterOnGoldCell() {
        World w = fixture();
        assertTrue(w.getPercepts(new Cell(2, 2)).glitter());
        assertFalse(w.getPercepts(new Cell(0, 0)).glitter());
    }

    @Test
    void isDeadly_trueForPitAndLiveWumpus() {
        World w = fixture();
        assertTrue(w.isDeadly(new Cell(2, 0)));   // hoyo
        assertTrue(w.isDeadly(new Cell(0, 2)));   // wumpus vivo
        assertFalse(w.isDeadly(new Cell(0, 0)));  // libre
    }

    @Test
    void shoot_killsWumpusInLine() {
        World w = fixture();
        // Disparar desde (0,0) hacia el norte → atraviesa (0,1) y (0,2) [wumpus]
        boolean hit = w.shoot(new Cell(0, 0), Direction.NORTH);
        assertTrue(hit);
        assertFalse(w.isWumpusAlive());
        assertFalse(w.isDeadly(new Cell(0, 2)));
    }

    @Test
    void shoot_missesIfNoWumpusInPath() {
        World w = fixture();
        // Disparar al este desde (0,0): (1,0),(2,0)hoyo,(3,0) — el Wumpus no está aquí
        boolean hit = w.shoot(new Cell(0, 0), Direction.EAST);
        assertFalse(hit);
        assertTrue(w.isWumpusAlive());
    }

    @Test
    void grabGold_removesGoldFromWorld() {
        World w = fixture();
        w.grabGold();
        assertNull(w.getGold());
        assertFalse(w.getPercepts(new Cell(2, 2)).glitter());
    }
}
