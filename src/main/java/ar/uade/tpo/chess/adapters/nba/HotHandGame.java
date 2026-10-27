package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.game.ChessGame;
import ar.uade.tpo.chess.core.game.GameDecorator;
import ar.uade.tpo.chess.core.game.TurnAction;
import ar.uade.tpo.chess.core.model.Team;

/**
 * Mechanic 3 - hot hand (pattern: Decorator).
 *
 * <p>When a team captures on {@value #STREAK_TO_IGNITE} consecutive moves of its own, it moves again before
 * the turn passes to the opponent.
 *
 * <p><b>Mandatory TP rule it breaks: turn alternation.</b> Standard chess alternates strictly; with this
 * mechanic a team can move twice in a row. Why it is worth it: it rewards sustained attack and gives the
 * NBA "hot hand" theme real weight in play. How it is switched off: do not add this decorator; nothing in
 * the core refers to it, so the game is then strictly alternating again. It is also one history entry per
 * move, so undo reverts the bonus move by moving back one step at a time.
 */
public final class HotHandGame extends GameDecorator {

    /** Consecutive own captures needed to earn the extra move. */
    public static final int STREAK_TO_IGNITE = 2;

    public HotHandGame(ChessGame delegate) {
        super(delegate);
    }

    @Override
    public void execute(TurnAction action) {
        delegate.execute(new HotHandAction(action));
    }

    /** The current capture streak of {@code team} in any game (0 when this mechanic is not enabled). */
    public static int streakOf(ChessGame game, Team team) {
        return game.extension(HotHandAction.KEY, Streaks.class).orElseGet(Streaks::fresh).of(team);
    }
}
