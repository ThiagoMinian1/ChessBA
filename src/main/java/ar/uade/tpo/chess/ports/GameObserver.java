package ar.uade.tpo.chess.ports;

import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Team;

import java.util.Optional;

/**
 * Puerto de salida (Observer) para que otros componentes reciban novedades de la partida sin tener que consultar
 * el estado continuamente. Cada método tiene una implementación vacía, así que el observador solo redefine lo
 * que necesita. Los eventos se envían cuando la acción ya se aplicó y confirmó.
 */
public interface GameObserver {

    /** Se movió una pieza. {@code piece} indica su tipo (sigue siendo peón aunque haya promocionado). */
    default void onMovePlayed(Team team, Move move, PieceType piece) {
    }

    /** {@code capturer} capturó {@code captured} en {@code at} (en la captura al paso, donde estaba el peón). */
    default void onPieceCaptured(Team capturer, Piece captured, Position at) {
    }

    default void onPiecePromoted(Team team, Position at, PieceType promotedTo) {
    }

    /** El rey de {@code checkedTeam} está en jaque y la partida continúa (el jaque mate se informa en onGameEnded). */
    default void onCheck(Team checkedTeam) {
    }

    default void onGameEnded(GameStatus status, Optional<Team> winner) {
    }

    /** Mensaje de texto libre de una acción ajena al ajedrez estándar, como una mecánica temática. */
    default void onNotice(String message) {
    }
}
