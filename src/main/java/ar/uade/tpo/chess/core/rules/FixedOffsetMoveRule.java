package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Square;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared logic for pieces that jump to a fixed set of offsets rather
 * than sliding (knight, king): no blocking along the way, just "is the
 * landing square in range and not occupied by my own team".
 */
abstract class FixedOffsetMoveRule implements MoveRule {

    protected abstract List<int[]> offsets();

    @Override
    public List<Position> possibleMoves(Position from, Board board) {
        List<Position> moves = new ArrayList<>();
        Piece mover = board.pieceAt(from);

        for (int[] offset : offsets()) {
            int row = from.row() + offset[0];
            int column = from.column() + offset[1];
            if (!Position.isInRange(row, column)) {
                continue;
            }
            Position candidate = new Position(row, column);
            Square square = board.squareAt(candidate);
            if (square.isEmpty() || !square.getPiece().isSameTeam(mover)) {
                moves.add(candidate);
            }
        }
        return moves;
    }
}
