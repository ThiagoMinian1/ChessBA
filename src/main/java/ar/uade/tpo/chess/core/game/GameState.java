package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.MoveType;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Team;
import ar.uade.tpo.chess.core.rules.MoveGenerator;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Everything that defines where a game stands. This is the Originator of the Memento pattern: it can
 * {@link #save()} itself and be rebuilt from a {@link GameMemento}.
 *
 * <p>The state is mutable <em>on purpose</em>, but only inside a {@link TurnAction}: the {@link Game} always
 * runs an action on a private copy and installs that copy only if the action succeeds. Code outside an action
 * never receives a live state.
 *
 * <p>The "bench" of a team holds that team's pieces that are off the board (captured ones). Extensions are an
 * opaque map of <em>immutable</em> values where decorators keep their own state (a clock, a streak); because
 * the map is part of every snapshot, that state is undone and redone together with the board.
 */
public final class GameState {

    private final Board board;
    private Team turn;
    private Position enPassantTarget;
    private int halfmoveClock;
    private final Map<String, Integer> positionCounts = new HashMap<>();
    private final Map<Team, List<Piece>> bench = new EnumMap<>(Team.class);
    private GameStatus status = GameStatus.ONGOING;
    private Team winner;
    private final Map<String, Object> extensions = new HashMap<>();
    private MoveRecord lastMove;
    private final List<GameEvent> pendingEvents = new ArrayList<>();

    private GameState(Board board, Team turn) {
        this.board = Objects.requireNonNull(board, "board");
        this.turn = Objects.requireNonNull(turn, "turn");
        for (Team team : Team.values()) {
            bench.put(team, new ArrayList<>());
        }
    }

    /** The standard starting position with white to move. */
    public static GameState initial() {
        return new GameState(Board.standard(), Team.WHITE);
    }

    /** A state for a custom position (the board is copied). Position history starts when a {@link Game} adopts it. */
    public static GameState of(Board board, Team turn) {
        return new GameState(board.copy(), turn);
    }

    // ---- Memento ---------------------------------------------------------------------------------------

    public GameMemento save() {
        return new GameMemento(copy());
    }

    /** A deep copy: board, histories and extensions are independent of the original. Pending events are not copied. */
    public GameState copy() {
        GameState copy = new GameState(board.copy(), turn);
        copy.enPassantTarget = enPassantTarget;
        copy.halfmoveClock = halfmoveClock;
        copy.positionCounts.putAll(positionCounts);
        for (Team team : Team.values()) {
            copy.bench.get(team).addAll(bench.get(team));
        }
        copy.status = status;
        copy.winner = winner;
        copy.extensions.putAll(extensions);
        copy.lastMove = lastMove;
        return copy;
    }

    // ---- Board and turn --------------------------------------------------------------------------------

    /** The live board of this state; inside an action it is the working copy, so mutating it is expected. */
    public Board board() {
        return board;
    }

    public Team turn() {
        return turn;
    }

    public void setTurn(Team turn) {
        this.turn = Objects.requireNonNull(turn, "turn");
    }

    /** The square a pawn just skipped over with a double push; capturable en passant for exactly one move. */
    public Optional<Position> enPassantTarget() {
        return Optional.ofNullable(enPassantTarget);
    }

    public void setEnPassantTarget(Position target) {
        this.enPassantTarget = target;
    }

    /** Half-moves since the last capture or pawn move; 100 of them draw the game (fifty-move rule). */
    public int halfmoveClock() {
        return halfmoveClock;
    }

    public void setHalfmoveClock(int halfmoveClock) {
        if (halfmoveClock < 0) {
            throw new IllegalArgumentException("halfmoveClock must not be negative");
        }
        this.halfmoveClock = halfmoveClock;
    }

    /** The live list of {@code team}'s pieces that are off the board. */
    public List<Piece> bench(Team team) {
        return bench.get(team);
    }

    // ---- Result ----------------------------------------------------------------------------------------

    public GameStatus status() {
        return status;
    }

    public Optional<Team> winner() {
        return Optional.ofNullable(winner);
    }

    /** Ends the game with the given result; {@code winner} is null for draws. */
    public void finish(GameStatus status, Team winner) {
        if (!status.isOver()) {
            throw new IllegalArgumentException("finish() needs a final status, not " + status);
        }
        this.status = status;
        this.winner = winner;
    }

    // ---- Extensions and last move ----------------------------------------------------------------------

    public <T> Optional<T> extension(String key, Class<T> type) {
        Object value = extensions.get(key);
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
    }

    /** Stores an <em>immutable</em> value under a key; it will travel with every snapshot. */
    public void putExtension(String key, Object value) {
        extensions.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
    }

    public Optional<MoveRecord> lastMove() {
        return Optional.ofNullable(lastMove);
    }

    public void setLastMove(MoveRecord lastMove) {
        this.lastMove = lastMove;
    }

    void clearLastMove() {
        this.lastMove = null;
    }

    // ---- Events ----------------------------------------------------------------------------------------

    public void emit(GameEvent event) {
        pendingEvents.add(Objects.requireNonNull(event, "event"));
    }

    List<GameEvent> drainEvents() {
        List<GameEvent> events = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return events;
    }

    // ---- Position history (threefold repetition) -------------------------------------------------------

    /** Forgets all earlier positions. Used when an action makes earlier positions unreachable. */
    public void clearPositionHistory() {
        positionCounts.clear();
    }

    /** Records the current position and returns how many times it has now occurred. */
    public int recordCurrentPosition() {
        return positionCounts.merge(positionKey(), 1, Integer::sum);
    }

    /**
     * Identity of the current position for repetition purposes: piece placement, side to move, castling
     * rights, and the en passant square only when an en passant capture is actually possible.
     */
    public String positionKey() {
        return board.toString().replace('\n', '/')
                + ' ' + (turn == Team.WHITE ? 'w' : 'b')
                + ' ' + castlingRights()
                + ' ' + enPassantKey();
    }

    private String castlingRights() {
        StringBuilder rights = new StringBuilder();
        if (castlingIntact(Team.WHITE, 7)) {
            rights.append('K');
        }
        if (castlingIntact(Team.WHITE, 0)) {
            rights.append('Q');
        }
        if (castlingIntact(Team.BLACK, 7)) {
            rights.append('k');
        }
        if (castlingIntact(Team.BLACK, 0)) {
            rights.append('q');
        }
        return rights.length() == 0 ? "-" : rights.toString();
    }

    private boolean castlingIntact(Team team, int rookFile) {
        int rank = team.homeRank();
        boolean kingReady = board.pieceAt(new Position(4, rank))
                .filter(p -> p.type() == PieceType.KING && p.team() == team && !p.hasMoved())
                .isPresent();
        boolean rookReady = board.pieceAt(new Position(rookFile, rank))
                .filter(p -> p.type() == PieceType.ROOK && p.team() == team && !p.hasMoved())
                .isPresent();
        return kingReady && rookReady;
    }

    private String enPassantKey() {
        if (enPassantTarget == null) {
            return "-";
        }
        boolean capturePossible = MoveGenerator.legalMoves(board, turn, enPassantTarget).stream()
                .anyMatch(move -> move.type() == MoveType.EN_PASSANT);
        return capturePossible ? enPassantTarget.toString() : "-";
    }
}
