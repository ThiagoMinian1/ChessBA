package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Queen;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueenMoveRuleTest {

    private final QueenMoveRule rule = new QueenMoveRule();

    @Test
    void aQueenInTheCenterCombinesRookAndBishopReach() {
        Board board = Board.empty();
        Position from = new Position(3, 3);
        board.placePiece(from, new Queen(Team.WHITE));

        List<Position> moves = rule.possibleMoves(from, board);

        // 14 rook-like squares + 13 bishop-like squares from the center
        assertEquals(27, moves.size());
        assertTrue(moves.contains(new Position(3, 7))); // horizontal
        assertTrue(moves.contains(new Position(7, 3))); // vertical
        assertTrue(moves.contains(new Position(6, 6))); // diagonal
    }
}
