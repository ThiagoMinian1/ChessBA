package ar.uade.tpo.chess.core.game;

/**
 * Se lanza cuando un peón llega a la última fila sin que se haya elegido la pieza de promoción.
 * Es una {@link IllegalMoveException}, así que quienes no necesiten tratarla por separado pueden considerarla
 * una jugada rechazada; un adaptador interactivo puede capturarla y pedirle al jugador que elija una pieza.
 */
public class PromotionRequiredException extends IllegalMoveException {

    private static final long serialVersionUID = 1L;

    public PromotionRequiredException(String message) {
        super(message);
    }
}
