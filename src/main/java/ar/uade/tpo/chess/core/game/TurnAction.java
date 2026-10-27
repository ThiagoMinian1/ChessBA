package ar.uade.tpo.chess.core.game;

/**
 * Command: one thing a player (or a rule) can do on their turn. A normal move is one implementation;
 * resigning or agreeing a draw are others; themed adapters add their own without touching the core.
 *
 * <p>An action never has to know how to undo itself: the {@link Game} runs it on a copy of the state and keeps
 * {@link GameMemento}s from before and after, so any action is automatically undoable and redoable.
 * An action may throw {@link IllegalMoveException}; nothing is then changed.
 */
public interface TurnAction {

    /** Applies this action to the given (working copy of the) state. */
    void execute(GameState state);

    /** Short human-readable description, used in the game history. */
    String describe();
}
