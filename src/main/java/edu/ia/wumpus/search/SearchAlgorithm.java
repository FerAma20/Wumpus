/*
 * ============================================================
 *  SearchAlgorithm.java
 *  --------------------------------------------------------
 *  Interfaz común para los algoritmos de búsqueda usados por el
 *  planificador. Permite intercambiarlos sin tocar el agente
 *  (Strategy pattern: BFS, A*, futuro DFS/UCS…).
 * ============================================================
 */
package edu.ia.wumpus.search;

import edu.ia.wumpus.core.Cell;

import java.util.List;
import java.util.function.Predicate;

public interface SearchAlgorithm {

    /**
     * Devuelve un camino de celdas (start → … → goal) sobre un grafo
     * 4-conectado de tamaño size×size donde sólo son transitables las
     * celdas que satisfagan `passable`. La meta SIEMPRE puede ser
     * destino aunque no sea "passable" — útil para "asumir riesgo".
     *
     * @return SearchResult con el camino y métricas; path vacío si no halló ruta.
     */
    SearchResult search(Cell start, Cell goal, Predicate<Cell> passable, int size);

    /** Nombre humano del algoritmo (para logs/UI). */
    String name();
}
