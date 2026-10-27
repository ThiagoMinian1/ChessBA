package ar.uade.tpo.chess.core.model;

public final class Rook extends Piece {

    public Rook(Team team) {
        this(team, false);
    }

    public Rook(Team team, boolean moved) {
        super(team, moved);
    }

    @Override
    public PieceType type() {
        return PieceType.ROOK;
    }

    @Override
    public Piece withMoved(boolean moved) {
        return new Rook(team(), moved);
    }

    @Override
    public int materialValue() {
        return 5;
    }
}
