/*
 * ============================================================
 *  Main.java
 *  --------------------------------------------------------
 *  Punto de entrada CLI del proyecto. Genera un mundo, crea
 *  el agente con el algoritmo de búsqueda solicitado, ejecuta
 *  la simulación turno a turno e imprime cada estado.
 *
 *  USO:
 *      mvn exec:java                              # 6x6, A*, semilla aleatoria
 *      mvn exec:java -Dexec.args="8 bfs 42"        # 8x8, BFS, semilla 42
 *      mvn exec:java -Dexec.args="4 astar 123 reveal"  # con modo dios
 *
 *  Parámetros (todos opcionales, en orden):
 *      [0] size       → 4, 6, 8 …  (default 6)
 *      [1] algorithm  → bfs | astar  (default astar)
 *      [2] seed       → long (default: aleatoria)
 *      [3] reveal     → palabra "reveal" para imprimir la verdad
 * ============================================================
 */
package edu.ia.wumpus;

import edu.ia.wumpus.agent.Agent;
import edu.ia.wumpus.environment.World;
import edu.ia.wumpus.environment.WorldGenerator;
import edu.ia.wumpus.search.AStar;
import edu.ia.wumpus.search.BFS;
import edu.ia.wumpus.search.SearchAlgorithm;
import edu.ia.wumpus.simulation.GameResult;
import edu.ia.wumpus.simulation.Simulator;
import edu.ia.wumpus.ui.ConsoleRenderer;

public final class Main {

    public static void main(String[] args) {
        int size           = args.length > 0 ? Integer.parseInt(args[0]) : 6;
        String algoName    = args.length > 1 ? args[1] : "astar";
        long seed          = args.length > 2 ? Long.parseLong(args[2]) : System.currentTimeMillis();
        boolean reveal     = args.length > 3 && args[3].equalsIgnoreCase("reveal");

        SearchAlgorithm algo = algoName.equalsIgnoreCase("bfs") ? new BFS() : new AStar();

        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  Wumpus World — Agente lógico                                ║");
        System.out.printf( "║  size=%d  algoritmo=%s  seed=%d %n", size, algo.name(), seed);
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        World world = new WorldGenerator(size, 0.18, seed).generate();
        Agent agent = new Agent(size, algo, world);
        if (reveal) ConsoleRenderer.renderTruth(world, agent);

        Simulator sim = new Simulator(world, agent);
        while (sim.getResult() == GameResult.RUNNING) {
            sim.step();
            ConsoleRenderer.renderAgentView(agent);
        }

        System.out.println("───── FIN ─────");
        System.out.println("Resultado: " + sim.getResult());
        System.out.println("Turnos:    " + sim.getTurn());
        System.out.println("Puntaje:   " + sim.getScore());
        if (reveal) ConsoleRenderer.renderTruth(world, agent);
    }
}
