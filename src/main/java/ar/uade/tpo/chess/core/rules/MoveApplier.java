package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.PieceFactory;
import ar.uade.tpo.chess.core.model.Position;

import java.util.Optional;

/**
 * Applies an already-validated {@link Move} to a {@link Board}, doing everything the move type implies:
 * removing the captured piece (including the pawn taken en passant), relocating the rook when castling,
 * and swapping in the promoted piece. Moved pieces are flagged as moved, which is what ends castling rights.
 */
public final class MoveApplier {

    private static final int KINGSIDE_ROOK_FILE = 7;
    private static final int QUEENSIDE_ROOK_FILE = 0;
    private static final int KINGSIDE_ROOK_TARGET_FILE = 5;
    private static final int QUEENSIDE_ROOK_TARGET_FILE = 3;

    private MoveApplier() {
    }

    /** Mutates {@code board}. Does not check legality. */
    public static MoveOutcome apply(Board board, Move move) {
        Piece piece = board.pieceAt(move.from())
                .orElseThrow(() -> new IllegalArgumentException("No piece at " + move.from()));
        Position from = move.from();
        Position to = move.to();

        switch (move.type()) {
            case NORMAL, PAWN_DOUBLE_PUSH -> {
                Optional<Piece> captured = board.pieceAt(to);
                board.remove(from);
                board.place(to, piece.withMoved(true));
                return new MoveOutcome(piece, captured, captured.map(c -> to));
            }
            case EN_PASSANT -> {
                Position victimPosition = new Position(to.file(), from.rank());
                Piece victim = board.remove(victimPosition)
                        .orElseThrow(() -> new IllegalArgumentException("No pawn to capture at " + victimPosition));
                board.remove(from);
                board.place(to, piece.withMoved(true));
                return new MoveOutcome(piece, Optional.of(victim), Optional.of(victimPosition));
            }
            case CASTLE_KINGSIDE -> {
                castle(board, from, to, piece, KINGSIDE_ROOK_FILE, KINGSIDE_ROOK_TARGET_FILE);
                return new MoveOutcome(piece, Optional.empty(), Optional.empty());
            }
            case CASTLE_QUEENSIDE -> {
                castle(board, from, to, piece, QUEENSIDE_ROOK_FILE, QUEENSIDE_ROOK_TARGET_FILE);
                return new MoveOutcome(piece, Optional.empty(), Optional.empty());
            }
            case PROMOTION -> {
                Optional<Piece> captured = board.pieceAt(to);
                board.remove(from);
                board.place(to, PieceFactory.create(move.promotion(), piece.team(), true));
                return new MoveOutcome(piece, captured, captured.map(c -> to));
            }
            default -> throw new IllegalArgumentException("Unsupported move type " + move.type());
        }
    }

    private static void castle(Board board, Position kingFrom, Position kingTo, Piece king,
                               int rookFromFile, int rookToFile) {
        int rank = kingFrom.rank();
        Piece rook = board.remove(new Position(rookFromFile, rank))
                .orElseThrow(() -> new IllegalArgumentException("No rook to castle with"));
        board.remove(kingFrom);
        board.place(kingTo, king.withMoved(true));
        board.place(new Position(rookToFile, rank), rook.withMoved(true));
    }
}
