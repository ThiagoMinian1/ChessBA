package ar.uade.tpo.chess.core.model;

public final class Pawn extends Piece {

    public Pawn(Team team) {
        this(team, false);
    }

    public Pawn(Team team, boolean moved) {
        super(team, moved);
    }

    @Override
    public PieceType type() {
        return PieceType.PAWN;
    }

    @Override
    public Piece withMoved(boolean moved) {
        return new Pawn(team(), moved);
    }

    @Override
    public int materialValue() {
        return 1;
    }
}
