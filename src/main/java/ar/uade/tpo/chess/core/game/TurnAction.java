package ar.uade.tpo.chess.core.game;

/**
 * Comando: algo que un jugador (o una regla) puede hacer en su turno. Una jugada normal es una implementación;
 * abandonar o acordar tablas son otras. Los adaptadores pueden agregar acciones propias sin tocar el núcleo.
 *
 * <p>Las acciones no necesitan saber cómo deshacerse: {@link Game} las ejecuta sobre una copia y guarda
 * {@link GameMemento}s del estado anterior y posterior. Así, cualquier acción se puede deshacer y rehacer.
 * Si una acción lanza {@link IllegalMoveException}, el estado no cambia.
 */
public interface TurnAction {

    /** Aplica esta acción al estado indicado (que es una copia de trabajo). */
    void execute(GameState state);

    /** Descripción breve para mostrar en el historial de la partida. */
    String describe();
}
