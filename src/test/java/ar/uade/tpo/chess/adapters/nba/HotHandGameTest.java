package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.game.ChessGame;
import ar.uade.tpo.chess.core.game.Game;
import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import static ar.uade.tpo.chess.TestSupport.gameOf;
import static ar.uade.tpo.chess.TestSupport.play;
import static ar.uade.tpo.chess.TestSupport.pos;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HotHandGameTest {

    private static final String[] PLACEMENTS = {"Ke1", "Ra1", "kh8", "pa5", "pe5"};

    private static ChessGame hotHand(String... placements) {
        return NbaGameFactory.decorate(gameOf(Team.WHITE, placements), GameConfig.builder().hotHand().build());
    }

    @Test
    void twoCapturesInARowEarnAnExtraMove() {
        ChessGame game = hotHand(PLACEMENTS);
        play(game, "a1a5");
        assertEquals(Team.BLACK, game.currentTurn());
        assertEquals(1, HotHandGame.streakOf(game, Team.WHITE));

        play(game, "h8g8");
        assertEquals(Team.WHITE, game.currentTurn());
        assertEquals(1, HotHandGame.streakOf(game, Team.WHITE), "the opponent's quiet move does not reset it");

        play(game, "a5e5");
        assertEquals(Team.WHITE, game.currentTurn(), "second capture: white moves again");
        assertEquals(0, HotHandGame.streakOf(game, Team.WHITE), "the streak is spent");

        play(game, "e1d1");
        assertEquals(Team.BLACK, game.currentTurn(), "after the bonus move the turn passes as usual");
    }

    @Test
    void theExtraMoveIsRecordedInTheHistory() {
        ChessGame game = hotHand(PLACEMENTS);
        play(game, "a1a5", "h8g8", "a5e5");
        assertEquals("a5e5 (hot hand: extra move)", game.history().get(2));
    }

    @Test
    void aQuietMoveBreaksTheStreak() {
        ChessGame game = hotHand(PLACEMENTS);
        play(game, "a1a5", "h8g8", "a5b5");
        assertEquals(0, HotHandGame.streakOf(game, Team.WHITE));
        play(game, "g8h8", "b5e5");
        assertEquals(1, HotHandGame.streakOf(game, Team.WHITE));
        assertEquals(Team.BLACK, game.currentTurn(), "only one capture in a row, so no extra move");
    }

    @Test
    void noExtraMoveWhenTheSecondCaptureGivesCheck() {
        ChessGame game = hotHand("Ke1", "Ra1", "ke8", "pa5", "pe5");
        play(game, "a1a5", "e8e7", "a5e5");
        assertTrue(game.isInCheck());
        assertEquals(Team.BLACK, game.currentTurn(), "the opponent must answer the check at once");
        assertEquals(0, HotHandGame.streakOf(game, Team.WHITE));
    }

    @Test
    void theThirdCaptureOfARunStartsANewStreak() {
        ChessGame game = hotHand("Ke1", "Ra1", "kh8", "pa5", "pe5", "pe7");
        play(game, "a1a5", "h8g8", "a5e5");
        assertEquals(Team.WHITE, game.currentTurn());
        play(game, "e5e7");
        assertEquals(Team.BLACK, game.currentTurn(), "no second bonus in a row");
        assertEquals(1, HotHandGame.streakOf(game, Team.WHITE));
    }

    @Test
    void undoRevertsTheBonusMoveAndTheStreakWithIt() {
        ChessGame game = hotHand(PLACEMENTS);
        play(game, "a1a5", "h8g8", "a5e5");

        assertTrue(game.undo());
        assertEquals(Team.WHITE, game.currentTurn());
        assertEquals(1, HotHandGame.streakOf(game, Team.WHITE));
        assertTrue(game.board().pieceAt(pos("e5")).isPresent());

        assertTrue(game.undo());
        assertEquals(Team.BLACK, game.currentTurn());

        assertTrue(game.redo());
        assertTrue(game.redo());
        assertEquals(Team.WHITE, game.currentTurn());
        assertEquals(0, HotHandGame.streakOf(game, Team.WHITE));
        assertTrue(game.board().pieceAt(pos("e5")).isPresent());
    }

    @Test
    void streaksAreKeptPerTeam() {
        ChessGame game = hotHand("Ke1", "Ra1", "kh8", "pa5", "rb8", "Pb2");
        play(game, "a1a5");
        assertEquals(1, HotHandGame.streakOf(game, Team.WHITE));
        assertEquals(0, HotHandGame.streakOf(game, Team.BLACK));
        play(game, "b8b2");
        assertEquals(1, HotHandGame.streakOf(game, Team.WHITE));
        assertEquals(1, HotHandGame.streakOf(game, Team.BLACK));
    }

    // ---- Mechanic off ----------------------------------------------------------------------------------

    @Test
    void withoutTheMechanicTwoCapturesDoNotChangeTheTurnOrder() {
        Game base = gameOf(Team.WHITE, PLACEMENTS);
        ChessGame game = NbaGameFactory.decorate(base, GameConfig.standard());
        assertSame(base, game);
        play(game, "a1a5", "h8g8", "a5e5");
        assertEquals(Team.BLACK, game.currentTurn());
        assertEquals(0, HotHandGame.streakOf(game, Team.WHITE), "nothing is tracked when the mechanic is off");
    }

    @Test
    void theMechanicDoesNotAlterOrdinaryChessRules() {
        ChessGame bare = NbaGameFactory.decorate(
                gameOf(Team.WHITE, "Ke1", "ke8"), GameConfig.builder().hotHand().build());
        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, bare.status());

        ChessGame fools = NbaGameFactory.decorate(new Game(), GameConfig.builder().hotHand().build());
        play(fools, "f2f3", "e7e5", "g2g4", "d8h4");
        assertEquals(GameStatus.CHECKMATE, fools.status());
        assertEquals(Team.BLACK, fools.winner().orElseThrow());
    }
}
