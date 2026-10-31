package ar.uade.tpo.chess.adapters.nba;

/**
 * Patrón State para el reloj de posesión de un bando. El reloj siempre está en un único estado y cada estado
 * determina cuál sigue después de una jugada:
 * <ul>
 *   <li>{@link Running}: quedan varias jugadas;</li>
 *   <li>{@link Warning}: quedan pocas jugadas antes de que venza el reloj;</li>
 *   <li>{@link Expired}: se alcanzó el límite y se saltará el siguiente turno del bando.</li>
 * </ul>
 * Se considera que hay progreso cuando se captura una pieza o se mueve un peón; esto reinicia el reloj con un
 * {@link Running} nuevo. Los estados son inmutables, así que se guardan en las copias de la partida y se
 * restauran al deshacer una jugada.
 */
public sealed interface ShotClockState {

    /** Jugadas restantes a partir de las que se avisa que el reloj está por vencer. */
    int WARNING_WINDOW = 3;

    /** Jugadas propias desde el último progreso del bando. */
    int movesWithoutProgress();

    /** Estado que queda después de una jugada propia. */
    ShotClockState afterOwnMove(boolean progress, int limit);

    static ShotClockState fresh() {
        return new Running(0);
    }

    default boolean isExpired() {
        return this instanceof Expired;
    }

    default boolean isWarning() {
        return this instanceof Warning;
    }

    private static ShotClockState classify(int moves, int limit) {
        if (moves >= limit) {
            return new Expired(moves);
        }
        if (limit - moves <= WARNING_WINDOW) {
            return new Warning(moves);
        }
        return new Running(moves);
    }

    record Running(int movesWithoutProgress) implements ShotClockState {
        @Override
        public ShotClockState afterOwnMove(boolean progress, int limit) {
            return progress ? fresh() : classify(movesWithoutProgress + 1, limit);
        }
    }

    record Warning(int movesWithoutProgress) implements ShotClockState {
        @Override
        public ShotClockState afterOwnMove(boolean progress, int limit) {
            return progress ? fresh() : classify(movesWithoutProgress + 1, limit);
        }
    }

    /**
     * Se alcanzó el límite. El reloj sigue vencido hasta que el bando progrese o se salte su turno
     * (el salto puede posponerse si está en jaque).
     */
    record Expired(int movesWithoutProgress) implements ShotClockState {
        @Override
        public ShotClockState afterOwnMove(boolean progress, int limit) {
            return progress ? fresh() : new Expired(movesWithoutProgress + 1);
        }
    }
}
