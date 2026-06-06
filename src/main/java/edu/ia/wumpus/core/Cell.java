/*
 * ============================================================
 *  Cell.java
 *  --------------------------------------------------------
 *  Representa una coordenada (x, y) dentro de la cuadrícula
 *  del Mundo de Wumpus.
 *
 *  - x: columna (0-indexada), crece hacia la DERECHA.
 *  - y: fila    (0-indexada), crece hacia ARRIBA.
 *  - (0,0) es la esquina inferior izquierda (donde inicia el agente).
 *
 *  Es un `record` para obtener equals/hashCode/toString gratis
 *  y poder usarlo como llave de HashSet/HashMap directamente.
 * ============================================================
 */
package edu.ia.wumpus.core;

public record Cell(int x, int y) {

    /** Devuelve true si la celda está dentro de un tablero de tamaño size×size. */
    public boolean inside(int size) {
        return x >= 0 && y >= 0 && x < size && y < size;
    }

    /** Distancia Manhattan entre dos celdas. Heurística admisible para A* en grid 4-conectado. */
    public int manhattanTo(Cell other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }

    /** Representación humana (1-indexada) — útil para imprimir en consola/log. */
    public String human() {
        return "(" + (x + 1) + "," + (y + 1) + ")";
    }
}
