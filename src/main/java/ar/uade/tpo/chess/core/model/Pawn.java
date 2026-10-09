package ar.uade.tpo.chess.core.model;

public final class Pawn extends Piece {

    public Pawn(Team team) {
        super(team);
    }

    @Override
    public PieceType getType() {
        return PieceType.PAWN;
    }

    @Override
    public Piece copy() {
        return new Pawn(getTeam());
    }

    /** Row a pawn of this team starts on, which is when it may move two squares. */
    public int startingRow() {
        return getTeam() == Team.WHITE ? 1 : 6;
    }

    /** +1 for White (moving up the board), -1 for Black (moving down). */
    public int forwardDirection() {
        return getTeam() == Team.WHITE ? 1 : -1;
    }
}
