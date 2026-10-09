package ar.uade.tpo.chess.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardTest {

    @Test
    void standardBoardPlacesSixteenPiecesPerTeam() {
        Board board = Board.standard();

        assertEquals(16, board.squaresWithPieceOf(Team.WHITE).size());
        assertEquals(16, board.squaresWithPieceOf(Team.BLACK).size());
    }

    @Test
    void standardBoardPlacesPawnsOnTheSecondAndSeventhRows() {
        Board board = Board.standard();

        for (int column = 0; column < 8; column++) {
            assertTrue(board.pieceAt(new Position(1, column)) instanceof Pawn);
            assertTrue(board.pieceAt(new Position(6, column)) instanceof Pawn);
        }
    }

    @Test
    void standardBoardPlacesKingsOnTheEFile() {
        Board board = Board.standard();

        assertTrue(board.pieceAt(new Position(0, 4)) instanceof King);
        assertTrue(board.pieceAt(new Position(7, 4)) instanceof King);
    }

    @Test
    void emptyBoardHasNoPieces() {
        Board board = Board.empty();

        assertTrue(board.squaresWithPieceOf(Team.WHITE).isEmpty());
        assertTrue(board.squaresWithPieceOf(Team.BLACK).isEmpty());
    }

    @Test
    void applyMoveRelocatesThePieceAndReturnsNullWhenDestinationWasEmpty() {
        Board board = Board.empty();
        Position from = new Position(1, 0);
        Position to = new Position(3, 0);
        board.placePiece(from, new Pawn(Team.WHITE));

        Piece captured = board.applyMove(from, to);

        assertNull(captured);
        assertTrue(board.isEmpty(from));
        assertTrue(board.pieceAt(to) instanceof Pawn);
    }

    @Test
    void applyMoveReturnsTheCapturedPiece() {
        Board board = Board.empty();
        Position from = new Position(1, 0);
        Position to = new Position(2, 0);
        board.placePiece(from, new Rook(Team.WHITE));
        Piece blackPawn = new Pawn(Team.BLACK);
        board.placePiece(to, blackPawn);

        Piece captured = board.applyMove(from, to);

        assertEquals(blackPawn, captured);
        assertTrue(board.pieceAt(to) instanceof Rook);
    }

    @Test
    void copyIsIndependentFromTheOriginal() {
        Board original = Board.standard();
        Board copy = original.copy();

        copy.applyMove(new Position(1, 0), new Position(3, 0));

        assertFalse(original.isEmpty(new Position(1, 0)));
        assertTrue(copy.isEmpty(new Position(1, 0)));
        assertNotSame(original.pieceAt(new Position(1, 1)), copy.pieceAt(new Position(1, 1)));
    }
}
