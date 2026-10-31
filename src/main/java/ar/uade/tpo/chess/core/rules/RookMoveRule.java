package ar.uade.tpo.chess.core.rules;

/** Torre: se desplaza cualquier cantidad de casillas en filas y columnas. */
public final class RookMoveRule extends LinearMoveRule {

    @Override
    protected int[][] directions() {
        return Directions.ORTHOGONAL;
    }
}
