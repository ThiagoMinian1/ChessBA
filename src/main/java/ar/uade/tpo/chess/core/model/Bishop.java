package ar.uade.tpo.chess.core.model;

public final class Bishop extends Piece {

    public Bishop(Team team) {
        super(team);
    }

    @Override
    public PieceType getType() {
        return PieceType.BISHOP;
    }

    @Override
    public Piece copy() {
        return new Bishop(getTeam());
    }
}
