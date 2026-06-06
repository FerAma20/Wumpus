/*
 * ============================================================
 *  KnowledgeBaseTest.java
 *  Verifica que tell() incorpore correctamente percepciones
 *  ¬Brisa/¬Hedor → ¬Hoyo/¬Wumpus en vecinas, sin necesidad de
 *  correr aún el motor de inferencia.
 * ============================================================
 */
package edu.ia.wumpus.knowledge;

import edu.ia.wumpus.core.Cell;
import edu.ia.wumpus.core.Direction;
import edu.ia.wumpus.core.Percept;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeBaseTest {

    /** Helper: vecinas de `c` en un tablero size×size (no requiere World). */
    private static List<Cell> neighbors(Cell c, int size) {
        List<Cell> out = new ArrayList<>();
        for (Direction d : Direction.values()) {
            Cell n = d.stepFrom(c);
            if (n.inside(size)) out.add(n);
        }
        return out;
    }

    @Test
    void tell_marksCellVisitedAndSafe() {
        KnowledgeBase kb = new KnowledgeBase(4);
        Cell c = new Cell(0, 0);
        kb.tell(c, Percept.of(false, false, false), neighbors(c, 4));
        assertTrue(kb.isVisited(c));
        assertTrue(kb.isSafe(c));
    }

    @Test
    void tell_noBreeze_propagatesNoPitToNeighbors() {
        KnowledgeBase kb = new KnowledgeBase(4);
        Cell c = new Cell(1, 1);
        kb.tell(c, Percept.of(false, false, false), neighbors(c, 4));
        for (Cell n : neighbors(c, 4)) {
            assertTrue(kb.isNoPit(n),   "¬Hoyo debería propagarse a vecina " + n);
            assertTrue(kb.isNoWumpus(n), "¬Wumpus debería propagarse a vecina " + n);
        }
    }

    @Test
    void tell_withBreeze_doesNotProveNeighborsNoPit() {
        KnowledgeBase kb = new KnowledgeBase(4);
        Cell c = new Cell(1, 1);
        kb.tell(c, Percept.of(false, true, false), neighbors(c, 4));
        // sólo la propia celda es ¬Hoyo (estamos vivos sobre ella)
        assertTrue(kb.isNoPit(c));
        // las vecinas NO se prueban como ¬Hoyo
        for (Cell n : neighbors(c, 4)) {
            assertFalse(kb.isNoPit(n));
        }
    }

    @Test
    void derivationLog_isAppendedOnEveryTell() {
        KnowledgeBase kb = new KnowledgeBase(4);
        Cell c = new Cell(0, 0);
        kb.tell(c, Percept.of(false, false, false), neighbors(c, 4));
        assertFalse(kb.getDerivationLog().isEmpty());
    }
}
