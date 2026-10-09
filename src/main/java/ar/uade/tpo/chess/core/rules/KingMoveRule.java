package ar.uade.tpo.chess.core.rules;

import java.util.List;

/** One square in any direction. Castling is intentionally not included yet. */
public final class KingMoveRule extends FixedOffsetMoveRule {

    private static final List<int[]> OFFSETS = List.of(
            new int[]{1, 0}, new int[]{-1, 0}, new int[]{0, 1}, new int[]{0, -1},
            new int[]{1, 1}, new int[]{1, -1}, new int[]{-1, 1}, new int[]{-1, -1}
    );

    @Override
    protected List<int[]> offsets() {
        return OFFSETS;
    }
}
