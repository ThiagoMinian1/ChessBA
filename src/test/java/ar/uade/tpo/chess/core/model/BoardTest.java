package ar.uade.tpo.chess.core.model;

import org.junit.jupiter.api.Test;

import static ar.uade.tpo.chess.TestSupport.boardOf;
import static ar.uade.tpo.chess.TestSupport.pos;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardTest {

    @Test
    void emptyBoardHasNoPieces() {
        Board board = Board.empty();
        assertTrue(board.pieces().isEmpty());
        assertTrue(board.pieceAt(pos("e4")).isEmpty());
        assertTrue(board.isEmpty(pos("e4")));
        assertTrue(board.findKing(Team.WHITE).isEmpty());
    }

    @Test
    void standardBoardHasThirtyTwoPiecesSixteenPerTeam() {
        Board board = Board.standard();
        assertEquals(32, board.pieces().size());
        assertEquals(16, board.pieces(Team.WHITE).size());
        assertEquals(16, board.pieces(Team.BLACK).size());
    }

    @Test
    void standardBoardHasTheRightBackRanks() {
        Board board = Board.standard();
        PieceType[] expected = {
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        };
        for (int file = 0; file < 8; file++) {
            Piece white = board.pieceAt(new Position(file, 0)).orElseThrow();
            Piece black = board.pieceAt(new Position(file, 7)).orElseThrow();
            assertEquals(expected[file], white.type());
            assertEquals(expected[file], black.type());
            assertEquals(Team.WHITE, white.team());
            assertEquals(Team.BLACK, black.team());
        }
    }

    @Test
    void standardBoardHasPawnsOnTheSecondAndSeventhRanksAndNothingBetween() {
        Board board = Board.standard();
        for (int file = 0; file < 8; file++) {
            assertEquals(PieceType.PAWN, board.pieceAt(new Position(file, 1)).orElseThrow().type());
            assertEquals(Team.WHITE, board.pieceAt(new Position(file, 1)).orElseThrow().team());
            assertEquals(PieceType.PAWN, board.pieceAt(new Position(file, 6)).orElseThrow().type());
            assertEquals(Team.BLACK, board.pieceAt(new Position(file, 6)).orElseThrow().team());
            for (int rank = 2; rank <= 5; rank++) {
                assertTrue(board.isEmpty(new Position(file, rank)));
            }
        }
    }

    @Test
    void queensStandOnTheirOwnColour() {
        Board board = Board.standard();
        assertEquals(PieceType.QUEEN, board.pieceAt(pos("d1")).orElseThrow().type());
        assertEquals(Team.WHITE, board.pieceAt(pos("d1")).orElseThrow().team());
        assertEquals(PieceType.QUEEN, board.pieceAt(pos("d8")).orElseThrow().type());
        assertEquals(Team.BLACK, board.pieceAt(pos("d8")).orElseThrow().team());
    }

    @Test
    void findsTheKings() {
        Board board = Board.standard();
        assertEquals(pos("e1"), board.findKing(Team.WHITE).orElseThrow());
        assertEquals(pos("e8"), board.findKing(Team.BLACK).orElseThrow());
    }

    @Test
    void placeAndRemoveWork() {
        Board board = Board.empty();
        Piece rook = new Rook(Team.WHITE);
        board.place(pos("a1"), rook);
        assertEquals(rook, board.pieceAt(pos("a1")).orElseThrow());
        assertFalse(board.isEmpty(pos("a1")));
        assertEquals(rook, board.remove(pos("a1")).orElseThrow());
        assertTrue(board.isEmpty(pos("a1")));
        assertTrue(board.remove(pos("a1")).isEmpty());
    }

    @Test
    void placingReplacesWhateverWasThere() {
        Board board = boardOf("Pe4");
        board.place(pos("e4"), new Queen(Team.BLACK));
        assertEquals(PieceType.QUEEN, board.pieceAt(pos("e4")).orElseThrow().type());
        assertEquals(1, board.pieces().size());
    }

    @Test
    void movePieceRelocatesAndReportsTheCapturedPiece() {
        Board board = boardOf("Ra1", "pa5");
        Piece captured = board.movePiece(pos("a1"), pos("a5")).orElseThrow();
        assertEquals(PieceType.PAWN, captured.type());
        assertTrue(board.isEmpty(pos("a1")));
        assertEquals(PieceType.ROOK, board.pieceAt(pos("a5")).orElseThrow().type());
        assertEquals(1, board.pieces().size());
    }

    @Test
    void movePieceToAnEmptySquareCapturesNothing() {
        Board board = boardOf("Ra1");
        assertTrue(board.movePiece(pos("a1"), pos("a4")).isEmpty());
    }

    @Test
    void movePieceFromAnEmptySquareFails() {
        assertThrows(IllegalArgumentException.class, () -> Board.empty().movePiece(pos("a1"), pos("a2")));
    }

    @Test
    void copyIsIndependentFromTheOriginal() {
        Board original = Board.standard();
        Board copy = original.copy();
        assertEquals(original, copy);
        copy.remove(pos("e2"));
        copy.place(pos("e4"), new Pawn(Team.WHITE));
        assertNotEquals(original, copy);
        assertEquals(PieceType.PAWN, original.pieceAt(pos("e2")).orElseThrow().type());
        assertTrue(original.isEmpty(pos("e4")));
    }

    @Test
    void squareAtDescribesEmptyAndOccupiedCells() {
        Board board = boardOf("Ke1");
        Square occupied = board.squareAt(pos("e1"));
        Square empty = board.squareAt(pos("e2"));
        assertFalse(occupied.isEmpty());
        assertEquals(PieceType.KING, occupied.occupant().orElseThrow().type());
        assertTrue(empty.isEmpty());
        assertTrue(empty.occupant().isEmpty());
        assertEquals(pos("e2"), empty.position());
    }

    @Test
    void equalBoardsHaveEqualHashCodes() {
        assertEquals(Board.standard(), Board.standard());
        assertEquals(Board.standard().hashCode(), Board.standard().hashCode());
        assertNotEquals(Board.standard(), Board.empty());
    }

    @Test
    void diagramRoundTripsTheStandardBoard() {
        Board board = Board.standard();
        assertEquals(board, Board.fromDiagram(board.toString().split("\n")));
    }

    @Test
    void diagramReadsRankEightFirst() {
        Board board = Board.fromDiagram(
                "....k...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "R...K...");
        assertEquals(PieceType.KING, board.pieceAt(pos("e8")).orElseThrow().type());
        assertEquals(Team.BLACK, board.pieceAt(pos("e8")).orElseThrow().team());
        assertEquals(PieceType.ROOK, board.pieceAt(pos("a1")).orElseThrow().type());
        assertEquals(Team.WHITE, board.pieceAt(pos("e1")).orElseThrow().team());
    }

    @Test
    void diagramRejectsBadShapesAndLetters() {
        assertThrows(IllegalArgumentException.class, () -> Board.fromDiagram("........"));
        assertThrows(IllegalArgumentException.class, () -> Board.fromDiagram(
                "......." , "........", "........", "........", "........", "........", "........", "........"));
        assertThrows(IllegalArgumentException.class, () -> Board.fromDiagram(
                "x.......", "........", "........", "........", "........", "........", "........", "........"));
    }
}
