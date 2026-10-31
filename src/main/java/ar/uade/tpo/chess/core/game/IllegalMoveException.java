package ar.uade.tpo.chess.core.game;

/** Se lanza cuando las reglas no permiten un movimiento o una acción, o si la partida ya terminó. */
public class IllegalMoveException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IllegalMoveException(String message) {
        super(message);
    }
}
