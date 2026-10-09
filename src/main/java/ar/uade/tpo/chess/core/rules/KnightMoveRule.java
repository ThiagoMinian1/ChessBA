package ar.uade.tpo.chess.core.rules;

import java.util.List;

public final class KnightMoveRule extends FixedOffsetMoveRule {

    private static final List<int[]> OFFSETS = List.of(
            new int[]{1, 2}, new int[]{2, 1}, new int[]{2, -1}, new int[]{1, -2},
            new int[]{-1, -2}, new int[]{-2, -1}, new int[]{-2, 1}, new int[]{-1, 2}
    );

    @Override
    protected List<int[]> offsets() {
        return OFFSETS;
    }
}
