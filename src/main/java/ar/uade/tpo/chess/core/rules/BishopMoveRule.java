package ar.uade.tpo.chess.core.rules;

/** Alfil: se desplaza cualquier cantidad de casillas en diagonal. */
public final class BishopMoveRule extends LinearMoveRule {

    @Override
    protected int[][] directions() {
        return Directions.DIAGONAL;
    }
}
