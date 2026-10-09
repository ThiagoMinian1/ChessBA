package ar.uade.tpo.chess.core.model;

public final class Queen extends Piece {

    public Queen(Team team) {
        super(team);
    }

    @Override
    public PieceType getType() {
        return PieceType.QUEEN;
    }

    @Override
    public Piece copy() {
        return new Queen(getTeam());
    }
}
