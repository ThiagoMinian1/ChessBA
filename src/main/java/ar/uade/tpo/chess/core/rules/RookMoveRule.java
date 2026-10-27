package ar.uade.tpo.chess.core.rules;

/** Rook: any distance along ranks and files. */
public final class RookMoveRule extends LinearMoveRule {

    @Override
    protected int[][] directions() {
        return Directions.ORTHOGONAL;
    }
}
