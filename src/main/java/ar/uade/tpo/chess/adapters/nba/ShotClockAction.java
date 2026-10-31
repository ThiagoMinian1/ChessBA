package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.game.GameEvent;
import ar.uade.tpo.chess.core.game.GameState;
import ar.uade.tpo.chess.core.game.TurnAction;
import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Team;
import ar.uade.tpo.chess.core.rules.MoveGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Decorador de {@link TurnAction}: ejecuta la acción, actualiza el reloj del bando que jugó y, si el del rival
 * venció, le salta el turno.
 *
 * <p>El reloj y el salto de turno forman parte de la misma acción y ocupan una sola entrada en el historial.
 * Al deshacer la jugada, también se revierte el salto de turno.
 *
 * <p>El turno solo se salta si la posición sigue siendo válida: el bando afectado no está en jaque (de lo
 * contrario no podría responder) y ninguno de los bandos está en jaque mate o ahogado.
 */
final class ShotClockAction implements TurnAction {

    static final String KEY = "nba.shotclock";

    /** Aviso que se envía a {@link ShotClockListener} una vez confirmada la acción. */
    record Notification(Team team, boolean violation, int movesRemaining) {
    }

    private final TurnAction inner;
    private final int limit;
    private final List<Notification> notifications = new ArrayList<>();

    ShotClockAction(TurnAction inner, int limit) {
        this.inner = inner;
        this.limit = limit;
    }

    List<Notification> notifications() {
        return List.copyOf(notifications);
    }

    @Override
    public void execute(GameState state) {
        notifications.clear();
        Team actor = state.turn();
        inner.execute(state);

        boolean progress = state.lastMove().map(m -> m.capture() || m.isPawnMove()).orElse(false);
        ShotClocks clocks = state.extension(KEY, ShotClocks.class).orElseGet(ShotClocks::fresh);
        ShotClockState updated = clocks.of(actor).afterOwnMove(progress, limit);
        clocks = clocks.with(actor, updated);
        if (updated instanceof ShotClockState.Warning warning) {
            notifications.add(new Notification(actor, false, limit - warning.movesWithoutProgress()));
        }

        Team next = state.turn();
        if (next != actor && clocks.of(next).isExpired() && canSkip(state, actor, next)) {
            state.setTurn(actor);
            state.setEnPassantTarget(null);
            clocks = clocks.with(next, ShotClockState.fresh());
            notifications.add(new Notification(next, true, 0));
            state.emit(new GameEvent.Notice("Shot clock violation: " + next + " loses its turn"));
        }
        state.putExtension(KEY, clocks);
    }

    private boolean canSkip(GameState state, Team actor, Team next) {
        Board board = state.board();
        return !MoveGenerator.isInCheck(board, next)
                && MoveGenerator.hasLegalMove(board, next, state.enPassantTarget().orElse(null))
                && MoveGenerator.hasLegalMove(board, actor, null);
    }

    @Override
    public String describe() {
        boolean skipped = notifications.stream().anyMatch(Notification::violation);
        return skipped ? inner.describe() + " (shot clock: opponent's turn skipped)" : inner.describe();
    }
}
