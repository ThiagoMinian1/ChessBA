package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.PieceType;

import java.util.EnumMap;
import java.util.Map;

/** Factory that hands out the {@link MoveRule} (Strategy) for each kind of piece. */
public final class MoveRuleFactory {

    private static final Map<PieceType, MoveRule> RULES = new EnumMap<>(PieceType.class);

    static {
        RULES.put(PieceType.KING, new KingMoveRule());
        RULES.put(PieceType.QUEEN, new QueenMoveRule());
        RULES.put(PieceType.ROOK, new RookMoveRule());
        RULES.put(PieceType.BISHOP, new BishopMoveRule());
        RULES.put(PieceType.KNIGHT, new KnightMoveRule());
        RULES.put(PieceType.PAWN, new PawnMoveRule());
    }

    private MoveRuleFactory() {
    }

    public static MoveRule forType(PieceType type) {
        MoveRule rule = RULES.get(type);
        if (rule == null) {
            throw new IllegalStateException("No move rule registered for " + type);
        }
        return rule;
    }

    public static MoveRule forPiece(Piece piece) {
        return forType(piece.type());
    }
}
