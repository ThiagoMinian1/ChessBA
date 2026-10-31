package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.game.GameEvent;
import ar.uade.tpo.chess.core.game.GameState;
import ar.uade.tpo.chess.core.game.TurnAction;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.Team;

import java.util.List;

/**
 * Decorador de {@link TurnAction}: ejecuta la acción y, si hubo una captura, suma al marcador del bando que
 * capturó el valor habitual de la pieza (peón 1, caballo 3, alfil 3, torre 5 y dama 9).
 *
 * <p>La pieza capturada se consulta en el banco del rival, donde la guarda el núcleo. Si se capturó una pieza
 * promocionada, cuenta con su tipo actual. Los intercambios y abandonos no suman puntos.
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
