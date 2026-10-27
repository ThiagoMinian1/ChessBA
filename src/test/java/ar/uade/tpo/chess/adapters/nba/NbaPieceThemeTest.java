package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.adapters.StandardPieceTheme;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Team;
import ar.uade.tpo.chess.ports.PieceTheme;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NbaPieceThemeTest {

    private final NbaPieceTheme theme = new NbaPieceTheme(Franchise.LOS_ANGELES_LAKERS, Franchise.BOSTON_CELTICS);

    @Test
    void eachSideIsNamedAfterItsFranchise() {
        assertEquals("Los Angeles Lakers", theme.teamName(Team.WHITE));
        assertEquals("Boston Celtics", theme.teamName(Team.BLACK));
        assertEquals(Franchise.LOS_ANGELES_LAKERS, theme.franchiseOf(Team.WHITE));
        assertEquals(Franchise.BOSTON_CELTICS, theme.franchiseOf(Team.BLACK));
    }

    @Test
    void piecesAreNamedAfterBasketballRoles() {
        assertEquals("Lakers Point Guard", theme.pieceName(PieceType.QUEEN, Team.WHITE));
        assertEquals("Celtics Center", theme.pieceName(PieceType.ROOK, Team.BLACK));
        assertEquals("Lakers Franchise Player", theme.pieceName(PieceType.KING, Team.WHITE));
        assertEquals("Celtics Small Forward", theme.pieceName(PieceType.BISHOP, Team.BLACK));
        assertEquals("Lakers Shooting Guard", theme.pieceName(PieceType.KNIGHT, Team.WHITE));
        assertEquals("Celtics Rookie", theme.pieceName(PieceType.PAWN, Team.BLACK));
    }

    @Test
    void iconsCombineTheFranchiseCodeAndTheRoleCode() {
        assertEquals("LAL-PG", theme.icon(PieceType.QUEEN, Team.WHITE));
        assertEquals("BOS-C", theme.icon(PieceType.ROOK, Team.BLACK));
        assertEquals("LAL-FP", theme.icon(PieceType.KING, Team.WHITE));
    }

    @Test
    void everyPieceTypeHasADistinctRoleNameAndCode() {
        Set<String> names = new HashSet<>();
        Set<String> codes = new HashSet<>();
        for (PieceType type : PieceType.values()) {
            assertTrue(names.add(NbaPieceTheme.roleName(type)), "duplicate name for " + type);
            assertTrue(codes.add(NbaPieceTheme.roleCode(type)), "duplicate code for " + type);
        }
        assertEquals(6, names.size());
    }

    @Test
    void bothSidesCannotBeTheSameFranchise() {
        assertThrows(IllegalArgumentException.class,
                () -> new NbaPieceTheme(Franchise.MIAMI_HEAT, Franchise.MIAMI_HEAT));
    }

    @Test
    void theThemeIsInterchangeableWithTheStandardOneThroughThePort() {
        PieceTheme standard = new StandardPieceTheme();
        assertEquals("White", standard.teamName(Team.WHITE));
        assertEquals("Black Knight", standard.pieceName(PieceType.KNIGHT, Team.BLACK));
        assertEquals("N", standard.icon(PieceType.KNIGHT, Team.WHITE));
        assertEquals("n", standard.icon(PieceType.KNIGHT, Team.BLACK));
        assertEquals("White King", standard.pieceName(PieceType.KING, Team.WHITE));

        PieceTheme nba = theme;
        assertTrue(!nba.pieceName(PieceType.KING, Team.WHITE).equals(standard.pieceName(PieceType.KING, Team.WHITE)));
    }
}
