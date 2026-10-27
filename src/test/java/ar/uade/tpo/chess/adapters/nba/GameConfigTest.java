package ar.uade.tpo.chess.adapters.nba;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameConfigTest {

    @Test
    void standardHasNoMechanicsAndIsStandardChess() {
        GameConfig config = GameConfig.standard();
        assertFalse(config.shotClockEnabled());
        assertFalse(config.tradeEnabled());
        assertFalse(config.hotHandEnabled());
        assertTrue(config.isStandardChess());
        assertEquals(GameConfig.DEFAULT_SHOT_CLOCK_LIMIT, config.shotClockLimit());
    }

    @Test
    void theScoreboardIsIndependentAndOffByDefault() {
        assertFalse(GameConfig.standard().scoreboardEnabled());
        GameConfig scoreboard = GameConfig.builder().scoreboard().build();
        assertTrue(scoreboard.scoreboardEnabled());
        assertFalse(scoreboard.shotClockEnabled());
        assertFalse(scoreboard.tradeEnabled());
        assertFalse(scoreboard.hotHandEnabled());
        assertFalse(scoreboard.isStandardChess());
    }

    @Test
    void theDefaultShotClockLimitIsTen() {
        assertEquals(10, GameConfig.DEFAULT_SHOT_CLOCK_LIMIT);
        assertEquals(10, GameConfig.builder().shotClock().build().shotClockLimit());
    }

    @Test
    void theBuilderEnablesEachMechanicIndependently() {
        GameConfig shotClock = GameConfig.builder().shotClock(4).build();
        assertTrue(shotClock.shotClockEnabled());
        assertEquals(4, shotClock.shotClockLimit());
        assertFalse(shotClock.tradeEnabled());
        assertFalse(shotClock.hotHandEnabled());
        assertFalse(shotClock.isStandardChess());

        GameConfig trade = GameConfig.builder().trade().build();
        assertTrue(trade.tradeEnabled());
        assertFalse(trade.shotClockEnabled());
        assertFalse(trade.hotHandEnabled());

        GameConfig hotHand = GameConfig.builder().hotHand().build();
        assertTrue(hotHand.hotHandEnabled());
        assertFalse(hotHand.shotClockEnabled());
        assertFalse(hotHand.tradeEnabled());
    }

    @Test
    void anyCombinationCanBeBuilt() {
        GameConfig all = GameConfig.builder().shotClock(6).trade().hotHand().build();
        assertTrue(all.shotClockEnabled() && all.tradeEnabled() && all.hotHandEnabled());
        assertEquals(6, all.shotClockLimit());
        GameConfig none = GameConfig.builder().build();
        assertTrue(none.isStandardChess());
    }

    @Test
    void theShotClockLimitMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> GameConfig.builder().shotClock(0).build());
        assertThrows(IllegalArgumentException.class, () -> GameConfig.builder().shotClock(-1).build());
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(false, 0, false, false, false));
    }

    @Test
    void configsAreValuesWithEquality() {
        assertEquals(GameConfig.builder().trade().build(), GameConfig.builder().trade().build());
        assertEquals(GameConfig.standard(), GameConfig.builder().build());
    }
}
