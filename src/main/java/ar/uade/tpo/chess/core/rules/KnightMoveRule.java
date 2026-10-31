package ar.uade.tpo.chess.core.rules;

/** Caballo: salta en forma de L (dos casillas en una dirección y una en la otra), incluso por encima de otras piezas. */
public final class KnightMoveRule extends StepMoveRule {

    @Override
    protected int[][] offsets() {
        return Directions.KNIGHT;
    }
}
