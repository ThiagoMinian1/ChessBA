package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.GameStatus;

/** Both players agree to a draw. Asking and accepting is the adapter's job; the core only records the result. */
public final class AgreeDrawAction implements TurnAction {

    @Override
    public void execute(GameState state) {
        state.finish(GameStatus.DRAW_BY_AGREEMENT, null);
    }

    @Override
    public String describe() {
        return "Draw by agreement";
    }
}
