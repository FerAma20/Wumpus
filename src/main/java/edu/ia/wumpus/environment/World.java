/*
 * ============================================================
 *  World.java
 *  --------------------------------------------------------
 *  Representa el ESTADO REAL del entorno del Mundo de Wumpus:
 *  - tamaño del tablero
 *  - posiciones de los hoyos
 *  - posición y estado (vivo/muerto) del Wumpus
 *  - posición del oro (o null si ya fue tomado)
 *
 *  Esta clase es la "verdad subyacente". El agente NO accede a
 *  ella directamente — solo a través de getPercepts(). El
 *  método isDeadly(cell) se usa SÓLO desde el Simulator para
 *  decidir el resultado real de una acción, nunca para el
 *  razonamiento del agente.
 * ============================================================
 */
package edu.ia.wumpus.environment;

import edu.ia.wumpus.core.Cell;
import edu.ia.wumpus.core.Direction;
import edu.ia.wumpus.core.Percept;

import java.util.*;

public final class World {

    private final int size;
    private final Set<Cell> pits;
    private Cell wumpus;
    private boolean wumpusAlive;
    private Cell gold;

    /** Constructor — invocado típicamente por WorldGenerator. */
    public World(int size, Set<Cell> pits, Cell wumpus, Cell gold) {
        this.size = size;
        this.pits = Set.copyOf(pits);
        this.wumpus = wumpus;
        this.wumpusAlive = true;
        this.gold = gold;
    }

    public int getSize() { return size; }
    public Set<Cell> getPits() { return pits; }
    public Cell getWumpus() { return wumpus; }
    public boolean isWumpusAlive() { return wumpusAlive; }
    public Cell getGold() { return gold; }

    /** Devuelve las celdas vecinas (4-conectadas) dentro de los límites del tablero. */
    public List<Cell> neighbors(Cell c) {
        List<Cell> ns = new ArrayList<>(4);
        for (Direction d : Direction.values()) {
            Cell n = d.stepFrom(c);
            if (n.inside(size)) ns.add(n);
        }
        return ns;
    }

    /**
     * Calcula las percepciones que recibe el agente al estar en `cell`.
     *  - hedor:    Wumpus vivo en alguna vecina
     *  - brisa:    Hoyo en alguna vecina
     *  - resplandor: oro en la celda actual
     */
    public Percept getPercepts(Cell cell) {
        boolean stench = false;
        boolean breeze = false;
        for (Cell n : neighbors(cell)) {
            if (pits.contains(n)) breeze = true;
            if (wumpusAlive && n.equals(wumpus)) stench = true;
        }
        boolean glitter = gold != null && gold.equals(cell);
        return Percept.of(stench, breeze, glitter);
    }

    /** True si pisar `cell` mata al agente (hoyo o Wumpus vivo). */
    public boolean isDeadly(Cell cell) {
        if (pits.contains(cell)) return true;
        return wumpusAlive && wumpus.equals(cell);
    }

    /** Llamado por el agente cuando recoge el oro — el oro deja de estar en el mundo. */
    public void grabGold() {
        this.gold = null;
    }

    /**
     * Simula el disparo de una flecha desde `from` en dirección `dir`.
     * Si la flecha atraviesa la celda del Wumpus, éste muere.
     * Devuelve true si el Wumpus fue alcanzado (el agente escuchará un Grito).
     */
    public boolean shoot(Cell from, Direction dir) {
        Cell c = dir.stepFrom(from);
        while (c.inside(size)) {
            if (wumpusAlive && c.equals(wumpus)) {
                wumpusAlive = false;
                return true;
            }
            c = dir.stepFrom(c);
        }
        return false;
    }
}
