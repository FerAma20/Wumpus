/*
 * ============================================================
 *  BoardPanel.java
 *  --------------------------------------------------------
 *  Componente Swing que DIBUJA el tablero del Mundo de Wumpus.
 *
 *  Pinta, para cada celda, el estado que el agente CONOCE
 *  (visitada, segura, sospechosa, peligro confirmado), las
 *  percepciones registradas (Brisa/Hedor/Resplandor), la ruta
 *  planificada y la ficha del agente.
 *
 *  En "modo dios" (reveal) superpone la verdad subyacente del
 *  mundo: hoyos, Wumpus y oro reales, para verificar que las
 *  inferencias del agente sean correctas.
 *
 *  Es puramente de presentación: lee el estado de World + Agent
 *  pero nunca lo modifica.
 * ============================================================
 */
package edu.ia.wumpus.ui.swing;

import edu.ia.wumpus.agent.Agent;
import edu.ia.wumpus.core.Cell;
import edu.ia.wumpus.environment.World;
import edu.ia.wumpus.knowledge.KnowledgeBase;
import edu.ia.wumpus.search.Planner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class BoardPanel extends JPanel {

    private World world;
    private Agent agent;
    private Planner.Plan plan;
    private boolean reveal = false;

    // --- Interacción (modo manual) ---
    private boolean interactive = false;       // true → el usuario puede hacer clic
    private final Set<Cell> clickable = new HashSet<>(); // celdas que se pueden pulsar
    private Cell hovered = null;                // celda bajo el cursor
    private Consumer<Cell> onCellClick;         // callback al pulsar una celda válida

    // --- Visualización de la búsqueda (BFS vs A*) ---
    private final java.util.List<Cell> explored = new java.util.ArrayList<>(); // casillas examinadas

    // --- Geometría calculada en el último paint (para hit-testing del ratón) ---
    private int geoOx, geoOy, geoGap, geoN;
    private double geoCs;

    public BoardPanel() {
        setBackground(Theme.PAPER);
        setPreferredSize(new Dimension(560, 560));
        installMouse();
    }

    /** Registra los listeners de ratón: hover resalta, clic dispara el callback. */
    private void installMouse() {
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                if (!interactive) return;
                Cell c = cellAt(e.getX(), e.getY());
                if (c != hovered) { hovered = c; repaint(); }
            }
            @Override public void mouseExited(MouseEvent e) {
                if (hovered != null) { hovered = null; repaint(); }
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (!interactive || onCellClick == null) return;
                Cell c = cellAt(e.getX(), e.getY());
                if (c != null && clickable.contains(c)) onCellClick.accept(c);
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    /** Define el callback invocado cuando el usuario pulsa una celda pulsable. */
    public void setOnCellClick(Consumer<Cell> cb) { this.onCellClick = cb; }

    /** Actualiza el estado a dibujar y solicita repintado. */
    public void update(World world, Agent agent, Planner.Plan plan, boolean reveal) {
        this.world = world;
        this.agent = agent;
        this.plan = plan;
        this.reveal = reveal;
        this.explored.clear();   // un refresco normal borra la visualización de búsqueda
        repaint();
    }

    /**
     * Resalta las casillas que un algoritmo de búsqueda examinó para hallar la
     * ruta. Es la diferencia VISIBLE entre BFS (mancha amplia) y A* (corredor
     * estrecho hacia la meta). Llamar DESPUÉS de update().
     */
    public void setExplored(java.util.List<Cell> cells) {
        this.explored.clear();
        if (cells != null) this.explored.addAll(cells);
        repaint();
    }

    /**
     * Configura el modo interactivo y QUÉ celdas son pulsables.
     * @param interactive true para habilitar clics del usuario
     * @param clickableCells celdas válidas (típicamente las vecinas del agente)
     */
    public void setInteractive(boolean interactive, Set<Cell> clickableCells) {
        this.interactive = interactive;
        this.clickable.clear();
        if (clickableCells != null) this.clickable.addAll(clickableCells);
        setCursor(Cursor.getPredefinedCursor(
                interactive ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    /** Traduce coordenadas de píxel a una celda del tablero, o null si está fuera. */
    public Cell cellAt(int mx, int my) {
        if (geoCs <= 0) return null;
        double unit = geoCs + geoGap;
        int col = (int) Math.floor((mx - geoOx) / unit);
        int rowFromTop = (int) Math.floor((my - geoOy) / unit);
        if (col < 0 || col >= geoN || rowFromTop < 0 || rowFromTop >= geoN) return null;
        // Verificar que el clic cayó DENTRO de la celda (no en el hueco entre celdas)
        double cellX = geoOx + col * unit;
        double cellY = geoOy + rowFromTop * unit;
        if (mx > cellX + geoCs || my > cellY + geoCs) return null;
        int y = geoN - 1 - rowFromTop;          // invertir: y crece hacia arriba
        return new Cell(col, y);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        if (world == null || agent == null) return;

        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int n = world.getSize();
        KnowledgeBase kb = agent.getKB();

        // Área cuadrada centrada con margen
        int margin = 16;
        int side = Math.min(getWidth(), getHeight()) - 2 * margin;
        int ox = (getWidth() - side) / 2;
        int oy = (getHeight() - side) / 2;
        int gap = 4;
        double cs = (side - gap * (n - 1)) / (double) n;

        // Guardar geometría para el hit-testing del ratón
        geoOx = ox; geoOy = oy; geoGap = gap; geoN = n; geoCs = cs;

        // Recorremos filas de arriba (y=n-1) hacia abajo
        for (int y = n - 1; y >= 0; y--) {
            for (int x = 0; x < n; x++) {
                Cell c = new Cell(x, y);
                int px = (int) Math.round(ox + x * (cs + gap));
                int py = (int) Math.round(oy + (n - 1 - y) * (cs + gap));
                int w = (int) Math.round(cs);
                drawCell(g, c, kb, px, py, w);
            }
        }

        // Resaltar celdas pulsables y la celda bajo el cursor (modo manual)
        if (interactive) drawClickableHighlights(g, ox, oy, cs, gap, n);

        // Visualización de la búsqueda (casillas examinadas) debajo de la ruta
        drawExplored(g, ox, oy, cs, gap, n);

        // Ruta planificada (línea punteada azul)
        drawPlannedPath(g, ox, oy, cs, gap, n);

        // Ficha del agente
        drawAgent(g, ox, oy, cs, gap, n);
    }

    /** Dibuja un anillo de acento en las celdas pulsables; relleno suave en la celda en hover. */
    private void drawClickableHighlights(Graphics2D g, int ox, int oy, double cs, int gap, int n) {
        for (Cell c : clickable) {
            int px = (int) Math.round(ox + c.x() * (cs + gap));
            int py = (int) Math.round(oy + (n - 1 - c.y()) * (cs + gap));
            int w = (int) Math.round(cs);
            boolean isHover = c.equals(hovered);
            if (isHover) {
                g.setColor(new Color(0x2C, 0x4A, 0x6E, 38));
                g.fillRoundRect(px, py, w, w, 6, 6);
            }
            g.setColor(Theme.ACCENT);
            g.setStroke(new BasicStroke(isHover ? 3f : 2f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND, 0, new float[]{4f, 4f}, 0));
            g.drawRoundRect(px + 2, py + 2, w - 4, w - 4, 6, 6);
        }
    }


    /** Pinta una celda con su color de estado, borde, coordenada y marcadores de percepción. */
    private void drawCell(Graphics2D g, Cell c, KnowledgeBase kb, int px, int py, int w) {
        Color fill = Theme.UNKNOWN, line = Theme.RULE;

        boolean visited = kb.isVisited(c);
        boolean revealedPit    = reveal && world.getPits().contains(c);
        boolean revealedWumpus = reveal && world.isWumpusAlive() && c.equals(world.getWumpus());
        boolean revealedGold   = reveal && world.getGold() != null && c.equals(world.getGold());

        if (visited)             { fill = Theme.VISITED; line = Theme.RULE; }
        else if (kb.isPit(c) || kb.isWumpus(c)) { fill = Theme.DANGER; line = Theme.DANGER_LINE; }
        else if (kb.isSafe(c))   { fill = Theme.SAFE;   line = Theme.SAFE_LINE; }
        else if (kb.isMaybePit(c) || kb.isMaybeWumpus(c)) { fill = Theme.MAYBE; line = Theme.MAYBE_LINE; }

        // Oro en celda visitada
        if (visited && revealedGold) { fill = Theme.GOLD; line = Theme.GOLD_LINE; }

        g.setColor(fill);
        g.fillRoundRect(px, py, w, w, 6, 6);
        g.setColor(line);
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(px, py, w, w, 6, 6);

        // Coordenada (1-indexada) arriba-izq
        g.setFont(Theme.mono(Math.max(9, w / 9), Font.PLAIN));
        g.setColor(Theme.INK_MUTE);
        g.drawString((c.x() + 1) + "," + (c.y() + 1), px + 5, py + 14);

        // Marcadores de percepción para celdas visitadas
        if (visited) {
            StringBuilder marks = new StringBuilder();
            if (Boolean.TRUE.equals(kb.getBreeze().get(c)))  marks.append("B ");
            if (Boolean.TRUE.equals(kb.getStench().get(c)))  marks.append("S ");
            if (Boolean.TRUE.equals(kb.getGlitter().get(c)) && !agent.hasGold()) marks.append("*");
            if (marks.length() > 0) {
                g.setFont(Theme.mono(Math.max(11, w / 6), Font.BOLD));
                g.setColor(Theme.ACCENT);
                FontMetrics fm = g.getFontMetrics();
                String s = marks.toString().trim();
                g.drawString(s, px + (w - fm.stringWidth(s)) / 2, py + w / 2 + fm.getAscent() / 2);
            }
        }

        // Etiqueta de estado abajo
        String badge = null;
        Color badgeColor = Theme.INK_MUTE;
        if (!visited && kb.isSafe(c))        { badge = "segura"; badgeColor = Theme.GREEN; }
        else if (kb.isPit(c))                { badge = "HOYO";   badgeColor = Theme.RED; }
        else if (kb.isWumpus(c))             { badge = "WUMPUS"; badgeColor = Theme.RED; }
        else if (kb.isMaybePit(c) && kb.isMaybeWumpus(c)) { badge = "?!?"; badgeColor = Theme.GOLD_TEXT; }
        else if (kb.isMaybePit(c))           { badge = "?hoyo"; badgeColor = Theme.GOLD_TEXT; }
        else if (kb.isMaybeWumpus(c))        { badge = "?wmp";  badgeColor = Theme.GOLD_TEXT; }
        if (badge != null && w > 40) {
            g.setFont(Theme.mono(Math.max(8, w / 11), Font.PLAIN));
            g.setColor(badgeColor);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(badge, px + w - fm.stringWidth(badge) - 5, py + w - 6);
        }

        // Iconos de verdad (modo dios) abajo-izq
        if (reveal) {
            String icon = null;
            if (revealedPit) icon = "(O)";
            else if (revealedWumpus) icon = "W!";
            else if (revealedGold && !agent.hasGold()) icon = "Au";
            if (icon != null) {
                g.setFont(Theme.mono(Math.max(9, w / 8), Font.BOLD));
                g.setColor(revealedGold ? Theme.GOLD_TEXT : Theme.RED);
                g.drawString(icon, px + 5, py + w - 6);
            }
        }
    }

    /**
     * Sombrea las casillas examinadas por la búsqueda con una numeración del
     * orden de expansión, para que se vea CUÁNTO exploró cada algoritmo.
     */
    private void drawExplored(Graphics2D g, int ox, int oy, double cs, int gap, int n) {
        if (explored.isEmpty()) return;
        for (int i = 0; i < explored.size(); i++) {
            Cell c = explored.get(i);
            int px = (int) Math.round(ox + c.x() * (cs + gap));
            int py = (int) Math.round(oy + (n - 1 - c.y()) * (cs + gap));
            int w = (int) Math.round(cs);
            g.setColor(new Color(0x2C, 0x4A, 0x6E, 46));   // tinte azul translucido
            g.fillRoundRect(px, py, w, w, 6, 6);
            // número de orden de expansión (pequeño, centrado-arriba)
            if (w > 36) {
                g.setColor(new Color(0x2C, 0x4A, 0x6E, 200));
                g.setFont(Theme.mono(Math.max(9, w / 8), Font.BOLD));
                String s = String.valueOf(i + 1);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(s, px + (w - fm.stringWidth(s)) / 2, py + w / 2 - 2);
            }
        }
    }

    /** Dibuja la ruta planificada como una polilínea azul punteada. */
    private void drawPlannedPath(Graphics2D g, int ox, int oy, double cs, int gap, int n) {
        if (plan == null || plan.path() == null || plan.path().size() < 2) return;
        List<Cell> path = plan.path();
        g.setColor(new Color(0x2C, 0x4A, 0x6E, 150));
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                0, new float[]{5f, 5f}, 0));
        Path2D p2 = new Path2D.Double();
        for (int i = 0; i < path.size(); i++) {
            Cell c = path.get(i);
            double cx = ox + c.x() * (cs + gap) + cs / 2;
            double cy = oy + (n - 1 - c.y()) * (cs + gap) + cs / 2;
            if (i == 0) p2.moveTo(cx, cy); else p2.lineTo(cx, cy);
        }
        g.draw(p2);
    }

    /** Dibuja la ficha circular del agente sobre su celda actual. */
    private void drawAgent(Graphics2D g, int ox, int oy, double cs, int gap, int n) {
        Cell pos = agent.getPos();
        double cx = ox + pos.x() * (cs + gap) + cs / 2;
        double cy = oy + (n - 1 - pos.y()) * (cs + gap) + cs / 2;
        int r = (int) (cs * 0.32);

        Color core = Theme.INK;
        if (!agent.isAlive())      core = Theme.RED;
        else if (agent.hasEscaped()) core = Theme.GREEN;

        // sombra
        g.setColor(new Color(0, 0, 0, 40));
        g.fillOval((int) (cx - r) + 2, (int) (cy - r) + 3, r * 2, r * 2);
        // cuerpo
        g.setColor(core);
        g.fillOval((int) (cx - r), (int) (cy - r), r * 2, r * 2);
        // etiqueta
        g.setColor(Theme.BG);
        g.setFont(Theme.mono(Math.max(11, r), Font.BOLD));
        String s = agent.hasGold() ? "A*" : "A";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (int) (cx - fm.stringWidth(s) / 2.0), (int) (cy + fm.getAscent() / 2.0 - 1));
    }
}
