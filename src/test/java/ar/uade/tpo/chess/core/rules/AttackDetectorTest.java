package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import static ar.uade.tpo.chess.TestSupport.boardOf;
import static ar.uade.tpo.chess.TestSupport.pos;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttackDetectorTest {

    private static boolean whiteAttacks(Board board, String square) {
        return AttackDetector.isAttacked(board, pos(square), Team.WHITE);
    }

    private static boolean blackAttacks(Board board, String square) {
        return AttackDetector.isAttacked(board, pos(square), Team.BLACK);
    }

    @Test
    void whitePawnAttacksDiagonallyUpwardOnly() {
        Board board = boardOf("Pe4");
        assertTrue(whiteAttacks(board, "d5"));
        assertTrue(whiteAttacks(board, "f5"));
        assertFalse(whiteAttacks(board, "e5"));
        assertFalse(whiteAttacks(board, "d3"));
        assertFalse(whiteAttacks(board, "f3"));
    }

    @Test
    void blackPawnAttacksDiagonallyDownwardOnly() {
        Board board = boardOf("pe5");
        assertTrue(blackAttacks(board, "d4"));
        assertTrue(blackAttacks(board, "f4"));
        assertFalse(blackAttacks(board, "e4"));
        assertFalse(blackAttacks(board, "d6"));
    }

    @Test
    void pawnAttacksDoNotWrapAroundTheEdge() {
        Board board = boardOf("Pa4");
        assertTrue(whiteAttacks(board, "b5"));
        assertFalse(whiteAttacks(board, "h5"));
        assertFalse(whiteAttacks(boardOf("Ph4"), "a5"));
    }

    @Test
    void knightAttacksItsEightSquares() {
        Board board = boardOf("Nd4");
        for (String square : new String[] {"b3", "b5", "c2", "c6", "e2", "e6", "f3", "f5"}) {
            assertTrue(whiteAttacks(board, square), square);
        }
        assertFalse(whiteAttacks(board, "d5"));
        assertFalse(whiteAttacks(board, "e5"));
    }

    @Test
    void knightAttackIgnoresBlockers() {
        assertTrue(whiteAttacks(boardOf("Nd4", "Pd5", "Pe4", "pe5"), "e6"));
    }

    @Test
    void rookAttacksAlongLinesUntilBlocked() {
        Board board = boardOf("Rd4", "pd7");
        assertTrue(whiteAttacks(board, "d6"));
        assertTrue(whiteAttacks(board, "d7"));
        assertFalse(whiteAttacks(board, "d8"));
        assertTrue(whiteAttacks(board, "a4"));
        assertFalse(whiteAttacks(board, "e5"));
    }

    @Test
    void bishopAttacksAlongDiagonalsUntilBlocked() {
        Board board = boardOf("Bd4", "pf6");
        assertTrue(whiteAttacks(board, "e5"));
        assertTrue(whiteAttacks(board, "f6"));
        assertFalse(whiteAttacks(board, "g7"));
        assertFalse(whiteAttacks(board, "d5"));
    }

    @Test
    void queenAttacksLikeRookAndBishop() {
        Board board = boardOf("Qd4");
        assertTrue(whiteAttacks(board, "d8"));
        assertTrue(whiteAttacks(board, "h4"));
        assertTrue(whiteAttacks(board, "h8"));
        assertTrue(whiteAttacks(board, "a1"));
        assertFalse(whiteAttacks(board, "e6"));
    }

    @Test
    void kingAttacksAdjacentSquaresOnly() {
        Board board = boardOf("Kd4");
        assertTrue(whiteAttacks(board, "e5"));
        assertTrue(whiteAttacks(board, "d3"));
        assertFalse(whiteAttacks(board, "d6"));
        assertFalse(whiteAttacks(board, "f4"));
    }

    @Test
    void attacksAreReportedPerTeam() {
        Board board = boardOf("Rd4", "rh8");
        assertTrue(whiteAttacks(board, "d1"));
        assertFalse(blackAttacks(board, "d1"));
        assertTrue(blackAttacks(board, "h1"));
        assertFalse(whiteAttacks(board, "h1"));
    }

    @Test
    void anOccupiedSquareCanBeAttacked() {
        Board board = boardOf("Rd1", "pd8");
        assertTrue(whiteAttacks(board, "d8"));
    }

    @Test
    void nothingAttacksOnAnEmptyBoard() {
        for (String square : new String[] {"a1", "e4", "h8"}) {
            assertFalse(whiteAttacks(Board.empty(), square));
            assertFalse(blackAttacks(Board.empty(), square));
        }
    }

    @Test
    void aPinnedPieceStillGivesCheck() {
        // El alfil blanco de e2 está clavado al rey, pero igual ataca al rey negro de a6.
        Board board = boardOf("Ke1", "Be2", "re8", "ka6");
        assertTrue(whiteAttacks(board, "a6"));
    }
}
