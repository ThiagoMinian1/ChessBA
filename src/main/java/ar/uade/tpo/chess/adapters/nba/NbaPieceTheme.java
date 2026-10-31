package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Team;
import ar.uade.tpo.chess.ports.PieceTheme;

import java.util.Objects;

/**
 * Implementación del puerto {@link PieceTheme} con una apariencia de la NBA: cada bando representa una
 * {@link Franchise} y sus piezas llevan nombres de posiciones de básquet, como «base de los Lakers» o
 * «pívot de los Celtics». Es solo visual; no cambia ninguna regla.
 *
 * <p>Roles: rey = jugador franquicia, dama = base (el armador más versátil), torre = pívot, alfil = alero,
 * caballo = escolta y peón = novato. No se usan nombres de jugadores reales.
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

    /** Abreviatura de la franquicia y código del rol, por ejemplo, "LAL-PG". */
    @Override
    public String icon(PieceType type, Team team) {
        return franchiseOf(team).abbreviation() + "-" + roleCode(type);
    }

    /** Posición de básquet que representa cada pieza, por ejemplo, «base» para la dama. */
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
