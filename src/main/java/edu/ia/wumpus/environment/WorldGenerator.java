/*
 * ============================================================
 *  WorldGenerator.java
 *  --------------------------------------------------------
 *  Construye mundos aleatorios (o con semilla fija) garantizando
 *  jugabilidad mínima:
 *    - El agente arranca en (0,0) sin peligro.
 *    - Las dos casillas adyacentes a (0,0) son seguras
 *      (esto da al agente percepciones útiles desde el inicio).
 *    - El Wumpus y el oro no comparten celda.
 *
 *  Usa java.util.Random sembrado para que dos ejecuciones con
 *  la misma semilla produzcan EXACTAMENTE el mismo mundo —
 *  necesario para tests reproducibles.
 * ============================================================
 */
package edu.ia.wumpus.environment;

import edu.ia.wumpus.core.Cell;

import java.util.*;

public final class WorldGenerator {

    private final int size;
    private final double pitProbability;
    private final Random rng;

    /** size NxN, probabilidad de hoyo en cada celda no protegida, y semilla determinista. */
    public WorldGenerator(int size, double pitProbability, long seed) {
        if (size < 4) throw new IllegalArgumentException("size mínimo = 4");
        this.size = size;
        this.pitProbability = pitProbability;
        this.rng = new Random(seed);
    }

    /** Conveniencia: mundo aleatorio sin semilla fija (no reproducible). */
    public WorldGenerator(int size, double pitProbability) {
        this(size, pitProbability, new Random().nextLong());
    }

    /**
     * Genera un mundo válido. Si tras 60 intentos no encuentra una
     * configuración con candidatos suficientes, devuelve un mundo
     * mínimo (caso patológico — sólo ocurre con tableros 4×4 y
     * probabilidad de hoyo muy alta).
     */
    public World generate() {
        for (int attempt = 0; attempt < 60; attempt++) {
            Set<Cell> pits = new HashSet<>();
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    if (isProtected(x, y)) continue;
                    if (rng.nextDouble() < pitProbability) pits.add(new Cell(x, y));
                }
            }
            List<Cell> candidates = listCandidates(pits);
            if (candidates.size() < 2) continue;

            Cell wumpus = candidates.get(rng.nextInt(candidates.size()));
            // El oro puede estar en cualquier celda válida menos la del Wumpus
            List<Cell> goldCandidates = new ArrayList<>(candidates);
            goldCandidates.remove(wumpus);
            Cell gold = goldCandidates.get(rng.nextInt(goldCandidates.size()));

            return new World(size, pits, wumpus, gold);
        }
        // Fallback determinista
        return new World(size, Set.of(), new Cell(size - 1, size - 1), new Cell(size - 1, 0));
    }

    /** Celdas que jamás pueden albergar un hoyo: (0,0) y sus dos vecinas inmediatas. */
    private boolean isProtected(int x, int y) {
        if (x == 0 && y == 0) return true;
        if (x == 1 && y == 0) return true;
        if (x == 0 && y == 1) return true;
        return false;
    }

    /** Todas las celdas que pueden albergar al Wumpus o al oro: no inicio, no adyacente a inicio, no hoyo. */
    private List<Cell> listCandidates(Set<Cell> pits) {
        List<Cell> out = new ArrayList<>();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (isProtected(x, y)) continue;
                Cell c = new Cell(x, y);
                if (pits.contains(c)) continue;
                out.add(c);
            }
        }
        return out;
    }
}
