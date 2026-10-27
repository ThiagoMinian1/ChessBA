package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Team;

import java.util.ArrayList;
import java.util.List;

/**
 * King: one square in any direction, plus castling.
 *
 * <p>Castling (chess.com rules) requires that: neither the king nor the chosen rook has moved; all squares
 * between them are empty; the king is not in check; and the king neither passes through nor lands on an
 * attacked square. (The rook may pass over an attacked square, and on the long side the b-file square only
 * needs to be empty.)
 */
public final class KingMoveRule extends StepMoveRule {

    private static final int KING_FILE = 4;
    private static final int KINGSIDE_ROOK_FILE = 7;
    private static final int QUEENSIDE_ROOK_FILE = 0;

    @Override
    protected int[][] offsets() {
        return Directions.ALL;
    }

    @Override
    public List<Move> pseudoLegalMoves(MoveContext context, Position from, Piece piece) {
        List<Move> moves = new ArrayList<>(super.pseudoLegalMoves(context, from, piece));
        addCastlingMoves(context.board(), from, piece, moves);
        return moves;
    }

    private void addCastlingMoves(Board board, Position from, Piece king, List<Move> moves) {
        Team team = king.team();
        int rank = team.homeRank();
        if (king.hasMoved() || from.file() != KING_FILE || from.rank() != rank) {
            return;
        }
        Team enemy = team.opposite();
        if (AttackDetector.isAttacked(board, from, enemy)) {
            return;
        }

        // Short castling: f and g empty and not attacked.
        Position f = new Position(5, rank);
        Position g = new Position(6, rank);
        if (rookReady(board, team, KINGSIDE_ROOK_FILE, rank)
                && board.isEmpty(f) && board.isEmpty(g)
                && !AttackDetector.isAttacked(board, f, enemy)
                && !AttackDetector.isAttacked(board, g, enemy)) {
            moves.add(Move.castleKingside(from, g));
        }

        // Long castling: b, c and d empty; c and d not attacked (b may be attacked).
        Position b = new Position(1, rank);
        Position c = new Position(2, rank);
        Position d = new Position(3, rank);
        if (rookReady(board, team, QUEENSIDE_ROOK_FILE, rank)
                && board.isEmpty(b) && board.isEmpty(c) && board.isEmpty(d)
                && !AttackDetector.isAttacked(board, c, enemy)
                && !AttackDetector.isAttacked(board, d, enemy)) {
            moves.add(Move.castleQueenside(from, c));
        }
    }

    private boolean rookReady(Board board, Team team, int file, int rank) {
        return board.pieceAt(new Position(file, rank))
                .filter(p -> p.type() == PieceType.ROOK && p.team() == team && !p.hasMoved())
                .isPresent();
    }
}
