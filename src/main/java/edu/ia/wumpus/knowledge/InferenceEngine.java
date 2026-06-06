/*
 * ============================================================
 *  InferenceEngine.java
 *  --------------------------------------------------------
 *  Motor de inferencia PROPOSICIONAL para el Mundo de Wumpus.
 *
 *  Implementa encadenamiento hacia adelante (forward chaining)
 *  con las reglas clásicas del problema. Se ejecuta hasta punto
 *  fijo: mientras la última iteración añada al menos un hecho
 *  nuevo, se vuelve a recorrer la base.
 *
 *  REGLAS APLICADAS  (B = Brisa, H = Hedor, P = Hoyo, W = Wumpus)
 *  ─────────────────────────────────────────────────────────────
 *  R1  ¬B(x,y) ⇒ ¬P(v)   ∀ v ∈ vecinos(x,y)        (en KB.tell)
 *  R2  ¬H(x,y) ⇒ ¬W(v)   ∀ v ∈ vecinos(x,y)        (en KB.tell)
 *  R3   B(x,y) ∧ #{v ∈ vec(x,y) : ¬P(v) no probado} = 1
 *                       ⇒ P(c)  donde c es ese único candidato
 *  R4   H(x,y) ∧ #{v ∈ vec(x,y) : ¬W(v) no probado} = 1
 *                       ⇒ W(c)
 *  R5   ¬P(c) ∧ ¬W(c) ⇒ Safe(c)
 *  R6   Pit(c) ⇒ ¬W(c)  y  Wumpus(c) ⇒ ¬P(c)
 *
 *  La sección "maybes" recalcula cada turno los candidatos
 *  sospechosos para que la UI los pinte (sirve a la heurística
 *  de "asumir riesgo" del agente).
 * ============================================================
 */
package edu.ia.wumpus.knowledge;

import edu.ia.wumpus.core.Cell;
import edu.ia.wumpus.environment.World;

import java.util.*;
import java.util.function.Function;

public final class InferenceEngine {

    private final KnowledgeBase kb;
    /** Función que dado un Cell devuelve sus vecinas válidas — inyectada para no acoplar a World. */
    private final Function<Cell, List<Cell>> neighborsOf;

    public InferenceEngine(KnowledgeBase kb, Function<Cell, List<Cell>> neighborsOf) {
        this.kb = kb;
        this.neighborsOf = neighborsOf;
    }

    /** Atajo: motor que toma las vecinas del propio World. */
    public InferenceEngine(KnowledgeBase kb, World world) {
        this(kb, world::neighbors);
    }

    /**
     * Ejecuta encadenamiento hacia adelante hasta punto fijo.
     * Devuelve el número de iteraciones realizadas (informativo).
     */
    public int run() {
        int iterations = 0;
        boolean changed = true;
        while (changed && iterations < 50) {
            changed = false;
            changed |= applyResolutionPits();
            changed |= applyResolutionWumpus();
            changed |= propagateMutualExclusion();
            changed |= deriveSafe();
            iterations++;
        }
        recomputeMaybes();
        return iterations;
    }

    // ---------- R3 ----------
    /** Para cada visitada con brisa, si sólo queda 1 candidata no descartada → es hoyo. */
    private boolean applyResolutionPits() {
        boolean changed = false;
        for (Map.Entry<Cell, Boolean> e : kb.getBreeze().entrySet()) {
            if (!e.getValue()) continue;
            Cell c = e.getKey();
            List<Cell> candidates = candidatesForPit(c);
            if (candidates.size() == 1) {
                Cell only = candidates.get(0);
                if (!kb.isPit(only)) {
                    kb.addPit(only);
                    kb.logDerivation("Brisa" + c.human() +
                            " ∧ única vecina no segura ⊢ Hoyo" + only.human());
                    changed = true;
                }
            }
        }
        return changed;
    }

    // ---------- R4 ----------
    /** Mismo razonamiento que R3 pero para Wumpus a partir de hedor. */
    private boolean applyResolutionWumpus() {
        boolean changed = false;
        for (Map.Entry<Cell, Boolean> e : kb.getStench().entrySet()) {
            if (!e.getValue()) continue;
            Cell c = e.getKey();
            List<Cell> candidates = candidatesForWumpus(c);
            if (candidates.size() == 1) {
                Cell only = candidates.get(0);
                if (!kb.isWumpus(only)) {
                    kb.addWumpus(only);
                    kb.logDerivation("Hedor" + c.human() +
                            " ∧ única vecina no segura ⊢ Wumpus" + only.human());
                    changed = true;
                }
            }
        }
        return changed;
    }

    // ---------- R6 ----------
    /** Pit(c) y Wumpus(c) se excluyen mutuamente: si una se prueba, la otra no aplica en `c`. */
    private boolean propagateMutualExclusion() {
        boolean changed = false;
        for (Cell c : kb.getPit())    if (!kb.isNoWumpus(c)) { kb.addNoWumpus(c); changed = true; }
        for (Cell c : kb.getWumpus()) if (!kb.isNoPit(c))    { kb.addNoPit(c);    changed = true; }
        return changed;
    }

    // ---------- R5 ----------
    /** ¬Pit ∧ ¬Wumpus ⇒ Safe. Sólo añade celdas que aún no están marcadas como seguras. */
    private boolean deriveSafe() {
        boolean changed = false;
        // Iteramos sobre una copia: addSafe muta el set subyacente
        for (Cell c : new ArrayList<>(kb.getNoPit())) {
            if (kb.isNoWumpus(c) && !kb.isSafe(c)) {
                kb.addSafe(c);
                kb.logDerivation("¬Hoyo" + c.human() + " ∧ ¬Wumpus" + c.human() +
                        " ⊢ Seguro" + c.human());
                changed = true;
            }
        }
        return changed;
    }

    /** Recalcula los conjuntos MaybePit/MaybeWumpus desde cero — usados sólo por la UI. */
    private void recomputeMaybes() {
        kb.clearMaybePit();
        kb.clearMaybeWumpus();
        for (Map.Entry<Cell, Boolean> e : kb.getBreeze().entrySet()) {
            if (!e.getValue()) continue;
            for (Cell n : neighborsOf.apply(e.getKey())) {
                if (!kb.isNoPit(n) && !kb.isPit(n)) kb.addMaybePit(n);
            }
        }
        for (Map.Entry<Cell, Boolean> e : kb.getStench().entrySet()) {
            if (!e.getValue()) continue;
            for (Cell n : neighborsOf.apply(e.getKey())) {
                if (!kb.isNoWumpus(n) && !kb.isWumpus(n)) kb.addMaybeWumpus(n);
            }
        }
    }

    /** Vecinas de `c` que NO han sido descartadas como hoyo. */
    private List<Cell> candidatesForPit(Cell c) {
        List<Cell> out = new ArrayList<>();
        for (Cell n : neighborsOf.apply(c)) if (!kb.isNoPit(n)) out.add(n);
        return out;
    }

    /** Vecinas de `c` que NO han sido descartadas como Wumpus. */
    private List<Cell> candidatesForWumpus(Cell c) {
        List<Cell> out = new ArrayList<>();
        for (Cell n : neighborsOf.apply(c)) if (!kb.isNoWumpus(n)) out.add(n);
        return out;
    }
}
