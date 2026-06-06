/*
 * ============================================================
 *  SwingMain.java
 *  --------------------------------------------------------
 *  Punto de entrada de la INTERFAZ GRÁFICA (Swing).
 *
 *  Ejecuta esta clase para ver el Mundo de Wumpus con tablero
 *  dibujado, controles y paneles de conocimiento — en lugar de
 *  la versión de consola (edu.ia.wumpus.Main).
 *
 *  En IntelliJ: abre este archivo y pulsa el ▶ verde junto a
 *  main(), o usa la configuración de ejecución "SwingMain".
 * ============================================================
 */
package edu.ia.wumpus;

import edu.ia.wumpus.ui.swing.WumpusFrame;

import javax.swing.*;

public final class SwingMain {

    public static void main(String[] args) {
        // Look & feel nativo del sistema (se ve mejor que el Metal por defecto)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // si falla, seguimos con el look por defecto
        }
        // Toda la UI Swing debe construirse en el Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new WumpusFrame().setVisible(true));
    }
}
