package ar.uade.tpo.chess.core.rules;

/** Queen: any distance along ranks, files and diagonals. */
public final class QueenMoveRule extends LinearMoveRule {

    @Override
    protected int[][] directions() {
        return Directions.ALL;
    }
}
