package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static ar.uade.tpo.chess.TestSupport.boardOf;
import static ar.uade.tpo.chess.TestSupport.pos;
import static ar.uade.tpo.chess.TestSupport.targets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveGeneratorTest {

    @Test
    void startingPositionHasTwentyMovesForEachSide() {
        Board board = Board.standard();
        assertEquals(20, MoveGenerator.legalMoves(board, Team.WHITE, null).size());
        assertEquals(20, MoveGenerator.legalMoves(board, Team.BLACK, null).size());
    }

    @Test
    void aPinnedPieceMayOnlyMoveAlongThePin() {
        Board board = boardOf("Ke1", "Re2", "re8", "ka8");
        List<Move> moves = MoveGenerator.legalMovesFrom(board, Team.WHITE, null, pos("e2"));
        assertEquals(Set.of("e3", "e4", "e5", "e6", "e7", "e8"), targets(moves));
    }

    @Test
    void aPinnedKnightCannotMoveAtAll() {
        Board board = boardOf("Ke1", "Ne2", "re8", "ka8");
        assertTrue(MoveGenerator.legalMovesFrom(board, Team.WHITE, null, pos("e2")).isEmpty());
    }

    @Test
    void theKingCannotStepIntoCheck() {
        Board board = boardOf("Ke1", "re8", "ka8");
        List<Move> moves = MoveGenerator.legalMoves(board, Team.WHITE, null);
        assertEquals(Set.of("d1", "d2", "f1", "f2"), targets(moves));
    }

    @Test
    void whenInCheckOnlyMovesThatResolveItAreLegal() {
        // The rook on e8 checks the king on e1. White can block with the rook on a4 on e4, or move the king.
        Board board = boardOf("Ke1", "Ra4", "re8", "ka8");
        List<Move> moves = MoveGenerator.legalMoves(board, Team.WHITE, null);
        assertTrue(targets(MoveGenerator.legalMovesFrom(board, Team.WHITE, null, pos("a4"))).equals(Set.of("e4")));
        assertEquals(5, moves.size());
    }

    @Test
    void aCheckingPieceMayBeCaptured() {
        Board board = boardOf("Ke1", "Ra8", "re8", "kh8");
        // Ra8 can capture the checking rook on e8 along the rank (b8..d8 empty).
        assertTrue(targets(MoveGenerator.legalMovesFrom(board, Team.WHITE, null, pos("a8"))).contains("e8"));
    }

    @Test
    void moveGenerationIsRestrictedToTheTeamAskedFor() {
        Board board = Board.standard();
        assertTrue(MoveGenerator.legalMovesFrom(board, Team.WHITE, null, pos("e7")).isEmpty());
        assertTrue(MoveGenerator.legalMovesFrom(board, Team.WHITE, null, pos("e4")).isEmpty());
    }

    @Test
    void detectsCheckForEitherTeam() {
        Board board = boardOf("Ke1", "re8", "ka8");
        assertTrue(MoveGenerator.isInCheck(board, Team.WHITE));
        assertFalse(MoveGenerator.isInCheck(board, Team.BLACK));
    }

    @Test
    void aBoardWithoutAKingIsNeverInCheck() {
        assertFalse(MoveGenerator.isInCheck(boardOf("Ra1", "re8"), Team.WHITE));
    }

    @Test
    void hasLegalMoveAgreesWithTheFullList() {
        assertTrue(MoveGenerator.hasLegalMove(Board.standard(), Team.WHITE, null));
        // Stalemate: black king a8 boxed in by Kb6 and Qc7, not in check.
        Board stalemate = boardOf("ka8", "Kb6", "Qc7");
        assertFalse(MoveGenerator.hasLegalMove(stalemate, Team.BLACK, null));
        assertTrue(MoveGenerator.legalMoves(stalemate, Team.BLACK, null).isEmpty());
        assertFalse(MoveGenerator.isInCheck(stalemate, Team.BLACK));
    }

    @Test
    void generatingMovesDoesNotChangeTheBoard() {
        Board board = Board.standard();
        Board before = board.copy();
        MoveGenerator.legalMoves(board, Team.WHITE, null);
        MoveGenerator.legalMoves(board, Team.BLACK, null);
        assertEquals(before, board);
    }

    @Test
    void applyingAMoveSetsTheMovedFlagAndRemovesTheCapturedPiece() {
        Board board = boardOf("Ra1", "pa5");
        MoveOutcome outcome = MoveApplier.apply(board, Move.normal(pos("a1"), pos("a5")));
        assertTrue(outcome.isCapture());
        assertEquals(pos("a5"), outcome.capturedAt().orElseThrow());
        assertTrue(board.pieceAt(pos("a5")).orElseThrow().hasMoved());
        assertTrue(board.isEmpty(pos("a1")));
    }
}
