package ar.uade.tpo.chess.core.game;

/** Thrown when a move or action is not allowed by the rules (or when the game is already over). */
public class IllegalMoveException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IllegalMoveException(String message) {
        super(message);
    }
}
