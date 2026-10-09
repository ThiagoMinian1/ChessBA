package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Knight;
import ar.uade.tpo.chess.core.model.Pawn;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnightMoveRuleTest {

    private final KnightMoveRule rule = new KnightMoveRule();

    @Test
    void aKnightInTheCenterHasEightPossibleMoves() {
        Board board = Board.empty();
        Position from = new Position(4, 4);
        board.placePiece(from, new Knight(Team.WHITE));

        List<Position> moves = rule.possibleMoves(from, board);

        assertEquals(8, moves.size());
    }

    @Test
    void aKnightJumpsOverPiecesInItsPath() {
        Board board = Board.empty();
        Position from = new Position(0, 1);
        board.placePiece(from, new Knight(Team.WHITE));
        // Surround the knight completely with friendly pawns.
        for (int column = 0; column <= 2; column++) {
            board.placePiece(new Position(1, column), new Pawn(Team.WHITE));
        }

        List<Position> moves = rule.possibleMoves(from, board);

        assertTrue(moves.contains(new Position(2, 0)));
        assertTrue(moves.contains(new Position(2, 2)));
    }

    @Test
    void aKnightCannotLandOnAFriendlyPiece() {
        Board board = Board.empty();
        Position from = new Position(4, 4);
        board.placePiece(from, new Knight(Team.WHITE));
        board.placePiece(new Position(6, 5), new Pawn(Team.WHITE));

        List<Position> moves = rule.possibleMoves(from, board);

        assertFalse(moves.contains(new Position(6, 5)));
    }
}
