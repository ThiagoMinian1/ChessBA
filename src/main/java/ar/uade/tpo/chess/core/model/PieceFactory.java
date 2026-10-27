package ar.uade.tpo.chess.core.model;

/** Creates pieces from a {@link PieceType}; used when promoting and when parsing diagrams. */
public final class PieceFactory {

    private PieceFactory() {
    }

    public static Piece create(PieceType type, Team team) {
        return create(type, team, false);
    }

    public static Piece create(PieceType type, Team team, boolean moved) {
        return switch (type) {
            case KING -> new King(team, moved);
            case QUEEN -> new Queen(team, moved);
            case ROOK -> new Rook(team, moved);
            case BISHOP -> new Bishop(team, moved);
            case KNIGHT -> new Knight(team, moved);
            case PAWN -> new Pawn(team, moved);
        };
    }
}
