/*
 * ============================================================
 *  PlannerTest.java
 *  Verifica las tres ramas del Planner:
 *      a) ya tiene oro → vuelve a casa
 *      b) hay segura no visitada → explora
 *      c) no hay seguras → arriesga (si hay candidatas) o STUCK
 * ============================================================
 */
package edu.ia.wumpus.search;

import edu.ia.wumpus.core.Cell;
import edu.ia.wumpus.knowledge.KnowledgeBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlannerTest {

    @Test
    void withGold_planIsReturnHome() {
        KnowledgeBase kb = new KnowledgeBase(4);
        // Marcamos (0,0) y (1,0) como seguras
        kb.addSafe(new Cell(0, 0)); kb.addSafe(new Cell(1, 0));
        Planner planner = new Planner(new AStar());
        Planner.Plan plan = planner.plan(new Cell(1, 0), kb, true);
        assertEquals(Planner.Intent.RETURN_HOME, plan.intent());
        assertEquals(new Cell(0, 0), plan.goal());
    }

    @Test
    void noGold_pickNearestSafeUnvisited() {
        KnowledgeBase kb = new KnowledgeBase(4);
        kb.addSafe(new Cell(0, 0)); kb.addSafe(new Cell(1, 0)); kb.addSafe(new Cell(0, 1));
        // visitada sólo (0,0)
        kb.tell(new Cell(0, 0), edu.ia.wumpus.core.Percept.of(false, false, false), java.util.List.of(new Cell(1, 0), new Cell(0, 1)));
        Planner planner = new Planner(new BFS());
        Planner.Plan plan = planner.plan(new Cell(0, 0), kb, false);
        assertEquals(Planner.Intent.EXPLORE, plan.intent());
    }

    @Test
    void noSafeUnvisited_takesRiskOnLeastDangerous() {
        KnowledgeBase kb = new KnowledgeBase(4);
        kb.addSafe(new Cell(0, 0));
        kb.tell(new Cell(0, 0), edu.ia.wumpus.core.Percept.of(false, true, false),
                java.util.List.of(new Cell(1, 0), new Cell(0, 1)));
        // ambas vecinas serán maybePit; el planner debería elegir la más cercana
        kb.addMaybePit(new Cell(1, 0));
        kb.addMaybePit(new Cell(0, 1));
        Planner planner = new Planner(new AStar());
        Planner.Plan plan = planner.plan(new Cell(0, 0), kb, false);
        assertEquals(Planner.Intent.TAKE_RISK, plan.intent());
    }
}
