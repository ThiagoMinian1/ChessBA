package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.Position;

import java.util.List;

/**
 * Strategy: how one kind of piece moves. A rule only produces <em>pseudo-legal</em> moves, i.e. moves that
 * respect the piece's own geometry and blocking, but that may still leave the mover's own king in check.
 * Filtering those out is the job of {@link MoveGenerator}, so every rule stays small and independent.
 */
public interface MoveRule {

    /**
     * @param context the board (and en passant target) to move on
     * @param from    where the piece currently stands
     * @param piece   the piece to move; its team decides direction and what counts as a capture
     */
    List<Move> pseudoLegalMoves(MoveContext context, Position from, Piece piece);
}
