/*
 * ============================================================
 *  KnowledgeBase.java
 *  --------------------------------------------------------
 *  Base de Conocimiento (KB) PROPOSICIONAL del agente.
 *
 *  Para cada celda (x,y) del tablero almacenamos átomos:
 *      Visited(x,y), Safe(x,y), NoPit(x,y), NoWumpus(x,y),
 *      Pit(x,y),     Wumpus(x,y), Breeze(x,y), Stench(x,y),
 *      Glitter(x,y), MaybePit(x,y), MaybeWumpus(x,y).
 *
 *  Por compacidad usamos Set<Cell> indexados por átomo en
 *  lugar de un Map<String,Boolean> verboso — semánticamente
 *  equivalente. La inferencia que MANIPULA estos conjuntos vive
 *  en InferenceEngine.java; aquí solo proveemos:
 *    - tell(): incorporar una percepción cruda
 *    - ask*(): consultas booleanas (¿es segura? ¿hay hoyo?)
 *    - getters: para que el planificador lea el grafo seguro
 * ============================================================
 */
package edu.ia.wumpus.knowledge;

import edu.ia.wumpus.core.Cell;
import edu.ia.wumpus.core.Percept;

import java.util.*;

public final class KnowledgeBase {

    private final int size;

    // Hechos sobre celdas visitadas
    private final Set<Cell> visited     = new HashSet<>();
    private final Map<Cell, Boolean> breeze  = new HashMap<>();
    private final Map<Cell, Boolean> stench  = new HashMap<>();
    private final Map<Cell, Boolean> glitter = new HashMap<>();

    // Conjuntos lógicos derivados (poblados por InferenceEngine)
    private final Set<Cell> safe        = new HashSet<>();
    private final Set<Cell> noPit       = new HashSet<>();
    private final Set<Cell> noWumpus    = new HashSet<>();
    private final Set<Cell> pit         = new HashSet<>();
    private final Set<Cell> wumpus      = new HashSet<>();
    private final Set<Cell> maybePit    = new HashSet<>();
    private final Set<Cell> maybeWumpus = new HashSet<>();

    // Log textual de los hechos derivados — útil para UI y video
    private final List<String> derivationLog = new ArrayList<>();

    public KnowledgeBase(int size) {
        this.size = size;
    }

    public int getSize() { return size; }

    // ------------------- TELL -------------------

    /**
     * Incorpora una percepción tomada en la celda `cell`.
     * - Marca la celda como visitada y segura (estamos vivos al recibirla).
     * - Guarda los sensores brutos (breeze, stench, glitter).
     * - Aplica las reglas DIRECTAS:
     *      ¬Brisa(x,y) ⇒ ∀ vecinas ¬Hoyo
     *      ¬Hedor(x,y) ⇒ ∀ vecinas ¬Wumpus
     *   Las reglas DERIVADAS (resolución por unicidad) viven en InferenceEngine.
     */
    public void tell(Cell cell, Percept p, Iterable<Cell> neighbors) {
        visited.add(cell);
        safe.add(cell);
        noPit.add(cell);
        noWumpus.add(cell);
        breeze.put(cell,  p.breeze());
        stench.put(cell,  p.stench());
        glitter.put(cell, p.glitter());

        derivationLog.add("Percibido " + cell.human() + " " + p.compact());

        if (!p.breeze()) {
            for (Cell n : neighbors) noPit.add(n);
            derivationLog.add("¬Brisa" + cell.human() + " ⊢ ¬Hoyo en todas las vecinas");
        }
        if (!p.stench()) {
            for (Cell n : neighbors) noWumpus.add(n);
            derivationLog.add("¬Hedor" + cell.human() + " ⊢ ¬Wumpus en todas las vecinas");
        }
    }

    // ------------------- ASK -------------------

    /** ¿Es seguro caminar sobre `c` según lo que el agente ha demostrado? */
    public boolean isSafe(Cell c)        { return safe.contains(c); }

    /** ¿Está demostrado que en `c` hay un hoyo? */
    public boolean isPit(Cell c)         { return pit.contains(c); }

    /** ¿Está demostrado que en `c` está el Wumpus? */
    public boolean isWumpus(Cell c)      { return wumpus.contains(c); }

    /** ¿Hemos visitado físicamente `c`? */
    public boolean isVisited(Cell c)     { return visited.contains(c); }

    /** ¿Probado que en `c` NO hay hoyo? */
    public boolean isNoPit(Cell c)       { return noPit.contains(c); }

    /** ¿Probado que en `c` NO hay Wumpus? */
    public boolean isNoWumpus(Cell c)    { return noWumpus.contains(c); }

    /** ¿`c` está bajo sospecha (vecina de una brisa, sin descartar)? */
    public boolean isMaybePit(Cell c)    { return maybePit.contains(c); }

    /** ¿`c` está bajo sospecha de Wumpus? */
    public boolean isMaybeWumpus(Cell c) { return maybeWumpus.contains(c); }

    // ------------------- GETTERS PARA EL MOTOR / UI -------------------

    public Set<Cell> getVisited()     { return Collections.unmodifiableSet(visited); }
    public Set<Cell> getSafe()        { return Collections.unmodifiableSet(safe); }
    public Set<Cell> getNoPit()       { return Collections.unmodifiableSet(noPit); }
    public Set<Cell> getNoWumpus()    { return Collections.unmodifiableSet(noWumpus); }
    public Set<Cell> getPit()         { return Collections.unmodifiableSet(pit); }
    public Set<Cell> getWumpus()      { return Collections.unmodifiableSet(wumpus); }
    public Set<Cell> getMaybePit()    { return Collections.unmodifiableSet(maybePit); }
    public Set<Cell> getMaybeWumpus() { return Collections.unmodifiableSet(maybeWumpus); }

    public Map<Cell, Boolean> getBreeze()  { return Collections.unmodifiableMap(breeze); }
    public Map<Cell, Boolean> getStench()  { return Collections.unmodifiableMap(stench); }
    public Map<Cell, Boolean> getGlitter() { return Collections.unmodifiableMap(glitter); }

    public List<String> getDerivationLog() { return Collections.unmodifiableList(derivationLog); }

    // ------------------- MUTACIONES (uso interno de InferenceEngine) -------------------

    void addPit(Cell c)         { pit.add(c); }
    void addWumpus(Cell c)      { wumpus.add(c); }
    void addNoPit(Cell c)       { noPit.add(c); }
    void addNoWumpus(Cell c)    { noWumpus.add(c); }
    void addSafe(Cell c)        { safe.add(c); }
    void addMaybePit(Cell c)    { maybePit.add(c); }
    void addMaybeWumpus(Cell c) { maybeWumpus.add(c); }
    void clearMaybePit()        { maybePit.clear(); }
    void clearMaybeWumpus()     { maybeWumpus.clear(); }
    void logDerivation(String s){ derivationLog.add(s); }

    /** Anula al Wumpus en la KB tras un disparo exitoso. */
    public void wumpusKilled() {
        // Si lo habíamos localizado, ese punto deja de ser un Wumpus vivo
        // pero el hueco sigue siendo seguro (no es hoyo).
        for (Cell c : new HashSet<>(wumpus)) {
            noWumpus.add(c);
            safe.add(c);
        }
        wumpus.clear();
        maybeWumpus.clear();
        derivationLog.add("Grito escuchado ⊢ Wumpus muerto. Recalcular seguridad.");
    }
}
