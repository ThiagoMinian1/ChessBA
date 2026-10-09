package ar.uade.tpo.chess.core.rules;

import java.util.List;

public final class RookMoveRule extends SlidingMoveRule {

    private static final List<int[]> DIRECTIONS = List.of(
            new int[]{1, 0}, new int[]{-1, 0}, new int[]{0, 1}, new int[]{0, -1}
    );

    @Override
    protected List<int[]> directions() {
        return DIRECTIONS;
    }
}
