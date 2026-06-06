/*
 * ============================================================
 *  SimulatorIntegrationTest.java
 *  Test de INTEGRACIÓN end-to-end:
 *    Genera un mundo con semilla fija, ejecuta la simulación
 *    completa, y verifica que el resultado sea estable (WIN o
 *    DEAD o STUCK) y que el puntaje siga la fórmula.
 *
 *  Estos tests son útiles para asegurar que el ensamblado de
 *  todos los componentes (Agent + KB + Planner + Search +
 *  World) funcione cohesivamente.
 * ============================================================
 */
package edu.ia.wumpus.simulation;

import edu.ia.wumpus.agent.Agent;
import edu.ia.wumpus.environment.World;
import edu.ia.wumpus.environment.WorldGenerator;
import edu.ia.wumpus.search.AStar;
import edu.ia.wumpus.search.BFS;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimulatorIntegrationTest {

    @Test
    void smallWorld_terminatesWithBfs() {
        World w = new WorldGenerator(4, 0.15, 42L).generate();
        Agent a = new Agent(4, new BFS(), w);
        Simulator sim = new Simulator(w, a, 200, s -> {}); // logger mudo
        GameResult result = sim.runToCompletion();
        assertNotEquals(GameResult.RUNNING, result, "La simulación debe terminar");
    }

    @Test
    void smallWorld_terminatesWithAStar() {
        World w = new WorldGenerator(4, 0.15, 99L).generate();
        Agent a = new Agent(4, new AStar(), w);
        Simulator sim = new Simulator(w, a, 200, s -> {});
        GameResult result = sim.runToCompletion();
        assertNotEquals(GameResult.RUNNING, result);
    }

    @Test
    void winnerScoreIsAboveBaseline() {
        // Buscamos semillas donde el agente gana
        int wins = 0;
        int bestScore = Integer.MIN_VALUE;
        for (long seed = 1; seed <= 30; seed++) {
            World w = new WorldGenerator(4, 0.10, seed).generate();
            Agent a = new Agent(4, new AStar(), w);
            Simulator sim = new Simulator(w, a, 200, s -> {});
            if (sim.runToCompletion() == GameResult.WIN) {
                wins++;
                bestScore = Math.max(bestScore, sim.getScore());
            }
        }
        assertTrue(wins > 0, "Debería ganar al menos una vez en 30 mundos pequeños");
        assertTrue(bestScore > 900, "El mejor puntaje ganador debe estar cerca de +1000");
    }

    @Test
    void bigWorld_runsWithoutCrashing() {
        World w = new WorldGenerator(8, 0.2, 7L).generate();
        Agent a = new Agent(8, new AStar(), w);
        Simulator sim = new Simulator(w, a, 500, s -> {});
        assertDoesNotThrow(sim::runToCompletion);
    }
}
