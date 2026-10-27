package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.game.ChessGame;
import ar.uade.tpo.chess.core.game.GameDecorator;
import ar.uade.tpo.chess.core.game.TurnAction;
import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.Team;

import java.util.Optional;

/**
 * Mechanic 4 - scoreboard (pattern: Decorator).
 *
 * <p>Every capture scores the captured piece's value for the capturer (pawn 1, knight 3, bishop 3, rook 5,
 * queen 9). If the game ends in a draw, the team with more points wins on the scoreboard.
 *
 * <p><b>Mandatory TP rule it changes: the result of a drawn game.</b> Standard chess has draws; with this
 * mechanic a draw by stalemate, threefold repetition, the fifty-move rule or insufficient material is decided
 * by points, and is still a draw only if the scores are level. A draw <i>by agreement</i> is never decided on
 * points: both players chose it. {@link #status()} still reports the real chess status (for example
 * {@code DRAW_FIFTY_MOVE_RULE}); only {@link #winner()} reflects the scoreboard, and
 * {@link #decidedOnPoints(ChessGame)} tells a front end which of the two happened.
 *
 * <p>How it is switched off: do not add this decorator. The scores live in the game state's extensions, so
 * undo and redo keep them in step with the board.
 */
public final class ScoreboardGame extends GameDecorator {

    public ScoreboardGame(ChessGame delegate) {
        super(delegate);
    }

    @Override
    public void execute(TurnAction action) {
        delegate.execute(new ScoreboardAction(action));
    }

    @Override
    public Optional<Team> winner() {
        Optional<Team> chessWinner = delegate.winner();
        return chessWinner.isPresent() ? chessWinner : pointsWinner(delegate);
    }

    /** The points of {@code team} in any game (0 when this mechanic is not enabled). */
    public static int scoreOf(ChessGame game, Team team) {
        return game.extension(ScoreboardAction.KEY, Scores.class).orElseGet(Scores::fresh).of(team);
    }

    /** True when the game is a drawn one (not by agreement) that the scoreboard turns into a win. */
    public static boolean decidedOnPoints(ChessGame game) {
        return pointsWinner(game).isPresent();
    }

    private static Optional<Team> pointsWinner(ChessGame game) {
        if (!isPointsDraw(game.status())) {
            return Optional.empty();
        }
        int white = scoreOf(game, Team.WHITE);
        int black = scoreOf(game, Team.BLACK);
        if (white == black) {
            return Optional.empty();
        }
        return Optional.of(white > black ? Team.WHITE : Team.BLACK);
    }

    private static boolean isPointsDraw(GameStatus status) {
        return status.isDraw() && status != GameStatus.DRAW_BY_AGREEMENT;
    }
}
