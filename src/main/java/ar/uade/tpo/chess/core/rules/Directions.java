package ar.uade.tpo.chess.core.rules;

/** Direction and offset tables shared by the movement rules and the attack detector. Each entry is {file, rank}. */
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
