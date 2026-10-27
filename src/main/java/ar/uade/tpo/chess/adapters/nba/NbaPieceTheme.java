package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Team;
import ar.uade.tpo.chess.ports.PieceTheme;

import java.util.Objects;

/**
 * Implementation of the {@link PieceTheme} port with an NBA look: each side plays as a {@link Franchise} and
 * its pieces are named after basketball roles, e.g. "Lakers Point Guard" or "Celtics Center". Purely
 * presentational - it changes no rule.
 *
 * <p>Role mapping: king = Franchise Player, queen = Point Guard (the most mobile playmaker), rook = Center,
 * bishop = Small Forward, knight = Shooting Guard, pawn = Rookie. No real player names are used.
 */
public final class NbaPieceTheme implements PieceTheme {

    private final Franchise white;
    private final Franchise black;

    public NbaPieceTheme(Franchise white, Franchise black) {
        this.white = Objects.requireNonNull(white, "white");
        this.black = Objects.requireNonNull(black, "black");
        if (white == black) {
            throw new IllegalArgumentException("Both teams cannot be the same franchise: " + white);
        }
    }

    public Franchise franchiseOf(Team team) {
        return team == Team.WHITE ? white : black;
    }

    @Override
    public String teamName(Team team) {
        return franchiseOf(team).displayName();
    }

    @Override
    public String pieceName(PieceType type, Team team) {
        return franchiseOf(team).nickname() + " " + roleName(type);
    }

    /** Franchise abbreviation plus role code, e.g. "LAL-PG". */
    @Override
    public String icon(PieceType type, Team team) {
        return franchiseOf(team).abbreviation() + "-" + roleCode(type);
    }

    /** The basketball role a piece type stands for, e.g. "Point Guard" for the queen. */
    public static String roleName(PieceType type) {
        return switch (type) {
            case KING -> "Franchise Player";
            case QUEEN -> "Point Guard";
            case ROOK -> "Center";
            case BISHOP -> "Small Forward";
            case KNIGHT -> "Shooting Guard";
            case PAWN -> "Rookie";
        };
    }

    public static String roleCode(PieceType type) {
        return switch (type) {
            case KING -> "FP";
            case QUEEN -> "PG";
            case ROOK -> "C";
            case BISHOP -> "SF";
            case KNIGHT -> "SG";
            case PAWN -> "RK";
        };
    }
}
