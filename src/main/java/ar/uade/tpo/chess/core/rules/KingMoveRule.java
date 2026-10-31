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
 * Rey: una casilla en cualquier dirección, además del enroque.
 *
 * <p>Para enrocar, según las reglas de chess.com, ni el rey ni la torre elegida deben haberse movido; las casillas
 * entre ambos deben estar vacías; el rey no puede estar en jaque ni pasar por una casilla atacada. La torre sí
 * puede pasar por una casilla atacada y, en el enroque largo, la casilla de la columna b solo debe estar vacía.
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

        // Enroque corto: f y g están vacías y no reciben ataques.
        Position f = new Position(5, rank);
        Position g = new Position(6, rank);
        if (rookReady(board, team, KINGSIDE_ROOK_FILE, rank)
                && board.isEmpty(f) && board.isEmpty(g)
                && !AttackDetector.isAttacked(board, f, enemy)
                && !AttackDetector.isAttacked(board, g, enemy)) {
            moves.add(Move.castleKingside(from, g));
        }

        // Enroque largo: b, c y d están vacías; c y d no reciben ataques (b sí puede estar atacada).
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
