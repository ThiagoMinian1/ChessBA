package ar.uade.tpo.chess.core.model;

import java.util.Objects;

/**
 * Movimiento de una pieza. En el enroque, {@code from}/{@code to} son las casillas del rey.
 * En una promoción se indica en qué pieza se convierte el peón; en los demás casos debe ser {@code null}.
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

    /** Notación por coordenadas: {@code e2e4} o {@code e7e8=Q} para una promoción. */
    @Override
    public String toString() {
        String base = from.toString() + to;
        return promotion == null ? base : base + "=" + promotion.letter();
    }
}
