package ar.uade.tpo.chess.ports;

import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Team;

/**
 * Output port used only by presentation adapters, never by the core
 * itself: "given a piece type and a team, what should it be called and
 * what should it look like". This is where the NBA theme plugs in later
 * (e.g. implemented by an {@code NbaPieceTheme} adapter) — the core
 * never references a team name, a player, or an icon.
 */
public interface PieceTheme {
    String displayName(PieceType type, Team team);

    String iconFor(PieceType type, Team team);
}
