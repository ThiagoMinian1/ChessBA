package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Pawn;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Rook;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PawnMoveRuleTest {

    private final PawnMoveRule rule = new PawnMoveRule();

    @Test
    void onItsStartingRowAWhitePawnCanAdvanceOneOrTwoSquares() {
        Board board = Board.empty();
        Position from = new Position(1, 4);
        board.placePiece(from, new Pawn(Team.WHITE));

        List<Position> moves = rule.possibleMoves(from, board);

        assertEquals(List.of(new Position(2, 4), new Position(3, 4)), moves);
    }

    @Test
    void afterItsFirstMoveAPawnCanOnlyAdvanceOneSquare() {
        Board board = Board.empty();
        Position from = new Position(2, 4);
        board.placePiece(from, new Pawn(Team.WHITE));

        List<Position> moves = rule.possibleMoves(from, board);

        assertEquals(List.of(new Position(3, 4)), moves);
    }

    @Test
    void aPawnCannotAdvanceThroughABlockingPiece() {
        Board board = Board.empty();
        Position from = new Position(1, 4);
        board.placePiece(from, new Pawn(Team.WHITE));
        board.placePiece(new Position(2, 4), new Rook(Team.BLACK));

        List<Position> moves = rule.possibleMoves(from, board);

        assertTrue(moves.isEmpty());
    }

    @Test
    void aPawnCanCaptureDiagonallyButNotMoveDiagonallyToAnEmptySquare() {
        Board board = Board.empty();
        Position from = new Position(4, 4);
        board.placePiece(from, new Pawn(Team.WHITE));
        board.placePiece(new Position(5, 5), new Rook(Team.BLACK));

        List<Position> moves = rule.possibleMoves(from, board);

        assertTrue(moves.contains(new Position(5, 5)));
        assertFalse(moves.contains(new Position(5, 3)));
    }

    @Test
    void blackPawnsMoveDownTheBoard() {
        Board board = Board.empty();
        Position from = new Position(6, 4);
        board.placePiece(from, new Pawn(Team.BLACK));

        List<Position> moves = rule.possibleMoves(from, board);

        assertEquals(List.of(new Position(5, 4), new Position(4, 4)), moves);
    }
}
