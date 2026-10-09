package ar.uade.tpo.chess.core.model;

public final class King extends Piece {

    public King(Team team) {
        super(team);
    }

    @Override
    public PieceType getType() {
        return PieceType.KING;
    }

    @Override
    public Piece copy() {
        return new King(getTeam());
    }
}
