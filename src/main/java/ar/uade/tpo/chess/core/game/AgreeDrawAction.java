package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.GameStatus;

/** Ambos jugadores acuerdan tablas. El adaptador se ocupa de pedir y aceptar; el núcleo solo registra el resultado. */
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
