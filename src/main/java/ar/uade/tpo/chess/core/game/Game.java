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
 * Ajedrez estándar, sin reglas adicionales. Mantiene el {@link GameState}, ejecuta cada {@link TurnAction}
 * de forma atómica, guarda el historial para deshacer y rehacer, determina si la partida terminó y avisa
 * a sus observadores de lo ocurrido.
 *
 * <p>Para ejecutar una acción, primero se copia el estado y se aplica sobre esa copia. Si falla, no cambia nada.
 * Luego se registra la posición, se comprueban el jaque mate, el ahogado y las condiciones de tablas, y se
 * guarda el estado junto con su copia anterior antes de avisar a los observadores.
 */
public class Game implements ChessGame {

    /** Medias jugadas sin capturas ni movimientos de peón necesarias para declarar tablas. */
    public static final int FIFTY_MOVE_HALF_MOVES = 100;

    /** Veces que debe repetirse una posición para declarar tablas por repetición. */
    public static final int REPETITION_LIMIT = 3;

    private record HistoryEntry(TurnAction action, GameMemento before, GameMemento after) {
    }

    private GameState state;
    private final List<HistoryEntry> history = new ArrayList<>();
    private int cursor;
    private final List<GameObserver> observers = new ArrayList<>();

    /** Crea una partida desde la posición inicial estándar. */
    public Game() {
        this(GameState.initial());
    }

    /** Crea una partida desde un estado personalizado (se copia el estado). Se usa en problemas y pruebas. */
    public Game(GameState initial) {
        GameState start = initial.copy();
        start.clearPositionHistory();
        int occurrences = start.recordCurrentPosition();
        if (!start.status().isOver()) {
            evaluate(start, occurrences);
        }
        this.state = start;
    }

    // ---- Consultas -------------------------------------------------------------------------------------

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

    // ---- Acciones --------------------------------------------------------------------------------------

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

    // ---- Métodos internos ------------------------------------------------------------------------------

    /** Después de una acción, registra la posición y comprueba si terminó la partida o se dio jaque. */
    private void settle(GameState working) {
        if (working.status().isOver()) {
            return; // La acción terminó la partida (abandono o tablas acordadas).
        }
        int occurrences = working.recordCurrentPosition();
        evaluate(working, occurrences);
        if (!working.status().isOver() && MoveGenerator.isInCheck(working.board(), working.turn())) {
            working.emit(new GameEvent.CheckGiven(working.turn()));
        }
    }

    /**
     * Define el resultado final si el bando al que le toca está en jaque mate o ahogado, o si la posición es tablas.
     * Como en las reglas estándar, el jaque mate tiene prioridad sobre cualquier condición de tablas.
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
