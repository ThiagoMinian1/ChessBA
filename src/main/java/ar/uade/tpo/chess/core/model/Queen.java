package ar.uade.tpo.chess.core.model;

public final class Queen extends Piece {

    public Queen(Team team) {
        this(team, false);
    }

    public Queen(Team team, boolean moved) {
        super(team, moved);
    }

    @Override
    public PieceType type() {
        return PieceType.QUEEN;
    }

    @Override
    public Piece withMoved(boolean moved) {
        return new Queen(team(), moved);
    }

    @Override
    public int materialValue() {
        return 9;
    }
}
