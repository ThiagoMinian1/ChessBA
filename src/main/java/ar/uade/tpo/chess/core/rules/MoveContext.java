package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Position;

import java.util.Optional;

/**
 * What a {@link MoveRule} may look at: the board and the one extra piece of game state that move generation
 * needs (the en passant target). Keeping this small is what lets rules be tested without a whole game.
 */
public interface MoveContext {

    Board board();

    /** The square a pawn skipped over on the previous move, if that move was a double push. */
    Optional<Position> enPassantTarget();

    static MoveContext of(Board board, Position enPassantTarget) {
        return new BasicMoveContext(board, enPassantTarget);
    }

    static MoveContext of(Board board) {
        return new BasicMoveContext(board, null);
    }
}
