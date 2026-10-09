package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.King;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Rook;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KingMoveRuleTest {

    private final KingMoveRule rule = new KingMoveRule();

    @Test
    void aKingInTheCenterHasEightPossibleMoves() {
        Board board = Board.empty();
        Position from = new Position(4, 4);
        board.placePiece(from, new King(Team.WHITE));

        List<Position> moves = rule.possibleMoves(from, board);

        assertEquals(8, moves.size());
    }

    @Test
    void aKingOnTheCornerHasOnlyThreePossibleMoves() {
        Board board = Board.empty();
        Position from = new Position(0, 0);
        board.placePiece(from, new King(Team.WHITE));

        List<Position> moves = rule.possibleMoves(from, board);

        assertEquals(3, moves.size());
    }

    @Test
    void aKingCanCaptureAnAdjacentEnemyPiece() {
        Board board = Board.empty();
        Position from = new Position(4, 4);
        board.placePiece(from, new King(Team.WHITE));
        board.placePiece(new Position(4, 5), new Rook(Team.BLACK));

        List<Position> moves = rule.possibleMoves(from, board);

        assertTrue(moves.contains(new Position(4, 5)));
    }
}
