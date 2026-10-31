package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.game.ChessGame;
import ar.uade.tpo.chess.core.game.GameDecorator;
import ar.uade.tpo.chess.core.game.TurnAction;
import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.Team;

import java.util.Optional;

/**
 * Mecánica 4: marcador (patrón Decorator).
 *
 * <p>Cada captura suma al captor el valor de la pieza capturada (peón 1, caballo 3, alfil 3, torre 5 y dama 9).
 * Si la partida termina en tablas, gana el bando con más puntos.
 *
 * <p><b>Regla obligatoria del TP que modifica: el resultado de una partida en tablas.</b> En el ajedrez estándar
 * hay varias formas de empatar; con esta mecánica, el ahogado, la repetición triple, la regla de los 50 movimientos
 * y el material insuficiente se deciden por puntos. Solo siguen siendo tablas si el marcador está igualado.
 * Las tablas <i>por acuerdo</i> no se deciden por puntos: las eligieron ambos jugadores. {@link #status()} sigue
 * informando el resultado real del ajedrez (por ejemplo, {@code DRAW_FIFTY_MOVE_RULE}); solo {@link #winner()}
 * refleja el marcador, y {@link #decidedOnPoints(ChessGame)} permite saber qué resultado se aplicó.
 *
 * <p>Para desactivarlo, no se agrega este decorador. Los puntos se guardan en las extensiones del estado, así
 * que al deshacer o rehacer una jugada el marcador se mantiene sincronizado con el tablero.
 */
public final class ScoreboardGame extends GameDecorator {

    public ScoreboardGame(ChessGame delegate) {
        super(delegate);
    }

    @Override
    public void execute(TurnAction action) {
        delegate.execute(new ScoreboardAction(action));
    }

    @Override
    public Optional<Team> winner() {
        Optional<Team> chessWinner = delegate.winner();
        return chessWinner.isPresent() ? chessWinner : pointsWinner(delegate);
    }

    /** Puntos de {@code team} en la partida (0 si esta mecánica está desactivada). */
    public static int scoreOf(ChessGame game, Team team) {
        return game.extension(ScoreboardAction.KEY, Scores.class).orElseGet(Scores::fresh).of(team);
    }

    /** Indica si el marcador convirtió en victoria unas tablas que no fueron por acuerdo. */
    public static boolean decidedOnPoints(ChessGame game) {
        return pointsWinner(game).isPresent();
    }

    private static Optional<Team> pointsWinner(ChessGame game) {
        if (!isPointsDraw(game.status())) {
            return Optional.empty();
        }
        int white = scoreOf(game, Team.WHITE);
        int black = scoreOf(game, Team.BLACK);
        if (white == black) {
            return Optional.empty();
        }
        return Optional.of(white > black ? Team.WHITE : Team.BLACK);
    }

    private static boolean isPointsDraw(GameStatus status) {
        return status.isDraw() && status != GameStatus.DRAW_BY_AGREEMENT;
    }
}
