package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.game.ChessGame;
import ar.uade.tpo.chess.core.game.GameDecorator;
import ar.uade.tpo.chess.core.game.TurnAction;
import ar.uade.tpo.chess.core.model.Team;

/**
 * Mecánica 3: mano caliente (patrón Decorator).
 *
 * <p>Cuando un bando captura en {@value #STREAK_TO_IGNITE} jugadas propias consecutivas, vuelve a jugar antes
 * de ceder el turno al rival.
 *
 * <p><b>Regla obligatoria del TP que modifica: alternancia de turnos.</b> En el ajedrez estándar los bandos
 * alternan; con esta mecánica uno puede jugar dos veces seguidas. Premia los ataques sostenidos y le da peso
 * al tema de la «mano caliente» de la NBA. Para desactivarla, basta con no agregar este decorador: el núcleo
 * no depende de él y los turnos vuelven a alternarse. Cada jugada queda en el historial, así que se puede
 * deshacer paso a paso.
 */
public final class HotHandGame extends GameDecorator {

    /** Capturas propias consecutivas necesarias para obtener una jugada extra. */
    public static final int STREAK_TO_IGNITE = 2;

    public HotHandGame(ChessGame delegate) {
        super(delegate);
    }

    @Override
    public void execute(TurnAction action) {
        delegate.execute(new HotHandAction(action));
    }

    /** Racha actual de capturas de {@code team} (0 si esta mecánica está desactivada). */
    public static int streakOf(ChessGame game, Team team) {
        return game.extension(HotHandAction.KEY, Streaks.class).orElseGet(Streaks::fresh).of(team);
    }
}
