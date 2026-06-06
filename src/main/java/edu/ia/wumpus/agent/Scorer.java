/*
 * ============================================================
 *  Scorer.java
 *  --------------------------------------------------------
 *  Aplica la función de puntuación canónica del Mundo de Wumpus
 *  (AIMA, cap. 7):
 *      −1     por cada acción
 *      −10    por disparar la flecha
 *      −1000  por morir
 *      +1000  por recoger el oro
 *
 *  Es mutable e inmutable a la vez — un acumulador simple
 *  separado del agente para poder testear puntuaciones sin
 *  necesitar el ciclo completo.
 * ============================================================
 */
package edu.ia.wumpus.agent;

public final class Scorer {

    public static final int COST_ACTION = -1;
    public static final int COST_SHOOT  = -10;
    public static final int COST_DEATH  = -1000;
    public static final int REWARD_GOLD = +1000;

    private int score = 0;

    public int get() { return score; }

    /** Suma un coste por acción ordinaria (avanzar, girar, agarrar, trepar). */
    public void onAction()     { score += COST_ACTION; }

    /** Suma costo adicional por disparar (la propia acción ya gastó COST_ACTION). */
    public void onShoot()      { score += COST_SHOOT; }

    /** El agente recoge el oro. */
    public void onGoldGrabbed(){ score += REWARD_GOLD; }

    /** El agente muere (hoyo o Wumpus). */
    public void onDeath()      { score += COST_DEATH; }
}
