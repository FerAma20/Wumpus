/*
 * ============================================================
 *  SearchResult.java
 *  --------------------------------------------------------
 *  Estructura de retorno de SearchAlgorithm.search().
 *  - path:    secuencia de celdas (vacía si no se halló ruta)
 *  - expanded: nº de nodos expandidos (para comparar BFS vs A*)
 * ============================================================
 */
package edu.ia.wumpus.search;

import edu.ia.wumpus.core.Cell;

import java.util.List;

public record SearchResult(List<Cell> path, int expanded, List<Cell> explored) {

    /** Atajo: sin lista de exploración (compatibilidad). */
    public SearchResult(List<Cell> path, int expanded) {
        this(path, expanded, List.of());
    }

    public boolean found() { return path != null && !path.isEmpty(); }
}
