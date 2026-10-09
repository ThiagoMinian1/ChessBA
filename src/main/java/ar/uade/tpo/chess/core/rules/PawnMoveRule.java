package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Pawn;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Square;

import java.util.ArrayList;
import java.util.List;

/**
 * Forward moves (one or, from the starting row, two squares) require the
 * path to be empty; diagonal moves are only legal as a capture. Promotion
 * and en passant are out of scope for now.
 */
public final class PawnMoveRule implements MoveRule {

    @Override
    public List<Position> possibleMoves(Position from, Board board) {
        List<Position> moves = new ArrayList<>();
        Piece piece = board.pieceAt(from);
        if (!(piece instanceof Pawn pawn)) {
            return moves;
        }

        int direction = pawn.forwardDirection();
        int oneStepRow = from.row() + direction;

        if (Position.isInRange(oneStepRow, from.column())) {
            Position oneStep = new Position(oneStepRow, from.column());
            if (board.isEmpty(oneStep)) {
                moves.add(oneStep);

                int twoStepRow = from.row() + 2 * direction;
                if (from.row() == pawn.startingRow() && Position.isInRange(twoStepRow, from.column())) {
                    Position twoStep = new Position(twoStepRow, from.column());
                    if (board.isEmpty(twoStep)) {
                        moves.add(twoStep);
                    }
                }
            }
        }

        addDiagonalCaptureIfAny(moves, from, oneStepRow, from.column() - 1, piece, board);
        addDiagonalCaptureIfAny(moves, from, oneStepRow, from.column() + 1, piece, board);

        return moves;
    }

    private void addDiagonalCaptureIfAny(
            List<Position> moves, Position from, int row, int column, Piece pawn, Board board) {
        if (!Position.isInRange(row, column)) {
            return;
        }
        Position candidate = new Position(row, column);
        Square square = board.squareAt(candidate);
        if (!square.isEmpty() && !square.getPiece().isSameTeam(pawn)) {
            moves.add(candidate);
        }
    }
}
