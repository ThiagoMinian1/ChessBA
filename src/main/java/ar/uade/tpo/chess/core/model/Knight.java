package ar.uade.tpo.chess.core.model;

public final class Knight extends Piece {

    public Knight(Team team) {
        super(team);
    }

    @Override
    public PieceType getType() {
        return PieceType.KNIGHT;
    }

    @Override
    public Piece copy() {
        return new Knight(getTeam());
    }
}
