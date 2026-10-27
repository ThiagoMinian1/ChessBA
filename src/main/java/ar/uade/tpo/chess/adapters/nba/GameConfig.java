package ar.uade.tpo.chess.adapters.nba;

/**
 * Which optional NBA mechanics are switched on. With everything off ({@link #standard()}) the game is pure
 * standard chess. Immutable; build with {@link #builder()}.
 *
 * <p>Each flag maps to one mechanic that lives entirely in this adapter package:
 * the shot clock ({@link ShotClockGame}), the trade ({@link TradeAction}) and the hot hand ({@link HotHandGame}).
 */
public record GameConfig(boolean shotClockEnabled, int shotClockLimit, boolean tradeEnabled, boolean hotHandEnabled) {

    /** Own moves without capturing or advancing a pawn before a team loses its turn. */
    public static final int DEFAULT_SHOT_CLOCK_LIMIT = 10;

    public GameConfig {
        if (shotClockLimit < 1) {
            throw new IllegalArgumentException("The shot clock limit must be at least 1");
        }
    }

    /** No mechanics: standard chess. */
    public static GameConfig standard() {
        return new GameConfig(false, DEFAULT_SHOT_CLOCK_LIMIT, false, false);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isStandardChess() {
        return !shotClockEnabled && !tradeEnabled && !hotHandEnabled;
    }

    public static final class Builder {

        private boolean shotClockEnabled;
        private int shotClockLimit = DEFAULT_SHOT_CLOCK_LIMIT;
        private boolean tradeEnabled;
        private boolean hotHandEnabled;

        private Builder() {
        }

        public Builder shotClock() {
            return shotClock(DEFAULT_SHOT_CLOCK_LIMIT);
        }

        public Builder shotClock(int limit) {
            this.shotClockEnabled = true;
            this.shotClockLimit = limit;
            return this;
        }

        public Builder trade() {
            this.tradeEnabled = true;
            return this;
        }

        public Builder hotHand() {
            this.hotHandEnabled = true;
            return this;
        }

        public GameConfig build() {
            return new GameConfig(shotClockEnabled, shotClockLimit, tradeEnabled, hotHandEnabled);
        }
    }
}
