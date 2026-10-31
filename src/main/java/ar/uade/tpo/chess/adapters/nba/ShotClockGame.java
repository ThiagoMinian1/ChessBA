package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.game.ChessGame;
import ar.uade.tpo.chess.core.game.GameDecorator;
import ar.uade.tpo.chess.core.game.TurnAction;
import ar.uade.tpo.chess.core.model.Team;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Mecánica 1: reloj de posesión (patrones Decorator, State y Observer).
 *
 * <p>Cada bando debe progresar (capturar una pieza o avanzar un peón) dentro de {@code limit} jugadas propias.
 * Si no lo hace, se salta su siguiente turno y se reinicia el reloj; el rival juega dos veces seguidas.
 *
 * <ul>
 *   <li><b>Decorator:</b> envuelve cualquier {@link ChessGame} sin intervenir en la validación del núcleo.</li>
 *   <li><b>State:</b> el reloj de cada bando se representa con {@link ShotClockState}.</li>
 *   <li><b>Observer:</b> {@link ShotClockListener} recibe avisos y notificaciones de infracciones.</li>
 * </ul>
 *
 * <p>Regla obligatoria del TP que modifica: <em>alternancia de turnos</em>, ya que un bando puede jugar dos
 * veces seguidas si se salta el turno del rival. La mecánica es opcional: solo se activa si se agrega este
 * decorador y no requiere cambios en el núcleo.
 */
public final class ShotClockGame extends GameDecorator {

    private final int limit;
    private final List<ShotClockListener> listeners = new CopyOnWriteArrayList<>();

    public ShotClockGame(ChessGame delegate) {
        this(delegate, GameConfig.DEFAULT_SHOT_CLOCK_LIMIT);
    }

    public ShotClockGame(ChessGame delegate, int limit) {
        super(delegate);
        if (limit < 1) {
            throw new IllegalArgumentException("The shot clock limit must be at least 1");
        }
        this.limit = limit;
    }

    public int limit() {
        return limit;
    }

    public void addListener(ShotClockListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ShotClockListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void execute(TurnAction action) {
        ShotClockAction wrapped = new ShotClockAction(action, limit);
        delegate.execute(wrapped);
        for (ShotClockAction.Notification notification : wrapped.notifications()) {
            for (ShotClockListener listener : listeners) {
                if (notification.violation()) {
                    listener.onShotClockViolation(notification.team());
                } else {
                    listener.onShotClockWarning(notification.team(), notification.movesRemaining());
                }
            }
        }
    }

    /** Estado actual del reloj de {@code team}; si la mecánica está desactivada, devuelve el estado inicial. */
    public static ShotClockState stateOf(ChessGame game, Team team) {
        return game.extension(ShotClockAction.KEY, ShotClocks.class)
                .orElseGet(ShotClocks::fresh)
                .of(team);
    }
}
