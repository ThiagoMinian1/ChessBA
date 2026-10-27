package ar.uade.tpo.chess.core.model;

import java.util.Objects;

/**
 * A move of a single piece. For castling, {@code from}/{@code to} are the king's squares.
 * Promotion moves carry the piece the pawn becomes; every other move must carry {@code null}.
 */
public record Move(Position from, Position to, MoveType type, PieceType promotion) {

    public Move {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(type, "type");
        if (type == MoveType.PROMOTION) {
            if (promotion == null || !promotion.isPromotionTarget()) {
                throw new IllegalArgumentException(
                        "A pawn can only promote to a queen, rook, bishop or knight, not to " + promotion);
            }
        } else if (promotion != null) {
            throw new IllegalArgumentException("Only promotion moves carry a promotion piece");
        }
    }

    public static Move normal(Position from, Position to) {
        return new Move(from, to, MoveType.NORMAL, null);
    }

    public static Move doublePush(Position from, Position to) {
        return new Move(from, to, MoveType.PAWN_DOUBLE_PUSH, null);
    }

    public static Move enPassant(Position from, Position to) {
        return new Move(from, to, MoveType.EN_PASSANT, null);
    }

    public static Move castleKingside(Position kingFrom, Position kingTo) {
        return new Move(kingFrom, kingTo, MoveType.CASTLE_KINGSIDE, null);
    }

    public static Move castleQueenside(Position kingFrom, Position kingTo) {
        return new Move(kingFrom, kingTo, MoveType.CASTLE_QUEENSIDE, null);
    }

    public static Move promotion(Position from, Position to, PieceType promoteTo) {
        return new Move(from, to, MoveType.PROMOTION, promoteTo);
    }

    public boolean isPromotion() {
        return type == MoveType.PROMOTION;
    }

    public boolean isCastling() {
        return type == MoveType.CASTLE_KINGSIDE || type == MoveType.CASTLE_QUEENSIDE;
    }

    /** Coordinate notation: {@code e2e4}, or {@code e7e8=Q} for promotions. */
    @Override
    public String toString() {
        String base = from.toString() + to;
        return promotion == null ? base : base + "=" + promotion.letter();
    }
}
