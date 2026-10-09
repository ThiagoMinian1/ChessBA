package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Bishop;
import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BishopMoveRuleTest {

    private final BishopMoveRule rule = new BishopMoveRule();

    @Test
    void aBishopInTheCenterReachesBothDiagonalsInAllFourDirections() {
        Board board = Board.empty();
        Position from = new Position(3, 3);
        board.placePiece(from, new Bishop(Team.WHITE));

        List<Position> moves = rule.possibleMoves(from, board);

        assertEquals(13, moves.size());
    }

    @Test
    void aBishopNeverMovesOrthogonally() {
        Board board = Board.empty();
        Position from = new Position(3, 3);
        board.placePiece(from, new Bishop(Team.WHITE));

        List<Position> moves = rule.possibleMoves(from, board);

        assertFalse(moves.contains(new Position(3, 5)));
        assertFalse(moves.contains(new Position(5, 3)));
    }
}
