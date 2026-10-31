package ar.uade.tpo.chess.adapters;

import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Team;
import ar.uade.tpo.chess.ports.PieceTheme;

import java.util.Locale;

/** Tema simple de {@link PieceTheme}: «Caballo blanco» e icono {@code N} (mayúscula para blancas y minúscula para negras). */
public final class StandardPieceTheme implements PieceTheme {

    @Override
    public String teamName(Team team) {
        return capitalize(team.name());
    }

    @Override
    public String pieceName(PieceType type, Team team) {
        return capitalize(team.name()) + " " + capitalize(type.name());
    }

    @Override
    public String icon(PieceType type, Team team) {
        char letter = type.letter();
        return String.valueOf(team == Team.WHITE ? letter : Character.toLowerCase(letter));
    }

    private static String capitalize(String name) {
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }
}
