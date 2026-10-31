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
 * Contiene todo lo que define el estado de una partida. Es el Originator del patrón Memento: puede guardarse
 * con {@link #save()} y reconstruirse a partir de un {@link GameMemento}.
 *
 * <p>El estado es mutable <em>a propósito</em>, pero solo mientras se ejecuta una {@link TurnAction}: {@link Game}
 * trabaja sobre una copia privada y la instala únicamente si la acción termina bien. Fuera de una acción nadie
 * recibe el estado activo.
 *
 * <p>El «banco» de cada bando guarda sus piezas que ya no están en el tablero, incluidas las capturadas.
 * Las extensiones forman un mapa opaco de valores <em>inmutables</em> donde los decoradores guardan su estado
 * (por ejemplo, un reloj o una racha). Como el mapa se incluye en cada copia, también se deshace y rehace junto
 * con el tablero.
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

    /** Posición inicial estándar, con las blancas al turno. */
    public static GameState initial() {
        return new GameState(Board.standard(), Team.WHITE);
    }

    /** Estado con una posición personalizada (se copia el tablero). El historial empieza cuando lo adopta {@link Game}. */
    public static GameState of(Board board, Team turn) {
        return new GameState(board.copy(), turn);
    }

    // ---- Memento ---------------------------------------------------------------------------------------

    public GameMemento save() {
        return new GameMemento(copy());
    }

    /** Copia profunda: tablero, historiales y extensiones son independientes del original. No copia eventos pendientes. */
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

    // ---- Tablero y turno -------------------------------------------------------------------------------

    /** Tablero activo del estado. Dentro de una acción es la copia de trabajo y se puede modificar. */
    public Board board() {
        return board;
    }

    public Team turn() {
        return turn;
    }

    public void setTurn(Team turn) {
        this.turn = Objects.requireNonNull(turn, "turn");
    }

    /** Casilla que saltó el peón al avanzar dos posiciones; permite una captura al paso durante un turno. */
    public Optional<Position> enPassantTarget() {
        return Optional.ofNullable(enPassantTarget);
    }

    public void setEnPassantTarget(Position target) {
        this.enPassantTarget = target;
    }

    /** Medias jugadas desde la última captura o movimiento de peón; a las 100 se declaran tablas (regla de los 50 movimientos). */
    public int halfmoveClock() {
        return halfmoveClock;
    }

    public void setHalfmoveClock(int halfmoveClock) {
        if (halfmoveClock < 0) {
            throw new IllegalArgumentException("halfmoveClock must not be negative");
        }
        this.halfmoveClock = halfmoveClock;
    }

    /** Lista activa de las piezas de {@code team} que están fuera del tablero. */
    public List<Piece> bench(Team team) {
        return bench.get(team);
    }

    // ---- Resultado ------------------------------------------------------------------------------------

    public GameStatus status() {
        return status;
    }

    public Optional<Team> winner() {
        return Optional.ofNullable(winner);
    }

    /** Termina la partida con el resultado indicado; {@code winner} es null si son tablas. */
    public void finish(GameStatus status, Team winner) {
        if (!status.isOver()) {
            throw new IllegalArgumentException("finish() needs a final status, not " + status);
        }
        this.status = status;
        this.winner = winner;
    }

    // ---- Extensiones y última jugada ------------------------------------------------------------------

    public <T> Optional<T> extension(String key, Class<T> type) {
        Object value = extensions.get(key);
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
    }

    /** Guarda un valor <em>inmutable</em> bajo una clave; se incluirá en cada copia del estado. */
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

    // ---- Eventos ---------------------------------------------------------------------------------------

    public void emit(GameEvent event) {
        pendingEvents.add(Objects.requireNonNull(event, "event"));
    }

    List<GameEvent> drainEvents() {
        List<GameEvent> events = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return events;
    }

    // ---- Historial de posiciones (repetición triple) --------------------------------------------------

    /** Borra las posiciones anteriores. Se usa cuando una acción impide volver a ellas. */
    public void clearPositionHistory() {
        positionCounts.clear();
    }

    /** Registra la posición actual y devuelve cuántas veces apareció hasta ahora. */
    public int recordCurrentPosition() {
        return positionCounts.merge(positionKey(), 1, Integer::sum);
    }

    /**
     * Identifica la posición para detectar repeticiones: ubicación de las piezas, bando al turno, derechos de
     * enroque y casilla de captura al paso, solo si esa captura es posible.
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
