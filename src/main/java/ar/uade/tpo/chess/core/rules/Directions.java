package ar.uade.tpo.chess.core.rules;

/** Direcciones y desplazamientos compartidos por las reglas de movimiento y el detector de ataques. Cada entrada es {columna, fila}. */
public final class Directions {

    public static final int[][] ORTHOGONAL = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    public static final int[][] DIAGONAL = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    public static final int[][] ALL = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };
    public static final int[][] KNIGHT = {
        {1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}
    };

    private Directions() {
    }
}
