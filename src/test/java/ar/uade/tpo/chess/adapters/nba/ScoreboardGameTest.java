package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.game.ChessGame;
import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static ar.uade.tpo.chess.TestSupport.gameOf;
import static ar.uade.tpo.chess.TestSupport.play;
import static ar.uade.tpo.chess.TestSupport.pos;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreboardGameTest {

    private static final GameConfig SCOREBOARD = GameConfig.builder().scoreboard().build();

    private static ChessGame scoreboard(Team turn, String... placements) {
        return NbaGameFactory.decorate(gameOf(turn, placements), SCOREBOARD);
    }

    // ---- Points ----------------------------------------------------------------------------------------

    @Test
    void everyCaptureScoresTheConventionalValueOfThePiece() {
        String[][] cases = {{"p", "1"}, {"n", "3"}, {"b", "3"}, {"r", "5"}, {"q", "9"}};
        for (String[] c : cases) {
            ChessGame game = scoreboard(Team.WHITE, "Ke1", "Ra1", "kh8", c[0] + "a5");
            play(game, "a1a5");
            assertEquals(Integer.parseInt(c[1]), ScoreboardGame.scoreOf(game, Team.WHITE), "capturing a " + c[0]);
            assertEquals(0, ScoreboardGame.scoreOf(game, Team.BLACK));
        }
    }

    @Test
    void quietMovesDoNotScore() {
        ChessGame game = NbaGameFactory.create(SCOREBOARD);
        play(game, "e2e4", "e7e5", "g1f3");
        assertEquals(0, ScoreboardGame.scoreOf(game, Team.WHITE));
        assertEquals(0, ScoreboardGame.scoreOf(game, Team.BLACK));
    }

    @Test
    void eachTeamKeepsItsOwnScore() {
        ChessGame game = NbaGameFactory.create(SCOREBOARD);
        play(game, "e2e4", "d7d5", "e4d5", "d8d5", "b1c3", "d5g2", "f1g2");
        // white took a pawn (1) and the queen (9); black took two pawns (1 + 1).
        assertEquals(10, ScoreboardGame.scoreOf(game, Team.WHITE));
        assertEquals(2, ScoreboardGame.scoreOf(game, Team.BLACK));
    }

    @Test
    void anEnPassantCaptureScoresAPawn() {
        ChessGame game = scoreboard(Team.BLACK, "Ke1", "kh8", "Pe5", "pd7");
        play(game, "d7d5", "e5d6");
        assertEquals(1, ScoreboardGame.scoreOf(game, Team.WHITE));
    }

    @Test
    void capturingAPromotedPieceScoresItAsThePieceItBecame() {
        ChessGame game = scoreboard(Team.WHITE, "Ka1", "kh6", "Pe7", "Pa3", "rh8");
        play(game, "e7e8q", "h8e8");
        assertEquals(9, ScoreboardGame.scoreOf(game, Team.BLACK), "a queen now, not a pawn");
    }

    @Test
    void aPromotionByCaptureScoresTheCapturedPiece() {
        ChessGame game = scoreboard(Team.WHITE, "Ka1", "kh6", "Pe7", "rd8");
        play(game, "e7d8q");
        assertEquals(5, ScoreboardGame.scoreOf(game, Team.WHITE));
    }

    @Test
    void aTradeNeverScores() {
        ChessGame game = NbaGameFactory.create(GameConfig.builder().scoreboard().trade().build());
        play(game, "e2e4", "d7d5", "e4d5");
        game.execute(new TradeAction(pos("c7"), PieceType.PAWN));
        assertEquals(1, ScoreboardGame.scoreOf(game, Team.WHITE));
        assertEquals(0, ScoreboardGame.scoreOf(game, Team.BLACK));
    }

    @Test
    void undoAndRedoKeepThePointsInStepWithTheBoard() {
        ChessGame game = NbaGameFactory.create(SCOREBOARD);
        play(game, "e2e4", "d7d5", "e4d5");
        assertEquals(1, ScoreboardGame.scoreOf(game, Team.WHITE));
        game.undo();
        assertEquals(0, ScoreboardGame.scoreOf(game, Team.WHITE));
        game.redo();
        assertEquals(1, ScoreboardGame.scoreOf(game, Team.WHITE));
    }

    @Test
    void aCaptureAnnouncesTheScoreToObservers() {
        ChessGame game = NbaGameFactory.create(SCOREBOARD);
        List<String> notices = new ArrayList<>();
        game.addObserver(new ar.uade.tpo.chess.ports.GameObserver() {
            @Override
            public void onNotice(String message) {
                notices.add(message);
            }
        });
        play(game, "e2e4", "d7d5", "e4d5");
        assertEquals(List.of("Scoreboard: WHITE scores 1 point (1-0)"), notices);
    }

    // ---- Deciding drawn games --------------------------------------------------------------------------

    @Test
    void aDrawByRuleGoesToTheTeamWithMorePoints() {
        // Bxf4 leaves king + bishop against a bare king: a draw by insufficient material, 3 points to white.
        ChessGame game = scoreboard(Team.WHITE, "Ka1", "Bc1", "kh8", "nf4");
        play(game, "c1f4");
        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, game.status(), "the chess status is still the real one");
        assertEquals(Team.WHITE, game.winner().orElseThrow());
        assertTrue(ScoreboardGame.decidedOnPoints(game));
    }

    @Test
    void withoutTheScoreboardTheSameDrawIsJustADraw() {
        ChessGame game = gameOf(Team.WHITE, "Ka1", "Bc1", "kh8", "nf4");
        play(game, "c1f4");
        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, game.status());
        assertTrue(game.winner().isEmpty());
        assertFalse(ScoreboardGame.decidedOnPoints(game));
        assertEquals(0, ScoreboardGame.scoreOf(game, Team.WHITE), "no scoreboard, no points");
    }

    @Test
    void aDrawWithLevelScoresStaysADraw() {
        ChessGame game = NbaGameFactory.create(SCOREBOARD);
        play(game, "g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8");
        assertEquals(GameStatus.DRAW_THREEFOLD_REPETITION, game.status());
        assertTrue(game.winner().isEmpty());
        assertFalse(ScoreboardGame.decidedOnPoints(game));
    }

    @Test
    void aDrawByAgreementIsNeverDecidedOnPoints() {
        ChessGame game = scoreboard(Team.WHITE, "Ka1", "Bc1", "Rb1", "kh8", "nf4", "pa7");
        play(game, "c1f4");
        game.agreeDraw();
        assertEquals(GameStatus.DRAW_BY_AGREEMENT, game.status());
        assertEquals(3, ScoreboardGame.scoreOf(game, Team.WHITE));
        assertTrue(game.winner().isEmpty());
        assertFalse(ScoreboardGame.decidedOnPoints(game));
    }

    @Test
    void checkmateAndResignationKeepTheirWinnerWhateverThePoints() {
        ChessGame mate = NbaGameFactory.create(SCOREBOARD);
        play(mate, "f2f3", "e7e5", "g2g4", "d8h4");
        assertEquals(GameStatus.CHECKMATE, mate.status());
        assertEquals(Team.BLACK, mate.winner().orElseThrow());
        assertFalse(ScoreboardGame.decidedOnPoints(mate));

        ChessGame resigned = NbaGameFactory.create(SCOREBOARD);
        resigned.resign(Team.WHITE);
        assertEquals(Team.BLACK, resigned.winner().orElseThrow());
    }

    @Test
    void anOngoingGameHasNoWinner() {
        ChessGame game = NbaGameFactory.create(SCOREBOARD);
        play(game, "e2e4", "d7d5", "e4d5");
        assertTrue(game.winner().isEmpty());
    }

    @Test
    void undoingTheDrawingMoveRestoresAnOngoingGameWithNoWinner() {
        ChessGame game = scoreboard(Team.WHITE, "Ka1", "Bc1", "kh8", "nf4");
        play(game, "c1f4");
        game.undo();
        assertEquals(GameStatus.ONGOING, game.status());
        assertTrue(game.winner().isEmpty());
        assertEquals(0, ScoreboardGame.scoreOf(game, Team.WHITE));
    }

    // ---- Together with the other mechanics -------------------------------------------------------------

    @Test
    void itWorksTogetherWithTheOtherThreeMechanics() {
        ChessGame game = NbaGameFactory.create(
                GameConfig.builder().shotClock().trade().hotHand().scoreboard().build());
        play(game, "e2e4", "d7d5", "e4d5", "d8d5");
        assertEquals(1, ScoreboardGame.scoreOf(game, Team.WHITE));
        assertEquals(1, ScoreboardGame.scoreOf(game, Team.BLACK));
    }
}
