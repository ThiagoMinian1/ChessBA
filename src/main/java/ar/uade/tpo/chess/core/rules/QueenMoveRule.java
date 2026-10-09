package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * A queen moves like a rook and a bishop combined. Rather than
 * duplicating the sliding logic or forcing an inheritance relationship
 * between pieces that aren't really "a queen is-a rook", this rule is
 * composed out of the two existing ones.
 */
public final class QueenMoveRule implements MoveRule {

    private final MoveRule rookLikeMoves = new RookMoveRule();
    private final MoveRule bishopLikeMoves = new BishopMoveRule();

    @Override
    public List<Position> possibleMoves(Position from, Board board) {
        List<Position> moves = new ArrayList<>(rookLikeMoves.possibleMoves(from, board));
        moves.addAll(bishopLikeMoves.possibleMoves(from, board));
        return moves;
    }
}
