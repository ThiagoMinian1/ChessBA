package ar.uade.tpo.chess.core.model;

public final class Knight extends Piece {

    public Knight(Team team) {
        this(team, false);
    }

    public Knight(Team team, boolean moved) {
        super(team, moved);
    }

    @Override
    public PieceType type() {
        return PieceType.KNIGHT;
    }

    @Override
    public Piece withMoved(boolean moved) {
        return new Knight(team(), moved);
    }

    @Override
    public int materialValue() {
        return 3;
    }
}
