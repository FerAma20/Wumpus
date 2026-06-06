/*
 * ============================================================
 *  Direction.java
 *  --------------------------------------------------------
 *  Las 4 orientaciones en las que el agente puede mirar.
 *  Encapsula el vector unitario (dx, dy) y los giros 90°.
 *
 *  Convención: N = +y, E = +x, S = -y, W = -x.
 * ============================================================
 */
package edu.ia.wumpus.core;

public enum Direction {
    NORTH(0, 1),
    EAST(1, 0),
    SOUTH(0, -1),
    WEST(-1, 0);

    public final int dx;
    public final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /** Rota 90° a la izquierda (sentido antihorario). */
    public Direction turnLeft() {
        return switch (this) {
            case NORTH -> WEST;
            case WEST  -> SOUTH;
            case SOUTH -> EAST;
            case EAST  -> NORTH;
        };
    }

    /** Rota 90° a la derecha (sentido horario). */
    public Direction turnRight() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST  -> SOUTH;
            case SOUTH -> WEST;
            case WEST  -> NORTH;
        };
    }

    /** Devuelve la celda resultante de avanzar una casilla desde `from` en esta dirección. */
    public Cell stepFrom(Cell from) {
        return new Cell(from.x() + dx, from.y() + dy);
    }
}
