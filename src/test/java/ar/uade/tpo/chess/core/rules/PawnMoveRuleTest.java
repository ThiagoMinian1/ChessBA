package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.MoveType;
import ar.uade.tpo.chess.core.model.PieceType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static ar.uade.tpo.chess.TestSupport.boardOf;
import static ar.uade.tpo.chess.TestSupport.pseudoMoves;
import static ar.uade.tpo.chess.TestSupport.pseudoTargets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PawnMoveRuleTest {

    @Test
    void whitePawnOnItsStartingRankMovesOneOrTwoSquares() {
        Board board = boardOf("Pe2");
        assertEquals(Set.of("e3", "e4"), pseudoTargets(board, "e2"));
        List<Move> moves = pseudoMoves(board, "e2");
        assertTrue(moves.stream().anyMatch(m -> m.to().toString().equals("e3") && m.type() == MoveType.NORMAL));
        assertTrue(moves.stream().anyMatch(
                m -> m.to().toString().equals("e4") && m.type() == MoveType.PAWN_DOUBLE_PUSH));
    }

    @Test
    void pawnOffItsStartingRankMovesOnlyOneSquare() {
        assertEquals(Set.of("e4"), pseudoTargets(boardOf("Pe3"), "e3"));
        assertEquals(Set.of("e5"), pseudoTargets(boardOf("pe6"), "e6"));
    }

    @Test
    void blackPawnMovesDownTheBoard() {
        assertEquals(Set.of("e6", "e5"), pseudoTargets(boardOf("pe7"), "e7"));
    }

    @Test
    void aPieceDirectlyAheadBlocksTheAdvance() {
        assertEquals(Set.of(), pseudoTargets(boardOf("Pe2", "pe3"), "e2"));
        assertEquals(Set.of(), pseudoTargets(boardOf("Pe2", "Ne3"), "e2"));
    }

    @Test
    void aPieceTwoSquaresAheadBlocksOnlyTheDoublePush() {
        assertEquals(Set.of("e3"), pseudoTargets(boardOf("Pe2", "pe4"), "e2"));
    }

    @Test
    void pawnCapturesDiagonallyForwardOnly() {
        Board board = boardOf("Pe4", "pd5", "pf5", "pe5", "pd3", "pf3");
        assertEquals(Set.of("d5", "f5"), pseudoTargets(board, "e4"));
    }

    @Test
    void pawnCannotCaptureItsOwnPieces() {
        assertEquals(Set.of("e5", "f5"), pseudoTargets(boardOf("Pe4", "Pd5", "pf5"), "e4"));
    }

    @Test
    void pawnCannotMoveDiagonallyToAnEmptySquare() {
        assertEquals(Set.of("e5"), pseudoTargets(boardOf("Pe4"), "e4"));
    }

    @Test
    void edgePawnsDoNotWrapAround() {
        assertEquals(Set.of("a5", "b5"), pseudoTargets(boardOf("Pa4", "pb5", "ph5"), "a4"));
        assertEquals(Set.of("h5", "g5"), pseudoTargets(boardOf("Ph4", "pg5", "pa5"), "h4"));
    }

    @Test
    void reachingTheLastRankOffersFourPromotions() {
        List<Move> moves = pseudoMoves(boardOf("Pe7"), "e7");
        assertEquals(4, moves.size());
        Set<PieceType> promotions = EnumSet.noneOf(PieceType.class);
        for (Move move : moves) {
            assertEquals(MoveType.PROMOTION, move.type());
            assertEquals("e8", move.to().toString());
            promotions.add(move.promotion());
        }
        assertEquals(EnumSet.of(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT), promotions);
    }

    @Test
    void promotionByCaptureOffersFourPromotionsPerTarget() {
        List<Move> moves = pseudoMoves(boardOf("Pe7", "rd8", "re8", "nf8"), "e7");
        assertEquals(8, moves.size());
        assertEquals(Set.of("d8", "f8"), pseudoTargets(boardOf("Pe7", "rd8", "re8", "nf8"), "e7"));
        for (Move move : moves) {
            assertEquals(MoveType.PROMOTION, move.type());
        }
    }

    @Test
    void blackPawnPromotesOnTheFirstRank() {
        List<Move> moves = pseudoMoves(boardOf("pe2"), "e2");
        assertEquals(4, moves.size());
        assertEquals(Set.of("e1"), pseudoTargets(boardOf("pe2"), "e2"));
    }

    @Test
    void enPassantIsAvailableOnlyOnTheTargetSquareNextToTheVictim() {
        Board board = boardOf("Pe5", "pd5");
        List<Move> moves = pseudoMoves(board, "e5", "d6");
        assertTrue(moves.stream().anyMatch(m -> m.type() == MoveType.EN_PASSANT && m.to().toString().equals("d6")));
        assertTrue(moves.stream().anyMatch(m -> m.type() == MoveType.NORMAL && m.to().toString().equals("e6")));
        assertEquals(2, moves.size());
    }

    @Test
    void noEnPassantWithoutATarget() {
        assertEquals(Set.of("e6"), pseudoTargets(boardOf("Pe5", "pd5"), "e5"));
    }

    @Test
    void noEnPassantWhenTheTargetIsNotBesideThePawn() {
        Board board = boardOf("Pe5", "pd5");
        assertEquals(Set.of("e6"), ar.uade.tpo.chess.TestSupport.targets(pseudoMoves(board, "e5", "f6")));
    }

    @Test
    void noEnPassantWhenThereIsNoVictim() {
        Board board = boardOf("Pe5");
        assertEquals(Set.of("e6"), ar.uade.tpo.chess.TestSupport.targets(pseudoMoves(board, "e5", "d6")));
    }

    @Test
    void noEnPassantAgainstYourOwnPawn() {
        Board board = boardOf("Pe5", "Pd5");
        assertEquals(Set.of("e6"), ar.uade.tpo.chess.TestSupport.targets(pseudoMoves(board, "e5", "d6")));
    }

    @Test
    void blackCanCaptureEnPassantToo() {
        Board board = boardOf("pd4", "Pe4");
        List<Move> moves = pseudoMoves(board, "d4", "e3");
        assertTrue(moves.stream().anyMatch(m -> m.type() == MoveType.EN_PASSANT && m.to().toString().equals("e3")));
    }

    @Test
    void aPawnOnTheLastRankHasNoMoves() {
        assertTrue(pseudoMoves(boardOf("Pe8"), "e8").isEmpty());
    }
}
