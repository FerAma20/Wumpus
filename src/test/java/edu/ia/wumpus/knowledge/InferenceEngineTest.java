/*
 * ============================================================
 *  InferenceEngineTest.java
 *  Casos canónicos del Mundo de Wumpus que el motor debe
 *  resolver. Cada test es un mini-escenario manual con
 *  percepciones forzadas — independiente de WorldGenerator.
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

class InferenceEngineTest {

    private static List<Cell> neighborsOf(Cell c, int size) {
        List<Cell> out = new ArrayList<>();
        for (Direction d : Direction.values()) {
            Cell n = d.stepFrom(c);
            if (n.inside(size)) out.add(n);
        }
        return out;
    }

    /** Ejemplo 1 del informe: ¬Brisa y ¬Hedor en (1,1) ⇒ (2,1) y (1,2) seguras. */
    @Test
    void example1_safetyPropagation() {
        KnowledgeBase kb = new KnowledgeBase(4);
        InferenceEngine engine = new InferenceEngine(kb, c -> neighborsOf(c, 4));
        Cell start = new Cell(0, 0);
        kb.tell(start, Percept.of(false, false, false), neighborsOf(start, 4));
        engine.run();
        assertTrue(kb.isSafe(new Cell(1, 0)));
        assertTrue(kb.isSafe(new Cell(0, 1)));
    }

    /** Ejemplo 2: dos brisas adyacentes + una celda ¬Brisa → resolución localiza el hoyo. */
    @Test
    void example2_resolutionFindsThePit() {
        KnowledgeBase kb = new KnowledgeBase(4);
        InferenceEngine engine = new InferenceEngine(kb, c -> neighborsOf(c, 4));

        // (1,1) sin brisa → (2,1) y (1,2) seguras
        kb.tell(new Cell(0, 0), Percept.of(false, false, false), neighborsOf(new Cell(0, 0), 4));
        // (2,1) con brisa
        kb.tell(new Cell(1, 0), Percept.of(false, true, false),  neighborsOf(new Cell(1, 0), 4));
        // (1,2) sin brisa → asegura (2,2)
        kb.tell(new Cell(0, 1), Percept.of(false, false, false), neighborsOf(new Cell(0, 1), 4));
        engine.run();

        // Vecinas de (1,0) [brisa] no descartadas: sólo queda (2,0) → debe ser hoyo
        assertTrue(kb.isPit(new Cell(2, 0)),
                "Por resolución, el hoyo debe estar en (3,1)");
        assertTrue(kb.isSafe(new Cell(1, 1)),
                "(2,2) debe haberse marcado como segura");
    }

    /** Ejemplo 3: hedor único + 3 vecinas descartadas → localiza al Wumpus. */
    @Test
    void example3_resolutionFindsTheWumpus() {
        KnowledgeBase kb = new KnowledgeBase(4);
        InferenceEngine engine = new InferenceEngine(kb, c -> neighborsOf(c, 4));

        // Visitamos (1,1) sin hedor → (2,1),(1,2),(0,1)←ya borde son ¬Wumpus
        kb.tell(new Cell(1, 1), Percept.of(false, false, false), neighborsOf(new Cell(1, 1), 4));
        // (2,1) con hedor → vecinas son (1,1)[ya safe], (3,1), (2,0), (2,2)
        kb.tell(new Cell(2, 1), Percept.of(true,  false, false), neighborsOf(new Cell(2, 1), 4));
        // (2,0) sin hedor → ¬Wumpus en (3,0),(2,1),(1,0)
        kb.tell(new Cell(2, 0), Percept.of(false, false, false), neighborsOf(new Cell(2, 0), 4));
        // (2,2) sin hedor → ¬Wumpus en (3,2),(1,2),(2,1),(2,3)
        kb.tell(new Cell(2, 2), Percept.of(false, false, false), neighborsOf(new Cell(2, 2), 4));
        engine.run();

        // Tras descartar 3 vecinas de (2,1), el Wumpus queda únicamente en (3,1)
        assertTrue(kb.isWumpus(new Cell(3, 1)),
                "El Wumpus debe localizarse en (4,2) por resolución");
    }

    @Test
    void wumpusKilled_resetsWumpusInferences() {
        KnowledgeBase kb = new KnowledgeBase(4);
        kb.addWumpus(new Cell(2, 2));
        kb.wumpusKilled();
        assertTrue(kb.getWumpus().isEmpty());
        assertTrue(kb.isNoWumpus(new Cell(2, 2)));
        assertTrue(kb.isSafe(new Cell(2, 2)));
    }
}
