package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.game.ChessGame;
import ar.uade.tpo.chess.core.game.Game;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static ar.uade.tpo.chess.TestSupport.gameOf;
import static ar.uade.tpo.chess.TestSupport.play;
import static ar.uade.tpo.chess.TestSupport.pos;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShotClockGameTest {

    private List<String> heard;
    private ShotClockListener listener;

    @BeforeEach
    void setUp() {
        heard = new ArrayList<>();
        listener = new ShotClockListener() {
            @Override
            public void onShotClockWarning(Team team, int movesRemaining) {
                heard.add("warn " + team + " " + movesRemaining);
            }

            @Override
            public void onShotClockViolation(Team team) {
                heard.add("violation " + team);
            }
        };
    }

    private ChessGame withClock(int limit, Team turn, String... placements) {
        GameConfig config = GameConfig.builder().shotClock(limit).build();
        return NbaGameFactory.decorate(gameOf(turn, placements), config, listener);
    }

    // ---- The clock runs and expires --------------------------------------------------------------------

    @Test
    void anExpiredClockCostsTheTeamItsNextTurn() {
        ChessGame game = withClock(2, Team.WHITE, "Kg1", "Ra1", "kg8", "ra8");
        play(game, "a1b1", "a8b8", "b1c1");
        assertEquals(Team.BLACK, game.currentTurn());
        assertTrue(ShotClockGame.stateOf(game, Team.WHITE).isExpired());

        play(game, "b8c8");
        // White's clock had expired, so white loses its turn and black moves again.
        assertEquals(Team.BLACK, game.currentTurn());
        assertEquals(ShotClockState.fresh(), ShotClockGame.stateOf(game, Team.WHITE), "the clock resets after a skip");
        assertEquals(List.of("warn WHITE 1", "warn BLACK 1", "violation WHITE"), heard);
    }

    @Test
    void theMoveThatCausedTheSkipAndTheSkipAreOneHistoryEntry() {
        ChessGame game = withClock(2, Team.WHITE, "Kg1", "Ra1", "kg8", "ra8");
        play(game, "a1b1", "a8b8", "b1c1", "b8c8");
        assertEquals(4, game.history().size());
        assertEquals("b8c8 (shot clock: opponent's turn skipped)", game.history().get(3));
    }

    @Test
    void undoRevertsTheMoveAndTheSkipTogether() {
        ChessGame game = withClock(2, Team.WHITE, "Kg1", "Ra1", "kg8", "ra8");
        play(game, "a1b1", "a8b8", "b1c1", "b8c8");

        assertTrue(game.undo());
        assertEquals(Team.BLACK, game.currentTurn());
        assertTrue(game.board().pieceAt(pos("b8")).isPresent());
        assertTrue(game.board().isEmpty(pos("c8")));
        assertTrue(ShotClockGame.stateOf(game, Team.WHITE).isExpired());
        assertEquals(2, ShotClockGame.stateOf(game, Team.WHITE).movesWithoutProgress());
        assertEquals(1, ShotClockGame.stateOf(game, Team.BLACK).movesWithoutProgress());

        assertTrue(game.redo());
        assertEquals(Team.BLACK, game.currentTurn());
        assertEquals(ShotClockState.fresh(), ShotClockGame.stateOf(game, Team.WHITE));
        assertTrue(game.board().pieceAt(pos("c8")).isPresent());
    }

    @Test
    void skipsKeepHappeningWhileClocksKeepExpiring() {
        ChessGame game = withClock(2, Team.WHITE, "Kg1", "Ra1", "kg8", "ra8");
        play(game, "a1b1", "a8b8", "b1c1", "b8c8");
        assertEquals(Team.BLACK, game.currentTurn());
        play(game, "c8d8");
        assertEquals(Team.WHITE, game.currentTurn(), "white's clock was reset, so no skip this time");
        play(game, "c1d1");
        assertEquals(Team.WHITE, game.currentTurn(), "now black's expired clock costs black its turn");
        assertEquals("violation BLACK", heard.get(heard.size() - 1));
    }

    // ---- Progress resets the clock ---------------------------------------------------------------------

    @Test
    void movingAPawnCountsAsProgress() {
        ChessGame game = withClock(3, Team.WHITE, "Kg1", "Rb1", "Pa2", "kg8", "ra8");
        play(game, "b1c1", "a8b8");
        assertEquals(1, ShotClockGame.stateOf(game, Team.WHITE).movesWithoutProgress());
        play(game, "a2a3");
        assertEquals(ShotClockState.fresh(), ShotClockGame.stateOf(game, Team.WHITE));
        assertEquals(1, ShotClockGame.stateOf(game, Team.BLACK).movesWithoutProgress());
    }

    @Test
    void capturingAPieceCountsAsProgress() {
        ChessGame game = withClock(3, Team.WHITE, "Kg1", "Rb1", "kg8", "ra8", "pb5");
        play(game, "b1b2", "a8a7");
        assertEquals(1, ShotClockGame.stateOf(game, Team.WHITE).movesWithoutProgress());
        play(game, "b2b5");
        assertEquals(ShotClockState.fresh(), ShotClockGame.stateOf(game, Team.WHITE));
    }

    @Test
    void quietPieceMovesAreNotProgress() {
        ChessGame game = withClock(5, Team.WHITE, "Kg1", "Ra1", "kg8", "ra8");
        play(game, "a1b1", "a8b8", "b1a1", "b8a8");
        assertEquals(2, ShotClockGame.stateOf(game, Team.WHITE).movesWithoutProgress());
        assertEquals(2, ShotClockGame.stateOf(game, Team.BLACK).movesWithoutProgress());
    }

    // ---- Observer notifications ------------------------------------------------------------------------

    @Test
    void teamsAreWarnedWhenThreeMovesRemain() {
        ChessGame game = withClock(5, Team.WHITE, "Kg1", "Ra1", "kg8", "ra8");
        play(game, "a1b1", "a8b8", "b1a1", "b8a8");
        assertEquals(List.of("warn WHITE 3", "warn BLACK 3"), heard);
        play(game, "a1b1", "a8b8", "b1a1", "b8a8");
        assertEquals(List.of("warn WHITE 3", "warn BLACK 3", "warn WHITE 2", "warn BLACK 2",
                "warn WHITE 1", "warn BLACK 1"), heard);
    }

    @Test
    void listenersCanBeAddedAndRemoved() {
        ShotClockGame game = new ShotClockGame(gameOf(Team.WHITE, "Kg1", "Ra1", "kg8", "ra8"), 2);
        game.addListener(listener);
        play(game, "a1b1");
        assertEquals(List.of("warn WHITE 1"), heard);
        game.removeListener(listener);
        play(game, "a8b8");
        assertEquals(List.of("warn WHITE 1"), heard);
    }

    // ---- Interaction with check ------------------------------------------------------------------------

    @Test
    void aTeamInCheckKeepsItsTurnEvenWithAnExpiredClock() {
        ChessGame game = withClock(1, Team.WHITE, "Kh1", "Rb2", "kh8", "ra8");
        play(game, "b2c2", "a8a1");
        assertTrue(game.isInCheck());
        assertEquals(Team.WHITE, game.currentTurn());
        assertTrue(heard.isEmpty(), "no violation was declared");
        assertTrue(ShotClockGame.stateOf(game, Team.WHITE).isExpired(), "white is still on an expired clock");

        play(game, "h1g2");
        // White escaped the check; now it is black (expired and not in check) who loses a turn.
        assertEquals(Team.WHITE, game.currentTurn());
        assertEquals(List.of("violation BLACK"), heard);
    }

    // ---- Mechanic off, and construction ----------------------------------------------------------------

    @Test
    void withoutTheMechanicTurnsAlwaysAlternate() {
        Game base = gameOf(Team.WHITE, "Kg1", "Ra1", "kg8", "ra8");
        ChessGame game = NbaGameFactory.decorate(base, GameConfig.standard());
        assertSame(base, game, "no decorator is added for the standard configuration");
        // Sixteen quiet rook moves that never repeat a position (so the repetition rule stays out of the way).
        String[] moves = {
            "a1b1", "a8b8", "b1b2", "b8b7", "b2c2", "b7c7", "c2c3", "c7c6",
            "c3d3", "c6d6", "d3d4", "d6d5", "d4e4", "d5e5", "e4e3", "e5e6"};
        Team expected = Team.WHITE;
        for (String move : moves) {
            assertEquals(expected, game.currentTurn());
            play(game, move);
            expected = expected.opposite();
        }
        assertEquals(Team.WHITE, game.currentTurn());
    }

    @Test
    void theLimitMustBeAtLeastOne() {
        assertThrows(IllegalArgumentException.class, () -> new ShotClockGame(new Game(), 0));
        assertThrows(IllegalArgumentException.class, () -> new ShotClockGame(new Game(), -3));
        assertEquals(GameConfig.DEFAULT_SHOT_CLOCK_LIMIT, new ShotClockGame(new Game()).limit());
        assertEquals(7, new ShotClockGame(new Game(), 7).limit());
    }

    @Test
    void theStateOfAGameWithoutTheMechanicIsAFreshClock() {
        assertEquals(ShotClockState.fresh(), ShotClockGame.stateOf(new Game(), Team.WHITE));
        assertFalse(ShotClockGame.stateOf(new Game(), Team.BLACK).isExpired());
    }

    @Test
    void theClockDoesNotTouchOrdinaryChessRules() {
        ChessGame game = NbaGameFactory.decorate(new Game(), GameConfig.builder().shotClock().build());
        play(game, "f2f3", "e7e5", "g2g4", "d8h4");
        assertEquals(ar.uade.tpo.chess.core.model.GameStatus.CHECKMATE, game.status());
        assertEquals(Team.BLACK, game.winner().orElseThrow());
    }
}
