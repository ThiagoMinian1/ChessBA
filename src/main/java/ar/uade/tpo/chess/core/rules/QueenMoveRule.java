package ar.uade.tpo.chess.core.rules;

/** Dama: se desplaza cualquier cantidad de casillas en filas, columnas y diagonales. */
public final class QueenMoveRule extends LinearMoveRule {

    @Override
    protected int[][] directions() {
        return Directions.ALL;
    }
}
