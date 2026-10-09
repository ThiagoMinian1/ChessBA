package ar.uade.tpo.chess.ports;

import ar.uade.tpo.chess.core.game.GameStatus;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.Team;

/**
 * Output port: how the core tells the outside world what happened,
 * without knowing who (or what) is listening. A UI adapter, a console
 * logger and a future network adapter can all implement this the same
 * way, independently of each other.
 */
public interface GameObserver {
    void onMove(Move move);

    void onCapture(Piece capturedPiece);

    void onCheck(Team teamInCheck);

    void onGameEnded(GameStatus result);
}
