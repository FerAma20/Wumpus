/*
 * ============================================================
 *  GameResult.java
 *  --------------------------------------------------------
 *  Resultado terminal de una simulación.
 *      WIN      → agente escapó con el oro
 *      DEAD     → agente murió (hoyo o Wumpus)
 *      STUCK    → sin movimientos posibles
 *      MAX_TURNS→ se agotó el límite de turnos (raro)
 * ============================================================
 */
package edu.ia.wumpus.simulation;

public enum GameResult {
    WIN, DEAD, STUCK, MAX_TURNS, RUNNING
}
