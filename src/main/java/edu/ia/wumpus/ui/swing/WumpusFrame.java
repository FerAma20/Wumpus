/*
 * ============================================================
 *  WumpusFrame.java
 *  --------------------------------------------------------
 *  Ventana principal de la interfaz gráfica Swing.
 *
 *  Organiza:
 *    - Barra superior de controles (tamaño, algoritmo, velocidad,
 *      botones Paso/Auto/Reiniciar/Nuevo, toggles de paneles).
 *    - Centro: BoardPanel (tablero dibujado).
 *    - Derecha: barra de estado + Base de Conocimiento +
 *      Inferencias en vivo + Registro de acciones.
 *
 *  Conduce la simulación con un javax.swing.Timer para el modo
 *  automático; cada "tick" llama a Simulator.step() y refresca
 *  la vista. Toda la lógica vive en el motor — esta clase sólo
 *  orquesta presentación e interacción.
 * ============================================================
 */
package edu.ia.wumpus.ui.swing;

import edu.ia.wumpus.agent.Agent;
import edu.ia.wumpus.core.Cell;
import edu.ia.wumpus.core.Direction;
import edu.ia.wumpus.environment.World;
import edu.ia.wumpus.environment.WorldGenerator;
import edu.ia.wumpus.search.AStar;
import edu.ia.wumpus.search.BFS;
import edu.ia.wumpus.search.SearchAlgorithm;
import edu.ia.wumpus.search.Planner;
import edu.ia.wumpus.simulation.GameResult;
import edu.ia.wumpus.simulation.Simulator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class WumpusFrame extends JFrame {

    // --- Estado de simulación ---
    private World world;
    private Agent agent;
    private Simulator sim;
    private long seed = new Random().nextLong();

    // --- Configuración ---
    private int size = 6;
    private String algoName = "astar";
    private int speedMs = 450;
    private boolean reveal = false;
    private boolean manualMode = true;   // por defecto: el USUARIO controla los movimientos

    // --- Componentes ---
    private final BoardPanel board = new BoardPanel();
    private final Timer timer;

    private JLabel statPos, statTurn, statScore, statState;
    private JTextArea kbArea, inferArea, logArea;
    private JCheckBox revealBox;
    private JButton autoBtn, stepBtn, climbBtn, hintBtn;
    private JLabel feedback;
    private int lastLogLen = 0, lastInferLen = 0;

    public WumpusFrame() {
        super("Mundo de Wumpus — Agente Lógico");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(Theme.BG);
        setLayout(new BorderLayout(0, 0));

        add(buildControlBar(), BorderLayout.NORTH);
        add(buildBoardArea(), BorderLayout.CENTER);
        add(buildSidePanel(), BorderLayout.EAST);

        // Timer del modo automático (se arranca/para con el botón Auto)
        timer = new Timer(speedMs, e -> doStep());

        // El tablero notifica los clics del usuario (modo manual)
        board.setOnCellClick(this::onManualClick);

        newWorld();
        setMinimumSize(new Dimension(1180, 760));
        pack();
        setLocationRelativeTo(null);
    }

    // ---------------- UI: barra de controles ----------------

    private JComponent buildControlBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 12));
        bar.setBackground(Theme.PAPER);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.RULE));

        // Tamaño
        JComboBox<String> sizeBox = new JComboBox<>(new String[]{"4×4", "6×6", "8×8"});
        sizeBox.setSelectedIndex(1);
        sizeBox.addActionListener(e -> {
            size = new int[]{4, 6, 8}[sizeBox.getSelectedIndex()];
            newWorld();
        });
        bar.add(labeled("Tamaño", sizeBox));

        // Algoritmo
        JComboBox<String> algoBox = new JComboBox<>(new String[]{"A*", "BFS"});
        algoBox.addActionListener(e -> {
            algoName = algoBox.getSelectedIndex() == 0 ? "astar" : "bfs";
            resetSim();
        });
        bar.add(labeled("Algoritmo", algoBox));

        // Velocidad
        JSlider speed = new JSlider(100, 1500, speedMs);
        speed.setPreferredSize(new Dimension(140, 28));
        speed.setBackground(Theme.PAPER);
        speed.addChangeListener(e -> {
            speedMs = speed.getValue();
            timer.setDelay(speedMs);
        });
        bar.add(labeled("Velocidad", speed));

        // Modo de juego: Manual (el usuario hace clic) o Automático (el agente decide)
        JComboBox<String> modeBox = new JComboBox<>(new String[]{"Manual", "Automático"});
        modeBox.setSelectedIndex(manualMode ? 0 : 1);
        modeBox.addActionListener(e -> {
            manualMode = modeBox.getSelectedIndex() == 0;
            timer.stop();
            autoBtn.setText("► Auto");
            updateInteractivity();
            setFeedback(manualMode
                    ? "Modo manual: haz clic en una casilla VECINA (resaltada) para mover al agente."
                    : "Modo automático: pulsa Paso o Auto para que el agente decida.", Theme.INK_SOFT);
        });
        bar.add(labeled("Modo", modeBox));

        // Botones
        stepBtn = button("Paso");
        stepBtn.addActionListener(e -> doStep());
        bar.add(stepBtn);

        autoBtn = button("► Auto");
        autoBtn.addActionListener(e -> toggleAuto());
        bar.add(autoBtn);

        climbBtn = button("Salir ⬆");
        climbBtn.addActionListener(e -> {
            if (sim.manualClimb()) {
                setFeedback(agent.hasGold()
                        ? "¡Saliste de la cueva con el oro! VICTORIA."
                        : "Saliste de la cueva (sin oro).", Theme.GREEN);
                refresh();
            }
        });
        bar.add(climbBtn);

        hintBtn = button("Pista ⓘ");
        hintBtn.addActionListener(e -> showHint());
        bar.add(hintBtn);

        JButton resetBtn = button("Reiniciar");
        resetBtn.addActionListener(e -> resetSim());
        bar.add(resetBtn);

        JButton newBtn = button("Nuevo mundo");
        newBtn.addActionListener(e -> { seed = new Random().nextLong(); newWorld(); });
        bar.add(newBtn);

        // Modo dios
        revealBox = new JCheckBox("Modo Dios");
        revealBox.setBackground(Theme.PAPER);
        revealBox.setFont(Theme.sans(13, Font.PLAIN));
        revealBox.addActionListener(e -> { reveal = revealBox.isSelected(); refresh(); });
        bar.add(revealBox);

        return bar;
    }

    /** Une una etiqueta pequeña encima de un control. */
    private JComponent labeled(String text, JComponent control) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setBackground(Theme.PAPER);
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(Theme.mono(10, Font.PLAIN));
        l.setForeground(Theme.INK_MUTE);
        p.add(l, BorderLayout.NORTH);
        p.add(control, BorderLayout.CENTER);
        return p;
    }

    private JButton button(String text) {
        JButton b = new JButton(text);
        b.setFont(Theme.sans(13, Font.BOLD));
        b.setFocusPainted(false);
        b.setBackground(Theme.PANEL);
        return b;
    }

    // ---------------- UI: área central (tablero + estado) ----------------

    private JComponent buildBoardArea() {
        JPanel wrap = new JPanel(new BorderLayout(0, 12));
        wrap.setBackground(Theme.BG);
        wrap.setBorder(new EmptyBorder(18, 18, 18, 18));
        wrap.add(board, BorderLayout.CENTER);

        // Zona inferior: barra de feedback + barra de estado
        JPanel south = new JPanel(new BorderLayout(0, 10));
        south.setBackground(Theme.BG);

        feedback = new JLabel("Modo manual: haz clic en una casilla vecina (resaltada) para mover al agente.");
        feedback.setFont(Theme.sans(13, Font.PLAIN));
        feedback.setForeground(Theme.INK_SOFT);
        feedback.setBorder(new EmptyBorder(2, 2, 2, 2));
        south.add(feedback, BorderLayout.NORTH);
        south.add(buildStatusBar(), BorderLayout.CENTER);

        wrap.add(south, BorderLayout.SOUTH);
        return wrap;
    }

    private JComponent buildStatusBar() {
        JPanel bar = new JPanel(new GridLayout(1, 4, 1, 0));
        bar.setBackground(Theme.RULE);
        bar.setBorder(BorderFactory.createLineBorder(Theme.RULE));
        statPos   = statCell(bar, "Posición");
        statTurn  = statCell(bar, "Turno");
        statScore = statCell(bar, "Puntuación");
        statState = statCell(bar, "Estado");
        return bar;
    }

    private JLabel statCell(JPanel parent, String label) {
        JPanel cell = new JPanel(new BorderLayout(0, 3));
        cell.setBackground(Theme.PANEL);
        cell.setBorder(new EmptyBorder(10, 14, 12, 14));
        JLabel l = new JLabel(label.toUpperCase());
        l.setFont(Theme.mono(9, Font.PLAIN));
        l.setForeground(Theme.INK_MUTE);
        JLabel v = new JLabel("—");
        v.setFont(Theme.mono(16, Font.BOLD));
        v.setForeground(Theme.INK);
        cell.add(l, BorderLayout.NORTH);
        cell.add(v, BorderLayout.CENTER);
        parent.add(cell);
        return v;
    }

    // ---------------- UI: panel lateral ----------------

    private JComponent buildSidePanel() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(Theme.BG);
        side.setBorder(new EmptyBorder(18, 0, 18, 18));
        side.setPreferredSize(new Dimension(380, 10));

        kbArea    = makeMono();
        inferArea = makeMono();
        logArea   = makeMono();

        side.add(titledPanel("BASE DE CONOCIMIENTO", kbArea, 180));
        side.add(Box.createVerticalStrut(14));
        side.add(titledPanel("INFERENCIAS EN VIVO", inferArea, 200));
        side.add(Box.createVerticalStrut(14));
        side.add(titledPanel("REGISTRO DE ACCIONES", logArea, 200));
        return side;
    }

    private JTextArea makeMono() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setFont(Theme.mono(11, Font.PLAIN));
        a.setForeground(Theme.INK_SOFT);
        a.setBackground(Theme.PANEL);
        a.setMargin(new Insets(8, 10, 8, 10));
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        return a;
    }

    private JComponent titledPanel(String title, JComponent content, int height) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.PANEL);
        p.setBorder(BorderFactory.createLineBorder(Theme.RULE));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));

        JLabel header = new JLabel("  " + title);
        header.setFont(Theme.mono(10, Font.BOLD));
        header.setForeground(Theme.INK_SOFT);
        header.setBorder(new EmptyBorder(8, 6, 8, 6));
        header.setOpaque(true);
        header.setBackground(Theme.PANEL);
        p.add(header, BorderLayout.NORTH);

        JScrollPane sc = new JScrollPane(content);
        sc.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.RULE));
        sc.getVerticalScrollBar().setUnitIncrement(16);
        p.add(sc, BorderLayout.CENTER);
        return p;
    }

    // ---------------- Lógica de simulación ----------------

    /** Crea un mundo nuevo con la configuración actual y reinicia todo. */
    private void newWorld() {
        timer.stop();
        if (autoBtn != null) autoBtn.setText("► Auto");
        world = new WorldGenerator(size, 0.18, seed).generate();
        buildAgentAndSim();
        refresh();
    }

    /** Reinicia el agente sobre el MISMO mundo (misma semilla). */
    private void resetSim() {
        timer.stop();
        if (autoBtn != null) autoBtn.setText("► Auto");
        world = new WorldGenerator(size, 0.18, seed).generate();
        buildAgentAndSim();
        refresh();
    }

    private void buildAgentAndSim() {
        SearchAlgorithm algo = algoName.equals("bfs") ? new BFS() : new AStar();
        agent = new Agent(size, algo, world);
        // Logger: vuelca cada línea del simulador al registro de acciones
        sim = new Simulator(world, agent, 500, line -> appendLog(line));
        lastLogLen = 0;
        lastInferLen = 0;
        logArea.setText("");
        // Percibir la casilla de salida (1,1) para que la KB marque sus vecinas seguras de inmediato
        sim.perceiveCurrent();
    }

    /**
     * Maneja un clic del usuario sobre una celda (modo manual): ordena el
     * movimiento al simulador — que ejecuta percepción, inferencia, puntuación
     * y comprobación de muerte/oro — y refresca la vista con el resultado.
     */
    /**
     * Calcula y resalta (sin mover) la ruta recomendada por la búsqueda activa.
     * Es la integración visible de BFS/A* dentro del modo manual.
     */
    private void showHint() {
        if (sim.getResult() != GameResult.RUNNING) return;
        Planner.Plan p = sim.suggestPath();
        if (p != null && p.hasPath()) {
            board.update(world, agent, p, reveal);   // dibuja la ruta punteada
            board.setExplored(p.explored());          // sombrea las casillas examinadas
            String meta = switch (p.intent()) {
                case RETURN_HOME -> "regreso seguro a (1,1)";
                case EXPLORE     -> "siguiente casilla segura";
                case TAKE_RISK   -> "no hay seguras: ruta de menor riesgo";
                case STUCK       -> "sin ruta";
            };
            setFeedback(p.algo() + " examinó " + p.explored().size() + " casilla(s) (expandidas: "
                    + p.expanded() + ") → " + meta + " " + p.goal().human()
                    + ".  Cambia de algoritmo y vuelve a pulsar Pista para comparar.", Theme.ACCENT);
        } else {
            setFeedback("La búsqueda no encontró una ruta segura desde aquí.", Theme.RED);
        }
    }

    private void onManualClick(Cell target) {
        Simulator.MoveStatus st = sim.manualMove(target);
        switch (st) {
            case OK -> setFeedback("Movido a " + target.human() + ". Inferencia actualizada.", Theme.INK_SOFT);
            case GOLD -> setFeedback("★ ¡Oro recogido en " + target.human() +
                    "! Regresa a (1,1) y pulsa «Salir».", Theme.GOLD_TEXT);
            case DIED -> setFeedback("☠ El agente murió en " + target.human() +
                    " (−1000). Pulsa «Reiniciar» o «Nuevo mundo».", Theme.RED);
            case GAME_OVER -> setFeedback("La partida ya terminó.", Theme.INK_MUTE);
            default -> { /* NOT_ADJACENT / OUT_OF_BOUNDS no ocurren vía clic válido */ }
        }
        refresh();
    }

    /**
     * Sincroniza la interactividad del tablero y el estado de los botones
     * con el modo actual y si la partida sigue en curso.
     */
    private void updateInteractivity() {
        boolean running = sim != null && sim.getResult() == GameResult.RUNNING;
        if (manualMode && running) {
            // Celdas pulsables = vecinas (4-conectadas) dentro del tablero
            Set<Cell> nb = new HashSet<>();
            for (Direction d : Direction.values()) {
                Cell c = d.stepFrom(agent.getPos());
                if (c.inside(size)) nb.add(c);
            }
            board.setInteractive(true, nb);
        } else {
            board.setInteractive(false, null);
        }
        if (stepBtn != null)  stepBtn.setEnabled(!manualMode && running);
        if (autoBtn != null)  autoBtn.setEnabled(!manualMode && running);
        if (hintBtn != null)  hintBtn.setEnabled(manualMode && running);
        if (climbBtn != null) climbBtn.setEnabled(manualMode && running
                && agent.hasGold() && agent.getPos().equals(new Cell(0, 0)));
    }

    /** Muestra un mensaje de feedback al usuario con el color indicado. */
    private void setFeedback(String text, Color color) {
        if (feedback == null) return;
        feedback.setText(text);
        feedback.setForeground(color);
    }

    /** Ejecuta un turno y refresca la vista. Detiene el timer si la partida terminó. */
    private void doStep() {
        if (sim.getResult() != GameResult.RUNNING) {
            timer.stop();
            autoBtn.setText("► Auto");
            return;
        }
        sim.step();
        refresh();
        if (sim.getResult() != GameResult.RUNNING) {
            timer.stop();
            autoBtn.setText("► Auto");
        }
    }

    private void toggleAuto() {
        if (timer.isRunning()) {
            timer.stop();
            autoBtn.setText("► Auto");
        } else if (sim.getResult() == GameResult.RUNNING) {
            timer.start();
            autoBtn.setText("❚❚ Pausa");
        }
    }

    /** Vuelca una línea del simulador al área de registro. */
    private void appendLog(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    /** Refresca tablero, barra de estado y paneles de KB/inferencias. */
    private void refresh() {
        // En modo manual no dibujamos ruta planificada (no hay plan del agente)
        board.update(world, agent, manualMode ? null : sim.getLastPlan(), reveal);

        statPos.setText(agent.getPos().human());
        statTurn.setText(String.valueOf(sim.getTurn()));
        statScore.setText(String.valueOf(sim.getScore()));

        GameResult r = sim.getResult();
        String label = switch (r) {
            case WIN -> "VICTORIA";
            case DEAD -> "MUERTO";
            case STUCK -> "BLOQUEADO";
            case MAX_TURNS -> "LÍMITE";
            case RUNNING -> sim.getTurn() == 0 ? "EN ESPERA" : "EXPLORANDO";
        };
        statState.setText(label);
        statState.setForeground(switch (r) {
            case WIN -> Theme.GREEN;
            case DEAD, STUCK -> Theme.RED;
            default -> Theme.INK;
        });
        statScore.setForeground(sim.getScore() < 0 ? Theme.RED
                : sim.getScore() > 0 ? Theme.GOLD_TEXT : Theme.INK);

        refreshKB();
        refreshInferences();
        updateInteractivity();
    }

    /** Construye el resumen textual de la KB (celdas por categoría). */
    private void refreshKB() {
        var kb = agent.getKB();
        StringBuilder sb = new StringBuilder();
        sb.append("Visitadas : ").append(fmtCells(kb.getVisited())).append('\n');
        sb.append("Seguras   : ").append(fmtCellsFiltered(kb.getSafe(), kb)).append('\n');
        sb.append("¿Hoyo?    : ").append(fmtCells(kb.getMaybePit())).append('\n');
        sb.append("¿Wumpus?  : ").append(fmtCells(kb.getMaybeWumpus())).append('\n');
        sb.append("HOYO!     : ").append(fmtCells(kb.getPit())).append('\n');
        sb.append("WUMPUS!   : ").append(fmtCells(kb.getWumpus()));
        kbArea.setText(sb.toString());
        kbArea.setCaretPosition(0);
    }

    /** Vuelca el log de derivaciones de la KB al panel de inferencias. */
    private void refreshInferences() {
        var deriv = agent.getKB().getDerivationLog();
        StringBuilder sb = new StringBuilder();
        int from = Math.max(0, deriv.size() - 60); // últimas 60 líneas
        for (int i = from; i < deriv.size(); i++) {
            sb.append("#").append(i + 1).append("  ").append(deriv.get(i)).append('\n');
        }
        inferArea.setText(sb.toString());
        inferArea.setCaretPosition(inferArea.getDocument().getLength());
    }

    private String fmtCells(java.util.Set<edu.ia.wumpus.core.Cell> cells) {
        if (cells.isEmpty()) return "—";
        return cells.stream()
                .sorted(java.util.Comparator
                        .comparingInt(edu.ia.wumpus.core.Cell::y)
                        .thenComparingInt(edu.ia.wumpus.core.Cell::x))
                .map(edu.ia.wumpus.core.Cell::human)
                .reduce((a, b) -> a + " " + b).orElse("—");
    }

    /** Igual que fmtCells pero excluye celdas ya visitadas (para "Seguras nuevas"). */
    private String fmtCellsFiltered(java.util.Set<edu.ia.wumpus.core.Cell> cells,
                                    edu.ia.wumpus.knowledge.KnowledgeBase kb) {
        var filtered = cells.stream().filter(c -> !kb.isVisited(c))
                .collect(java.util.stream.Collectors.toSet());
        return fmtCells(filtered);
    }
}
