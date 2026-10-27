package ar.uade.tpo.chess.adapters.nba;

/**
 * State pattern for one team's shot clock. A clock is always in exactly one state, and each state decides
 * what the next state is after the team makes a move:
 * <ul>
 *   <li>{@link Running}: plenty of moves left;</li>
 *   <li>{@link Warning}: only a few moves left before the clock expires;</li>
 *   <li>{@link Expired}: the limit has been reached; the team's next turn will be skipped.</li>
 * </ul>
 * "Progress" means capturing a piece or moving a pawn; it sends any state back to a fresh {@link Running} clock.
 * States are immutable values, so they can live in game snapshots and be undone for free.
 */
public sealed interface ShotClockState {

    /** Moves left at or below which a clock reports a warning. */
    int WARNING_WINDOW = 3;

    /** Own moves made since the team last made progress. */
    int movesWithoutProgress();

    /** The state after the team makes one of its own moves. */
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
     * The limit was reached. It stays expired until the team makes progress or its turn is skipped
     * (a skip can be postponed while the team is in check).
     */
    record Expired(int movesWithoutProgress) implements ShotClockState {
        @Override
        public ShotClockState afterOwnMove(boolean progress, int limit) {
            return progress ? fresh() : new Expired(movesWithoutProgress + 1);
        }
    }
}
