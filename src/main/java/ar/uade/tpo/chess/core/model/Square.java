package ar.uade.tpo.chess.core.model;

/**
 * One cell of the board: a fixed position plus whichever piece currently
 * sits there (possibly none). Mutation is package-private — only
 * {@link Board} is allowed to change what is on a square.
 */
public final class Square {

    private final Position position;
    private Piece piece;

    Square(Position position, Piece piece) {
        this.position = position;
        this.piece = piece;
    }

    public Position getPosition() {
        return position;
    }

    public Piece getPiece() {
        return piece;
    }

    public boolean isEmpty() {
        return piece == null;
    }

    public boolean isOccupiedBy(Team team) {
        return piece != null && piece.getTeam() == team;
    }

    void setPiece(Piece piece) {
        this.piece = piece;
    }
}
