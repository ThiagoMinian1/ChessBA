package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.game.GameEvent;
import ar.uade.tpo.chess.core.game.GameState;
import ar.uade.tpo.chess.core.game.TurnAction;
import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Team;
import ar.uade.tpo.chess.core.rules.MoveGenerator;

/**
 * Decorator around a {@link TurnAction}: runs the wrapped action, then updates the acting team's capture
 * streak and, when the streak reaches {@link HotHandGame#STREAK_TO_IGNITE}, keeps the turn with the same team
 * so it moves again.
 *
 * <p>The streak counts the team's own consecutive captures: any own action that is not a capture
 * (including a trade) resets it, and the opponent's moves do not. Reaching the target always resets the streak;
 * the bonus move is granted only if it makes sense: the opponent is not in check (it would have to answer the
 * check immediately), the opponent still has a legal move, the team itself has a legal move, and the turn was
 * not already kept by another mechanic (a shot-clock skip).
 */
final class HotHandAction implements TurnAction {

    static final String KEY = "nba.hothand";

    private final TurnAction inner;
    private boolean granted;

    HotHandAction(TurnAction inner) {
        this.inner = inner;
    }

    @Override
    public void execute(GameState state) {
        granted = false;
        Team actor = state.turn();
        inner.execute(state);

        boolean capture = state.lastMove().map(m -> m.capture()).orElse(false);
        Streaks streaks = state.extension(KEY, Streaks.class).orElseGet(Streaks::fresh);
        int streak = capture ? streaks.of(actor) + 1 : 0;

        if (streak >= HotHandGame.STREAK_TO_IGNITE) {
            streak = 0;
            Team opponent = state.turn();
            if (opponent != actor && canGrant(state, actor, opponent)) {
                state.setTurn(actor);
                state.setEnPassantTarget(null);
                granted = true;
                state.emit(new GameEvent.Notice("Hot hand! " + actor + " moves again"));
            }
        }
        state.putExtension(KEY, streaks.with(actor, streak));
    }

    private boolean canGrant(GameState state, Team actor, Team opponent) {
        Board board = state.board();
        return !MoveGenerator.isInCheck(board, opponent)
                && MoveGenerator.hasLegalMove(board, opponent, state.enPassantTarget().orElse(null))
                && MoveGenerator.hasLegalMove(board, actor, null);
    }

    @Override
    public String describe() {
        return granted ? inner.describe() + " (hot hand: extra move)" : inner.describe();
    }
}
