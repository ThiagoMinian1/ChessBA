package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.PieceType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Looks up the {@link MoveRule} for a given {@link PieceType}. Adding a
 * new piece type means adding one line here and one new MoveRule class —
 * nothing in {@code Game} or the other rules has to change.
 */
public final class MoveRuleFactory {

    private static final Map<PieceType, MoveRule> RULES = new EnumMap<>(PieceType.class);

    static {
        RULES.put(PieceType.PAWN, new PawnMoveRule());
        RULES.put(PieceType.ROOK, new RookMoveRule());
        RULES.put(PieceType.KNIGHT, new KnightMoveRule());
        RULES.put(PieceType.BISHOP, new BishopMoveRule());
        RULES.put(PieceType.QUEEN, new QueenMoveRule());
        RULES.put(PieceType.KING, new KingMoveRule());
    }

    private MoveRuleFactory() {
    }

    public static MoveRule ruleFor(PieceType type) {
        MoveRule rule = RULES.get(type);
        if (rule == null) {
            throw new IllegalArgumentException("No MoveRule registered for " + type);
        }
        return rule;
    }
}
