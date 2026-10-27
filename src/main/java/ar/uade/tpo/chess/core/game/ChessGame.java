package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Team;
import ar.uade.tpo.chess.ports.GameObserver;

import java.util.List;
import java.util.Optional;

/**
 * What a game offers to the outside world. {@link Game} implements it for standard chess, and decorators
 * implement it by wrapping another {@code ChessGame}, which is how optional mechanics are layered on without
 * touching the core.
 *
 * <p>Everything a player can do funnels through {@link #execute(TurnAction)} (Command). The convenience
 * methods below are default methods built on it, so a decorator that intercepts {@code execute} intercepts
 * them all.
 */
public interface ChessGame {

    Team currentTurn();

    /** A copy of the board; changing it does not affect the game. */
    Board board();

    GameStatus status();

    Optional<Team> winner();

    /** Whether the side to move is in check. */
    boolean isInCheck();

    /** Legal moves for the side to move; empty once the game is over. */
    List<Move> legalMoves();

    List<Move> legalMovesFrom(Position from);

    /** Pieces of {@code team} that are off the board (captured, or traded out by a mechanic). */
    List<Piece> bench(Team team);

    /** Half-moves since the last capture or pawn move. */
    int halfmoveClock();

    /** Read access to state kept by decorators, which travels with snapshots. */
    <T> Optional<T> extension(String key, Class<T> type);

    /** Runs an action: validates it, applies it atomically, and records it for undo/redo. */
    void execute(TurnAction action);

    /** Reverts the last executed action. Returns false if there is nothing to undo. */
    boolean undo();

    /** Re-applies the last undone action. Returns false if there is nothing to redo. */
    boolean redo();

    boolean canUndo();

    boolean canRedo();

    /** Descriptions of the actions currently applied, oldest first. */
    List<String> history();

    void addObserver(GameObserver observer);

    void removeObserver(GameObserver observer);

    // ---- Conveniences built on execute() ---------------------------------------------------------------

    default void play(Move move) {
        execute(new MoveAction(move));
    }

    /** Plays the legal move from {@code from} to {@code to}; throws {@link PromotionRequiredException} for promotions. */
    default void play(Position from, Position to) {
        play(from, to, null);
    }

    /** Like {@link #play(Position, Position)} but chooses the promotion piece (null when the move is not a promotion). */
    default void play(Position from, Position to, PieceType promotion) {
        List<Move> candidates = legalMovesFrom(from).stream().filter(m -> m.to().equals(to)).toList();
        if (candidates.isEmpty()) {
            throw new IllegalMoveException("Illegal move: " + from + " to " + to);
        }
        if (promotion == null) {
            if (candidates.get(0).isPromotion()) {
                throw new PromotionRequiredException(
                        "Moving to " + to + " promotes the pawn: choose queen, rook, bishop or knight");
            }
            play(candidates.get(0));
            return;
        }
        Move chosen = candidates.stream()
                .filter(m -> m.promotion() == promotion)
                .findFirst()
                .orElseThrow(() -> new IllegalMoveException(
                        "Illegal move: " + from + " to " + to + " cannot promote to " + promotion));
        play(chosen);
    }

    /** True if moving {@code from} to {@code to} is a promotion, so the caller must choose a piece. */
    default boolean requiresPromotion(Position from, Position to) {
        return legalMovesFrom(from).stream().anyMatch(m -> m.to().equals(to) && m.isPromotion());
    }

    default void resign(Team team) {
        execute(new ResignAction(team));
    }

    default void agreeDraw() {
        execute(new AgreeDrawAction());
    }
}
