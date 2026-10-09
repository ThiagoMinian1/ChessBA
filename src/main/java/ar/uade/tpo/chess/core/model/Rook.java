package ar.uade.tpo.chess.core.model;

public final class Rook extends Piece {

    public Rook(Team team) {
        super(team);
    }

    @Override
    public PieceType getType() {
        return PieceType.ROOK;
    }

    @Override
    public Piece copy() {
        return new Rook(getTeam());
    }
}
