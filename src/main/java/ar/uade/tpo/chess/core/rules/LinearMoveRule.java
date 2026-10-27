package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base for pieces that slide any number of squares along fixed directions until blocked (rook, bishop, queen).
 * A subclass only declares its directions.
 */
abstract class LinearMoveRule implements MoveRule {

    protected abstract int[][] directions();

    @Override
    public List<Move> pseudoLegalMoves(MoveContext context, Position from, Piece piece) {
        Board board = context.board();
        List<Move> moves = new ArrayList<>();
        for (int[] direction : directions()) {
            int file = from.file() + direction[0];
            int rank = from.rank() + direction[1];
            while (Position.inBounds(file, rank)) {
                Position to = new Position(file, rank);
                Optional<Piece> occupant = board.pieceAt(to);
                if (occupant.isEmpty()) {
                    moves.add(Move.normal(from, to));
                } else {
                    if (occupant.get().team() != piece.team()) {
                        moves.add(Move.normal(from, to));
                    }
                    break;
                }
                file += direction[0];
                rank += direction[1];
            }
        }
        return moves;
    }
}
