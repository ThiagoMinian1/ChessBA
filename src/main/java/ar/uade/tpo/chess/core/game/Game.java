package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Team;
import ar.uade.tpo.chess.core.rules.MaterialChecker;
import ar.uade.tpo.chess.core.rules.MoveGenerator;
import ar.uade.tpo.chess.ports.GameObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Standard chess, and nothing else. It owns the current {@link GameState}, runs every {@link TurnAction}
 * atomically, keeps the undo/redo history, decides after each action whether the game has ended, and tells
 * its observers what happened.
 *
 * <p>How an action runs: copy the state; run the action on the copy (if it throws, nothing changed);
 * record the new position; evaluate checkmate, stalemate and the draw conditions; then install the copy,
 * store a before/after {@link GameMemento} pair, and notify observers.
 */
public class Game implements ChessGame {

    /** The number of half-moves without a capture or pawn move after which the game is drawn. */
    public static final int FIFTY_MOVE_HALF_MOVES = 100;

    /** How many times the same position must occur for a draw by repetition. */
    public static final int REPETITION_LIMIT = 3;

    private record HistoryEntry(TurnAction action, GameMemento before, GameMemento after) {
    }

    private GameState state;
    private final List<HistoryEntry> history = new ArrayList<>();
    private int cursor;
    private final List<GameObserver> observers = new ArrayList<>();

    /** A new game from the standard starting position. */
    public Game() {
        this(GameState.initial());
    }

    /** A game starting from a custom state (the state is copied). Used for puzzles and tests. */
    public Game(GameState initial) {
        GameState start = initial.copy();
        start.clearPositionHistory();
        int occurrences = start.recordCurrentPosition();
        if (!start.status().isOver()) {
            evaluate(start, occurrences);
        }
        this.state = start;
    }

    // ---- Queries ---------------------------------------------------------------------------------------

    @Override
    public Team currentTurn() {
        return state.turn();
    }

    @Override
    public Board board() {
        return state.board().copy();
    }

    @Override
    public GameStatus status() {
        return state.status();
    }

    @Override
    public Optional<Team> winner() {
        return state.winner();
    }

    @Override
    public boolean isInCheck() {
        return MoveGenerator.isInCheck(state.board(), state.turn());
    }

    @Override
    public List<Move> legalMoves() {
        if (state.status().isOver()) {
            return List.of();
        }
        return MoveGenerator.legalMoves(state.board(), state.turn(), state.enPassantTarget().orElse(null));
    }

    @Override
    public List<Move> legalMovesFrom(Position from) {
        if (state.status().isOver()) {
            return List.of();
        }
        return MoveGenerator.legalMovesFrom(
                state.board(), state.turn(), state.enPassantTarget().orElse(null), from);
    }

    @Override
    public List<Piece> bench(Team team) {
        return List.copyOf(state.bench(team));
    }

    @Override
    public int halfmoveClock() {
        return state.halfmoveClock();
    }

    @Override
    public <T> Optional<T> extension(String key, Class<T> type) {
        return state.extension(key, type);
    }

    @Override
    public List<String> history() {
        return history.subList(0, cursor).stream().map(entry -> entry.action().describe()).toList();
    }

    // ---- Commands --------------------------------------------------------------------------------------

    @Override
    public void execute(TurnAction action) {
        if (state.status().isOver()) {
            throw new IllegalMoveException("The game is over: " + state.status());
        }
        GameState working = state.copy();
        working.clearLastMove();
        action.execute(working);
        settle(working);

        GameMemento before = state.save();
        GameMemento after = working.save();
        List<GameEvent> events = new ArrayList<>(working.drainEvents());
        if (working.status().isOver()) {
            events.add(new GameEvent.GameEnded(working.status(), working.winner()));
        }

        state = working;
        history.subList(cursor, history.size()).clear();
        history.add(new HistoryEntry(action, before, after));
        cursor++;

        dispatch(events);
    }

    @Override
    public boolean undo() {
        if (cursor == 0) {
            return false;
        }
        cursor--;
        state = history.get(cursor).before().restore();
        return true;
    }

    @Override
    public boolean redo() {
        if (cursor == history.size()) {
            return false;
        }
        state = history.get(cursor).after().restore();
        cursor++;
        return true;
    }

    @Override
    public boolean canUndo() {
        return cursor > 0;
    }

    @Override
    public boolean canRedo() {
        return cursor < history.size();
    }

    @Override
    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    // ---- Internals -------------------------------------------------------------------------------------

    /** After an action: record the position and decide whether the game just ended or a check was given. */
    private void settle(GameState working) {
        if (working.status().isOver()) {
            return; // the action itself ended the game (resignation, agreed draw)
        }
        int occurrences = working.recordCurrentPosition();
        evaluate(working, occurrences);
        if (!working.status().isOver() && MoveGenerator.isInCheck(working.board(), working.turn())) {
            working.emit(new GameEvent.CheckGiven(working.turn()));
        }
    }

    /**
     * Sets the final status if the side to move is checkmated or stalemated, or the position is drawn.
     * Checkmate takes precedence over every draw condition, as in standard rules.
     */
    private static void evaluate(GameState s, int occurrences) {
        Team toMove = s.turn();
        Position ep = s.enPassantTarget().orElse(null);
        if (!MoveGenerator.hasLegalMove(s.board(), toMove, ep)) {
            if (MoveGenerator.isInCheck(s.board(), toMove)) {
                s.finish(GameStatus.CHECKMATE, toMove.opposite());
            } else {
                s.finish(GameStatus.STALEMATE, null);
            }
        } else if (MaterialChecker.isInsufficientMaterial(s.board())) {
            s.finish(GameStatus.DRAW_INSUFFICIENT_MATERIAL, null);
        } else if (occurrences >= REPETITION_LIMIT) {
            s.finish(GameStatus.DRAW_THREEFOLD_REPETITION, null);
        } else if (s.halfmoveClock() >= FIFTY_MOVE_HALF_MOVES) {
            s.finish(GameStatus.DRAW_FIFTY_MOVE_RULE, null);
        }
    }

    private void dispatch(List<GameEvent> events) {
        for (GameEvent event : events) {
            for (GameObserver observer : List.copyOf(observers)) {
                switch (event) {
                    case GameEvent.MovePlayed e -> observer.onMovePlayed(e.team(), e.move(), e.piece());
                    case GameEvent.PieceCaptured e -> observer.onPieceCaptured(e.capturer(), e.captured(), e.at());
                    case GameEvent.PiecePromoted e -> observer.onPiecePromoted(e.team(), e.at(), e.promotedTo());
                    case GameEvent.CheckGiven e -> observer.onCheck(e.checkedTeam());
                    case GameEvent.GameEnded e -> observer.onGameEnded(e.status(), e.winner());
                    case GameEvent.Notice e -> observer.onNotice(e.message());
                }
            }
        }
    }
}
