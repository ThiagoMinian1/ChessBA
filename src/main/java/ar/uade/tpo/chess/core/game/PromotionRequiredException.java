package ar.uade.tpo.chess.core.game;

/**
 * Thrown when a pawn move reaches the last rank but no promotion piece was chosen. It is an
 * {@link IllegalMoveException} so callers that don't care can treat it like any rejected move, while an
 * interactive adapter can catch it to ask the player which piece they want.
 */
public class PromotionRequiredException extends IllegalMoveException {

    private static final long serialVersionUID = 1L;

    public PromotionRequiredException(String message) {
        super(message);
    }
}
