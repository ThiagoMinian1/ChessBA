package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.game.ChessGame;
import ar.uade.tpo.chess.core.game.Game;
import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static ar.uade.tpo.chess.TestSupport.play;
import static ar.uade.tpo.chess.TestSupport.pos;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NbaGameFactoryTest {

    @Test
    void theStandardConfigurationGivesTheUntouchedCoreGame() {
        assertEquals(Game.class, NbaGameFactory.create(GameConfig.standard()).getClass());
    }

    @Test
    void eachMechanicAddsItsOwnDecorator() {
        assertEquals(ShotClockGame.class,
                NbaGameFactory.create(GameConfig.builder().shotClock().build()).getClass());
        assertEquals(HotHandGame.class,
                NbaGameFactory.create(GameConfig.builder().hotHand().build()).getClass());
    }

    @Test
    void theTradeNeedsNoDecoratorBecauseItIsAnAction() {
        assertEquals(Game.class, NbaGameFactory.create(GameConfig.builder().trade().build()).getClass());
    }

    @Test
    void withEveryMechanicTheOuterDecoratorIsTheHotHand() {
        ChessGame game = NbaGameFactory.create(GameConfig.builder().shotClock().trade().hotHand().build());
        assertEquals(HotHandGame.class, game.getClass());
    }

    @Test
    void theScoreboardAloneProducesAScoreboardGame() {
        ChessGame game = NbaGameFactory.create(GameConfig.builder().scoreboard().build());
        assertEquals(ScoreboardGame.class, game.getClass());
    }

    @Test
    void withEveryMechanicIncludingTheScoreboardItIsOutermost() {
        ChessGame game = NbaGameFactory.create(
                GameConfig.builder().shotClock().trade().hotHand().scoreboard().build());
        assertEquals(ScoreboardGame.class, game.getClass());
    }

    @Test
    void shotClockListenersPassedToTheFactoryAreRegistered() {
        List<String> heard = new ArrayList<>();
        ChessGame game = NbaGameFactory.decorate(
                ar.uade.tpo.chess.TestSupport.gameOf(Team.WHITE, "Kg1", "Ra1", "kg8", "ra8"),
                GameConfig.builder().shotClock(1).build(),
                new ShotClockListener() {
                    @Override
                    public void onShotClockViolation(Team team) {
                        heard.add("violation " + team);
                    }
                });
        play(game, "a1b1", "a8b8");
        assertEquals(List.of("violation WHITE"), heard);
        assertEquals(Team.BLACK, game.currentTurn());
    }

    @Test
    void aWholeGameWithEveryMechanicOnStillFollowsChessRules() {
        ChessGame game = NbaGameFactory.create(GameConfig.builder().shotClock().trade().hotHand().build());
        play(game, "e2e4", "e7e5", "d1h5", "b8c6", "f1c4", "g8f6", "h5f7");
        assertEquals(GameStatus.CHECKMATE, game.status());
        assertEquals(Team.WHITE, game.winner().orElseThrow());
    }

    @Test
    void undoingEverythingWithAllMechanicsOnReturnsToTheStartingPosition() {
        ChessGame game = NbaGameFactory.create(GameConfig.builder().shotClock().trade().hotHand().build());
        play(game, "e2e4", "d7d5", "e4d5", "d8d5", "b1c3", "d5a5");
        assertEquals(6, game.history().size());
        while (game.canUndo()) {
            game.undo();
        }
        assertEquals(Board.standard(), game.board());
        assertEquals(Team.WHITE, game.currentTurn());
        assertTrue(game.bench(Team.WHITE).isEmpty());
        assertTrue(game.bench(Team.BLACK).isEmpty());
        assertEquals(ShotClockState.fresh(), ShotClockGame.stateOf(game, Team.WHITE));
        assertEquals(0, HotHandGame.streakOf(game, Team.WHITE));
    }

    @Test
    void capturesFeedTheBenchWhateverTheConfiguration() {
        ChessGame game = NbaGameFactory.create(GameConfig.builder().trade().build());
        play(game, "e2e4", "d7d5", "e4d5");
        assertEquals(PieceType.PAWN, game.bench(Team.BLACK).get(0).type());
        game.execute(new TradeAction(pos("c7"), PieceType.PAWN));
        assertTrue(TradeAction.hasTraded(game, Team.BLACK));
    }
}
