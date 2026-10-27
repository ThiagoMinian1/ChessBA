package ar.uade.tpo.chess.core.game;

/**
 * Memento: an opaque, frozen snapshot of a {@link GameState}. The state (Originator) creates it with
 * {@link GameState#save()}; the game's history (Caretaker) stores it without looking inside; restoring
 * always yields a fresh copy, so a snapshot can never be altered by later play.
 *
 * <p>Undoing by restoring snapshots, instead of reversing each kind of move by hand, is what makes
 * castling, en passant, promotion, captures and any custom action revert correctly with the same code.
 */
public final class GameMemento {

    private final GameState snapshot;

    GameMemento(GameState snapshot) {
        this.snapshot = snapshot;
    }

    GameState restore() {
        return snapshot.copy();
    }
}
