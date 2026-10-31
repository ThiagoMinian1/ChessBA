package ar.uade.tpo.chess.adapters.nba;

/**
 * Indica qué mecánicas opcionales de la NBA están activas. Si todas están desactivadas ({@link #standard()}),
 * la partida sigue las reglas estándar del ajedrez. Es inmutable y se crea con {@link #builder()}.
 *
 * <p>Cada opción corresponde a una mecánica de este paquete: el reloj de posesión ({@link ShotClockGame}),
 * el intercambio ({@link TradeAction}), la mano caliente ({@link HotHandGame}) y el marcador ({@link ScoreboardGame}).
 */
public record GameConfig(boolean shotClockEnabled, int shotClockLimit, boolean tradeEnabled, boolean hotHandEnabled,
                         boolean scoreboardEnabled) {

    /** Jugadas propias sin capturas ni avances de peón antes de perder el turno. */
    public static final int DEFAULT_SHOT_CLOCK_LIMIT = 10;

    public GameConfig {
        if (shotClockLimit < 1) {
            throw new IllegalArgumentException("The shot clock limit must be at least 1");
        }
    }

    /** Sin mecánicas adicionales: ajedrez estándar. */
    public static GameConfig standard() {
        return new GameConfig(false, DEFAULT_SHOT_CLOCK_LIMIT, false, false, false);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isStandardChess() {
        return !shotClockEnabled && !tradeEnabled && !hotHandEnabled && !scoreboardEnabled;
    }

    public static final class Builder {

        private boolean shotClockEnabled;
        private int shotClockLimit = DEFAULT_SHOT_CLOCK_LIMIT;
        private boolean tradeEnabled;
        private boolean hotHandEnabled;
        private boolean scoreboardEnabled;

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

        public Builder scoreboard() {
            this.scoreboardEnabled = true;
            return this;
        }

        public GameConfig build() {
            return new GameConfig(shotClockEnabled, shotClockLimit, tradeEnabled, hotHandEnabled, scoreboardEnabled);
        }
    }
}
