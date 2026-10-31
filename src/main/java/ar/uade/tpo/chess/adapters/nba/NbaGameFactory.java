package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.game.ChessGame;
import ar.uade.tpo.chess.core.game.Game;

/**
 * Punto de composición de las mecánicas de la NBA: crea la partida y la envuelve con los decoradores indicados
 * en {@link GameConfig}. Con la configuración estándar devuelve {@link Game} sin modificar.
 *
 * <p>El decorador de mano caliente queda por fuera del reloj de posesión. Después de cada jugada se evalúa
 * primero la racha y luego el reloj; por último, el núcleo pasa el turno. Ambos mecanismos funcionan en
 * conjunto: si se salta un turno, el mismo bando ya conserva la jugada y no recibe otra adicional.
 *
 * <p>El marcador queda por fuera de los demás: suma puntos después de las capturas y resuelve las partidas
 * empatadas, sin importar la posición de los otros decoradores.
 *
 * <p>El intercambio no es un decorador: cuando está habilitado, la interfaz envía una {@link TradeAction};
 * no hay nada que envolver.
 */
public final class NbaGameFactory {

    private NbaGameFactory() {
    }

    public static ChessGame create(GameConfig config, ShotClockListener... shotClockListeners) {
        return decorate(new Game(), config, shotClockListeners);
    }

    /** Envuelve una partida existente, por ejemplo, una creada desde una posición personalizada. */
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
        if (config.scoreboardEnabled()) {
            game = new ScoreboardGame(game);
        }
        return game;
    }
}
