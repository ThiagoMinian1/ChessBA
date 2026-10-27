package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.Team;

import java.util.Objects;

/** A player gives up; the other side wins. Being an action, it can be undone like any other. */
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
