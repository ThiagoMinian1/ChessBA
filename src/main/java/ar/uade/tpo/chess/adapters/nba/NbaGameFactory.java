package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.game.ChessGame;
import ar.uade.tpo.chess.core.game.Game;

/**
 * Composition root for the NBA mechanics: builds a game and wraps it in the decorators the {@link GameConfig}
 * asks for. With the standard configuration it returns the plain core {@link Game}, untouched.
 *
 * <p>Decorator order: the hot hand is outermost and the shot clock innermost, so after a move the streak
 * is evaluated first and the clock second, and the core then passes the turn. The two cooperate whichever
 * order they run in (a skipped turn already keeps the move with the same team, so no extra move is granted).
 *
 * <p>The trade is not a decorator: it is a {@link TradeAction} that the front end submits when the config
 * enables it, so there is nothing to wrap.
 */
public final class NbaGameFactory {

    private NbaGameFactory() {
    }

    public static ChessGame create(GameConfig config, ShotClockListener... shotClockListeners) {
        return decorate(new Game(), config, shotClockListeners);
    }

    /** Wraps an existing game (for example one built from a custom position). */
    public static ChessGame decorate(ChessGame base, GameConfig config, ShotClockListener... shotClockListeners) {
        ChessGame game = base;
        if (config.shotClockEnabled()) {
            ShotClockGame shotClock = new ShotClockGame(game, config.shotClockLimit());
            for (ShotClockListener listener : shotClockListeners) {
                shotClock.addListener(listener);
            }
            game = shotClock;
        }
        if (config.hotHandEnabled()) {
            game = new HotHandGame(game);
        }
        return game;
    }
}
