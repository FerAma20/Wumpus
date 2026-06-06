/*
 * ============================================================
 *  Action.java
 *  --------------------------------------------------------
 *  Catálogo de acciones que el agente puede ejecutar sobre el
 *  entorno en un turno. Sigue las acciones canónicas del
 *  Mundo de Wumpus (AIMA, cap. 7).
 * ============================================================
 */
package edu.ia.wumpus.core;

public enum Action {
    /** Avanzar una casilla en la dirección actual del agente. */
    FORWARD,

    /** Girar 90° a la izquierda. */
    TURN_LEFT,

    /** Girar 90° a la derecha. */
    TURN_RIGHT,

    /** Recoger oro si está en la casilla actual. */
    GRAB,

    /** Disparar la flecha en la dirección actual del agente (una sola vez). */
    SHOOT,

    /** Trepar para salir — sólo válido en (1,1). */
    CLIMB
}
