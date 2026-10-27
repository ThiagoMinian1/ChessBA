package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.game.ChessGame;
import ar.uade.tpo.chess.core.game.GameDecorator;
import ar.uade.tpo.chess.core.game.TurnAction;
import ar.uade.tpo.chess.core.model.Team;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Mechanic 1 - shot clock (patterns: Decorator + State + Observer).
 *
 * <p>Each team must make progress (capture a piece or advance a pawn) within {@code limit} of its own moves.
 * If it does not, its next turn is skipped and its clock resets, so the opponent moves twice in a row.
 *
 * <ul>
 *   <li><b>Decorator:</b> wraps any {@link ChessGame} and never touches the core's move validation.</li>
 *   <li><b>State:</b> each team's clock is a {@link ShotClockState}.</li>
 *   <li><b>Observer:</b> {@link ShotClockListener}s hear about warnings and violations.</li>
 * </ul>
 *
 * <p>Mandatory TP rule touched: <em>turn alternation</em>, because a skip lets one side move twice in a row.
 * It is reversible: the mechanic exists only when this decorator is in the chain, and the core is unchanged.
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

    /** The current clock state of {@code team} in any game, even when this mechanic is not enabled (then: fresh). */
    public static ShotClockState stateOf(ChessGame game, Team team) {
        return game.extension(ShotClockAction.KEY, ShotClocks.class)
                .orElseGet(ShotClocks::fresh)
                .of(team);
    }
}
