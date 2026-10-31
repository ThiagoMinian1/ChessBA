package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.game.GameEvent;
import ar.uade.tpo.chess.core.game.GameState;
import ar.uade.tpo.chess.core.game.TurnAction;
import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Team;
import ar.uade.tpo.chess.core.rules.MoveGenerator;

/**
 * Decorador de {@link TurnAction}: ejecuta la acción y actualiza la racha de capturas del bando que jugó.
 * Cuando llega a {@link HotHandGame#STREAK_TO_IGNITE}, ese bando conserva el turno y vuelve a jugar.
 *
 * <p>La racha cuenta las capturas consecutivas del mismo bando: cualquier acción propia que no capture (incluido
 * un intercambio) la reinicia; las jugadas del rival no la afectan. Al llegar al objetivo, la racha se reinicia.
 * La jugada extra solo se concede si el rival no está en jaque, ambos bandos tienen jugadas legales y otra
 * mecánica no mantuvo ya el turno (por ejemplo, al saltarse un turno por el reloj).
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
