package ar.uade.tpo.chess.ports;

import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Team;

/**
 * Outbound port: how a piece and a team are presented. The core only ever deals with {@link Team} and
 * {@link PieceType}; an adapter implements this to translate them into names and icons of whatever theme
 * is in use, so the core never learns which theme that is.
 */
public interface PieceTheme {

    /** Display name of a side, e.g. "White" or the name of a franchise. */
    String teamName(Team team);

    /** Display name of a piece of the given side, e.g. "White Knight" or "Lakers Shooting Guard". */
    String pieceName(PieceType type, Team team);

    /** Short icon or code for a piece, suitable for compact displays. */
    String icon(PieceType type, Team team);
}
