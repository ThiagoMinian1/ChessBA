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
 * Interfaz de una partida para los componentes externos. {@link Game} la implementa para el ajedrez estándar;
 * los decoradores la envuelven para agregar mecánicas opcionales sin modificar el núcleo.
 *
 * <p>Todas las acciones del jugador pasan por {@link #execute(TurnAction)} (Command). Los métodos auxiliares
 * de abajo se apoyan en esa operación, así que un decorador que la intercepte también recibe esas acciones.
 */
public interface ChessGame {

    Team currentTurn();

    /** Copia del tablero; modificarla no afecta la partida. */
    Board board();

    GameStatus status();

    Optional<Team> winner();

    /** Indica si el bando al que le toca jugar está en jaque. */
    boolean isInCheck();

    /** Jugadas legales del bando al que le toca; queda vacío cuando termina la partida. */
    List<Move> legalMoves();

    List<Move> legalMovesFrom(Position from);

    /** Piezas de {@code team} que están fuera del tablero (capturadas o intercambiadas por alguna mecánica). */
    List<Piece> bench(Team team);

    /** Medias jugadas desde la última captura o movimiento de peón. */
    int halfmoveClock();

    /** Acceso de lectura al estado de los decoradores, que se conserva en las copias de la partida. */
    <T> Optional<T> extension(String key, Class<T> type);

    /** Ejecuta una acción: la valida, la aplica de forma atómica y la registra para deshacer o rehacer. */
    void execute(TurnAction action);

    /** Revierte la última acción. Devuelve false si no hay nada que deshacer. */
    boolean undo();

    /** Vuelve a aplicar la última acción deshecha. Devuelve false si no hay nada que rehacer. */
    boolean redo();

    boolean canUndo();

    boolean canRedo();

    /** Descripciones de las acciones aplicadas, de la más antigua a la más reciente. */
    List<String> history();

    void addObserver(GameObserver observer);

    void removeObserver(GameObserver observer);

    // ---- Métodos auxiliares basados en execute() -------------------------------------------------------

    default void play(Move move) {
        execute(new MoveAction(move));
    }

    /** Juega el movimiento legal de {@code from} a {@code to}; lanza {@link PromotionRequiredException} si hay que promocionar. */
    default void play(Position from, Position to) {
        play(from, to, null);
    }

    /** Como {@link #play(Position, Position)}, pero permite elegir la pieza de promoción (null si no hay promoción). */
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

    /** Indica si mover de {@code from} a {@code to} es una promoción y hay que elegir una pieza. */
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
