package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Square;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared sliding logic for pieces that move in straight lines until
 * blocked (rook, bishop). Subclasses only declare their direction
 * vectors; the walking, blocking and capture rules live here once.
 */
abstract class SlidingMoveRule implements MoveRule {

    protected abstract List<int[]> directions();

    @Override
    public List<Position> possibleMoves(Position from, Board board) {
        List<Position> moves = new ArrayList<>();
        Piece mover = board.pieceAt(from);

        for (int[] direction : directions()) {
            int row = from.row();
            int column = from.column();

            while (true) {
                row += direction[0];
                column += direction[1];
                if (!Position.isInRange(row, column)) {
                    break;
                }
                Position candidate = new Position(row, column);
                Square square = board.squareAt(candidate);

                if (square.isEmpty()) {
                    moves.add(candidate);
                    continue;
                }
                if (!square.getPiece().isSameTeam(mover)) {
                    moves.add(candidate);
                }
                break;
            }
        }
        return moves;
    }
}
