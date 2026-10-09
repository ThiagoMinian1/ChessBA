package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Position;

import java.util.List;

/**
 * Strategy for computing where a piece could move to, given only the
 * board and its own movement pattern. Results are <b>pseudo-legal</b>:
 * they never land on a square held by the same team, but they do not
 * check whether the move would leave the mover's own king in check —
 * that filtering is {@code Game}'s responsibility, so a new piece type
 * only ever needs a new {@code MoveRule}, nothing else changes.
 */
public interface MoveRule {
    List<Position> possibleMoves(Position from, Board board);
}
