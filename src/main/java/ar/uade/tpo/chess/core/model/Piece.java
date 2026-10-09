package ar.uade.tpo.chess.core.model;

/**
 * Base type for every chess piece. Concrete pieces (Pawn, Rook, Knight,
 * Bishop, Queen, King) hold no board-specific state themselves: "can this
 * pawn still move two squares" is derived from its starting row instead of
 * a mutable flag, so a Piece is safe to share and cheap to copy.
 */
public abstract class Piece {

    private final Team team;

    protected Piece(Team team) {
        this.team = team;
    }

    public Team getTeam() {
        return team;
    }

    public abstract PieceType getType();

    /**
     * Returns a new, independent Piece of the same concrete type and team.
     * Used when the board is copied to simulate a move.
     */
    public abstract Piece copy();

    public boolean isSameTeam(Piece other) {
        return other != null && this.team == other.team;
    }
}
