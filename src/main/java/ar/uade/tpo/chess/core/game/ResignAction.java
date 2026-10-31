package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.Team;

import java.util.Objects;

/** Un jugador abandona y gana el otro bando. Como cualquier acción, se puede deshacer. */
public final class ResignAction implements TurnAction {

    private final Team resigning;

    public ResignAction(Team resigning) {
        this.resigning = Objects.requireNonNull(resigning, "resigning");
    }

    @Override
    public void execute(GameState state) {
        state.finish(GameStatus.RESIGNATION, resigning.opposite());
    }

    @Override
    public String describe() {
        return resigning + " resigns";
    }
}
