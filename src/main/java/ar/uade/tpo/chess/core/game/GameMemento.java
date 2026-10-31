package ar.uade.tpo.chess.core.game;

/**
 * Memento: una copia inmutable y opaca de {@link GameState}. El estado (Originator) la crea con
 * {@link GameState#save()} y el historial de la partida (Caretaker) la guarda sin acceder a su contenido.
 * Al restaurarla siempre se obtiene una copia nueva, que no puede cambiar durante el juego.
 *
 * <p>Restaurar una copia en vez de revertir cada tipo de movimiento por separado permite deshacer
 * correctamente enroques, capturas al paso, promociones, capturas y cualquier acción personalizada.
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
