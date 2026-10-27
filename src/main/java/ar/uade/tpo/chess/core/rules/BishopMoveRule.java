package ar.uade.tpo.chess.core.rules;

/** Bishop: any distance along diagonals. */
public final class BishopMoveRule extends LinearMoveRule {

    @Override
    protected int[][] directions() {
        return Directions.DIAGONAL;
    }
}
