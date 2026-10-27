package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base for pieces that jump to a fixed set of offsets, ignoring anything in between (knight, king).
 * A subclass only declares its offsets.
 */
abstract class StepMoveRule implements MoveRule {

    protected abstract int[][] offsets();

    @Override
    public List<Move> pseudoLegalMoves(MoveContext context, Position from, Piece piece) {
        Board board = context.board();
        List<Move> moves = new ArrayList<>();
        for (int[] offset : offsets()) {
            Optional<Position> target = from.offset(offset[0], offset[1]);
            if (target.isEmpty()) {
                continue;
            }
            Optional<Piece> occupant = board.pieceAt(target.get());
            if (occupant.isEmpty() || occupant.get().team() != piece.team()) {
                moves.add(Move.normal(from, target.get()));
            }
        }
        return moves;
    }
}
