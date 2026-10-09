package ar.uade.tpo.chess.core.rules;

import java.util.List;

public final class BishopMoveRule extends SlidingMoveRule {

    private static final List<int[]> DIRECTIONS = List.of(
            new int[]{1, 1}, new int[]{1, -1}, new int[]{-1, 1}, new int[]{-1, -1}
    );

    @Override
    protected List<int[]> directions() {
        return DIRECTIONS;
    }
}
