package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Team;

/**
 * What the most recent move did, left in the {@link GameState} so that actions wrapped around a move
 * (decorators) can react to it without re-deriving it.
 *
 * @param team      who moved
 * @param movedType the kind of piece that moved (a pawn, even when it promoted)
 * @param capture   whether the move captured a piece (en passant included)
 */
public record MoveRecord(Team team, Move move, PieceType movedType, boolean capture) {

    public boolean isPawnMove() {
        return movedType == PieceType.PAWN;
    }
}
