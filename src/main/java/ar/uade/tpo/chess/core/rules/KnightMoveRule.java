package ar.uade.tpo.chess.core.rules;

/** Knight: an L-shaped jump (two squares one way, one the other); it leaps over any piece in between. */
public final class KnightMoveRule extends StepMoveRule {

    @Override
    protected int[][] offsets() {
        return Directions.KNIGHT;
    }
}
