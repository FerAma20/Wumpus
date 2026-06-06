/*
 * ============================================================
 *  Percept.java
 *  --------------------------------------------------------
 *  Vector inmutable de percepciones que el entorno entrega al
 *  agente cada turno. Los 5 sensores clásicos del Mundo de
 *  Wumpus (AIMA, cap. 7).
 *
 *      stench  — hay un Wumpus en una casilla adyacente.
 *      breeze  — hay un hoyo en una casilla adyacente.
 *      glitter — el oro está en la casilla actual.
 *      bump    — el agente chocó contra una pared al avanzar.
 *      scream  — el Wumpus murió (eco de un disparo exitoso).
 * ============================================================
 */
package edu.ia.wumpus.core;

public record Percept(boolean stench, boolean breeze, boolean glitter,
                      boolean bump, boolean scream) {

    /** Atajo constructor para los 3 sensores principales (no hubo bump ni scream). */
    public static Percept of(boolean stench, boolean breeze, boolean glitter) {
        return new Percept(stench, breeze, glitter, false, false);
    }

    /** Representación compacta para logs: "[ S B - - - ]". */
    public String compact() {
        return "[" +
                (stench  ? "S" : "·") + " " +
                (breeze  ? "B" : "·") + " " +
                (glitter ? "G" : "·") + " " +
                (bump    ? "X" : "·") + " " +
                (scream  ? "!" : "·") + "]";
    }
}
