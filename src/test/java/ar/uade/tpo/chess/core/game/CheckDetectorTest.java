package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.King;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Rook;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckDetectorTest {

    @Test
    void neitherKingIsInCheckAtTheStartOfTheGame() {
        Board board = Board.standard();

        assertFalse(CheckDetector.isInCheck(board, Team.WHITE));
        assertFalse(CheckDetector.isInCheck(board, Team.BLACK));
    }

    @Test
    void aKingIsInCheckWhenAnEnemyRookSeesItAlongAClearFile() {
        Board board = Board.empty();
        board.placePiece(new Position(0, 4), new King(Team.WHITE));
        board.placePiece(new Position(7, 4), new Rook(Team.BLACK));

        assertTrue(CheckDetector.isInCheck(board, Team.WHITE));
    }

    @Test
    void aKingIsNotInCheckWhenTheAttackLineIsBlocked() {
        Board board = Board.empty();
        board.placePiece(new Position(0, 4), new King(Team.WHITE));
        board.placePiece(new Position(7, 4), new Rook(Team.BLACK));
        board.placePiece(new Position(3, 4), new Rook(Team.WHITE));

        assertFalse(CheckDetector.isInCheck(board, Team.WHITE));
    }

    @Test
    void findKingReturnsThePositionOfThatTeamsKing() {
        Board board = Board.standard();

        assertEquals(new Position(0, 4), CheckDetector.findKing(board, Team.WHITE));
        assertEquals(new Position(7, 4), CheckDetector.findKing(board, Team.BLACK));
    }
}
