/*
 * ============================================================
 *  Theme.java
 *  --------------------------------------------------------
 *  Paleta y fuentes compartidas por toda la interfaz Swing.
 *  Centraliza los colores "académicos limpios" para que el
 *  tablero y los paneles luzcan coherentes.
 * ============================================================
 */
package edu.ia.wumpus.ui.swing;

import java.awt.Color;
import java.awt.Font;

public final class Theme {

    private Theme() {}

    // Fondo / superficies
    public static final Color BG          = new Color(0xF5, 0xF2, 0xEB);
    public static final Color PAPER       = new Color(0xFB, 0xF9, 0xF3);
    public static final Color PANEL       = new Color(0xFF, 0xFF, 0xFF);
    public static final Color RULE        = new Color(0xD6, 0xCF, 0xBF);

    // Tinta / texto
    public static final Color INK         = new Color(0x1C, 0x1F, 0x26);
    public static final Color INK_SOFT    = new Color(0x4A, 0x4D, 0x56);
    public static final Color INK_MUTE    = new Color(0x8A, 0x8B, 0x8F);

    // Estados de celda
    public static final Color UNKNOWN     = new Color(0xEB, 0xE6, 0xD9);
    public static final Color VISITED     = new Color(0xFF, 0xFF, 0xFF);
    public static final Color SAFE        = new Color(0xDB, 0xE5, 0xD4);
    public static final Color SAFE_LINE   = new Color(0xB8, 0xCD, 0xB0);
    public static final Color MAYBE       = new Color(0xEF, 0xE1, 0xC7);
    public static final Color MAYBE_LINE  = new Color(0xD8, 0xC8, 0x9E);
    public static final Color DANGER      = new Color(0xEC, 0xD6, 0xD2);
    public static final Color DANGER_LINE = new Color(0xC8, 0x99, 0x94);
    public static final Color GOLD        = new Color(0xF1, 0xE3, 0xB6);
    public static final Color GOLD_LINE   = new Color(0xD6, 0xB8, 0x5A);

    // Acentos
    public static final Color ACCENT      = new Color(0x2C, 0x4A, 0x6E);
    public static final Color GREEN       = new Color(0x4A, 0x7A, 0x3F);
    public static final Color RED         = new Color(0x8A, 0x2C, 0x2C);
    public static final Color GOLD_TEXT   = new Color(0xB8, 0x86, 0x0B);

    // Fuentes
    public static Font sans(int size, int style) {
        return new Font("SansSerif", style, size);
    }
    public static Font mono(int size, int style) {
        return new Font("Monospaced", style, size);
    }
}
