package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.game.GameEvent;
import ar.uade.tpo.chess.core.game.GameState;
import ar.uade.tpo.chess.core.game.TurnAction;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.Team;

import java.util.List;

/**
 * Decorator around a {@link TurnAction}: runs the wrapped action and, if it captured a piece, adds that piece's
 * conventional value (pawn 1, knight 3, bishop 3, rook 5, queen 9) to the capturing team's score.
 *
 * <p>The captured piece is read from the opponent's bench, where the core puts it (a captured promoted piece is
 * on the bench as its promoted type, so it scores as that piece). Trades and resignations never score.
 */
final class ScoreboardAction implements TurnAction {

    static final String KEY = "nba.scoreboard";

    private final TurnAction inner;

    ScoreboardAction(TurnAction inner) {
        this.inner = inner;
    }

    @Override
    public void execute(GameState state) {
        Team actor = state.turn();
        int benchBefore = state.bench(actor.opposite()).size();
        inner.execute(state);

        List<Piece> bench = state.bench(actor.opposite());
        boolean captured = state.lastMove().map(m -> m.capture()).orElse(false) && bench.size() == benchBefore + 1;
        if (captured) {
            int points = bench.get(bench.size() - 1).materialValue();
            Scores scores = state.extension(KEY, Scores.class).orElseGet(Scores::fresh).plus(actor, points);
            state.putExtension(KEY, scores);
            state.emit(new GameEvent.Notice("Scoreboard: " + actor + " scores " + points
                    + (points == 1 ? " point" : " points") + " (" + scores.white() + "-" + scores.black() + ")"));
        }
    }

    @Override
    public String describe() {
        return inner.describe();
    }
}
