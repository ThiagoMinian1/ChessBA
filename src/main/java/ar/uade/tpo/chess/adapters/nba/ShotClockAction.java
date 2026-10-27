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
 * Decorator around a {@link TurnAction}: runs the wrapped action, then updates the acting team's shot clock
 * and, if the other team's clock has expired, skips that team's turn.
 *
 * <p>Because the clock update and the skip happen inside the same action, they are one entry in the history:
 * undoing the move also undoes the skip it caused.
 *
 * <p>A turn is only skipped when it would leave the game in a sensible position: the team to be skipped is
 * not in check (skipping would leave its king unable to answer), and neither side is checkmated or stalemated.
 */
final class ShotClockAction implements TurnAction {

    static final String KEY = "nba.shotclock";

    /** Something worth telling a {@link ShotClockListener} once the action is committed. */
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
