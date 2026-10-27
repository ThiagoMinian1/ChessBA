package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Bishop;
import ar.uade.tpo.chess.core.model.King;
import ar.uade.tpo.chess.core.model.Knight;
import ar.uade.tpo.chess.core.model.Pawn;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Queen;
import ar.uade.tpo.chess.core.model.Rook;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static ar.uade.tpo.chess.TestSupport.boardOf;
import static ar.uade.tpo.chess.TestSupport.pseudoMoves;
import static ar.uade.tpo.chess.TestSupport.pseudoTargets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Movement of rook, knight, bishop, queen and king, plus the rule factory that hands them out. */
class PieceMovementTest {

    // ---- Rook ------------------------------------------------------------------------------------------

    @Test
    void rookOnAnEmptyBoardSlidesAlongRankAndFile() {
        Set<String> targets = pseudoTargets(boardOf("Rd4"), "d4");
        assertEquals(14, targets.size());
        assertEquals(Set.of("d1", "d2", "d3", "d5", "d6", "d7", "d8", "a4", "b4", "c4", "e4", "f4", "g4", "h4"),
                targets);
    }

    @Test
    void rookStopsBeforeOwnPiecesAndCapturesEnemyOnes() {
        Set<String> targets = pseudoTargets(boardOf("Rd4", "Pd6", "pb4"), "d4");
        assertEquals(Set.of("d1", "d2", "d3", "d5", "c4", "b4", "e4", "f4", "g4", "h4"), targets);
    }

    @Test
    void rookCannotJumpOverAnEnemyPiece() {
        assertFalse(pseudoTargets(boardOf("Rd4", "pd6"), "d4").contains("d7"));
    }

    @Test
    void rookInTheCornerHasFourteenMoves() {
        assertEquals(14, pseudoTargets(boardOf("Ra1"), "a1").size());
    }

    @Test
    void rookNeverMovesDiagonally() {
        assertFalse(pseudoTargets(boardOf("Rd4"), "d4").contains("e5"));
    }

    // ---- Knight ----------------------------------------------------------------------------------------

    @Test
    void knightInTheCentreHasEightMoves() {
        assertEquals(Set.of("b3", "b5", "c2", "c6", "e2", "e6", "f3", "f5"), pseudoTargets(boardOf("Nd4"), "d4"));
    }

    @Test
    void knightInTheCornerHasTwoMoves() {
        assertEquals(Set.of("b3", "c2"), pseudoTargets(boardOf("Na1"), "a1"));
        assertEquals(Set.of("f7", "g6"), pseudoTargets(boardOf("Nh8"), "h8"));
    }

    @Test
    void knightJumpsOverPiecesButNotOntoOwnOnes() {
        Set<String> targets = pseudoTargets(
                boardOf("Nd4", "Pd5", "Pd3", "Pe4", "Pc4", "Pb3", "pf5"), "d4");
        assertEquals(Set.of("b5", "c2", "c6", "e2", "e6", "f3", "f5"), targets);
    }

    @Test
    void knightOnTheEdgeDoesNotWrapAround() {
        assertEquals(Set.of("b2", "b6", "c3", "c5"), pseudoTargets(boardOf("Na4"), "a4"));
        assertEquals(Set.of("g2", "g6", "f3", "f5"), pseudoTargets(boardOf("Nh4"), "h4"));
    }

    // ---- Bishop ----------------------------------------------------------------------------------------

    @Test
    void bishopOnAnEmptyBoardSlidesAlongDiagonals() {
        Set<String> targets = pseudoTargets(boardOf("Bd4"), "d4");
        assertEquals(13, targets.size());
        assertTrue(targets.containsAll(Set.of("e5", "f6", "g7", "h8", "c5", "b6", "a7", "e3", "f2", "g1", "c3", "b2", "a1")));
    }

    @Test
    void bishopIsBlockedByOwnPiecesAndCapturesEnemies() {
        Set<String> targets = pseudoTargets(boardOf("Bd4", "Pf6", "pf2"), "d4");
        assertTrue(targets.contains("e5"));
        assertFalse(targets.contains("f6"));
        assertFalse(targets.contains("g7"));
        assertTrue(targets.contains("e3"));
        assertTrue(targets.contains("f2"));
        assertFalse(targets.contains("g1"));
        assertEquals(9, targets.size());
    }

    @Test
    void bishopNeverMovesStraight() {
        Set<String> targets = pseudoTargets(boardOf("Bd4"), "d4");
        assertFalse(targets.contains("d5"));
        assertFalse(targets.contains("e4"));
    }

    @Test
    void bishopStaysOnItsOwnColour() {
        boolean startsLight = ar.uade.tpo.chess.TestSupport.pos("c1").isLightSquare();
        for (String target : pseudoTargets(boardOf("Bc1"), "c1")) {
            assertEquals(startsLight, ar.uade.tpo.chess.TestSupport.pos(target).isLightSquare());
        }
    }

    // ---- Queen -----------------------------------------------------------------------------------------

    @Test
    void queenCombinesRookAndBishop() {
        assertEquals(27, pseudoTargets(boardOf("Qd4"), "d4").size());
    }

    @Test
    void queenIsBlockedAndCapturesLikeTheOthers() {
        Set<String> targets = pseudoTargets(boardOf("Qd4", "Pd5", "pe5", "Pc4"), "d4");
        assertFalse(targets.contains("d5"));
        assertFalse(targets.contains("d6"));
        assertTrue(targets.contains("e5"));
        assertFalse(targets.contains("f6"));
        assertFalse(targets.contains("c4"));
        assertFalse(targets.contains("b4"));
        assertTrue(targets.contains("e4"));
    }

    // ---- King ------------------------------------------------------------------------------------------

    @Test
    void kingMovesOneSquareInAnyDirection() {
        assertEquals(Set.of("c3", "c4", "c5", "d3", "d5", "e3", "e4", "e5"), pseudoTargets(boardOf("Kd4"), "d4"));
    }

    @Test
    void kingInTheCornerHasThreeMoves() {
        assertEquals(Set.of("a2", "b1", "b2"), pseudoTargets(boardOf("Ka1"), "a1"));
    }

    @Test
    void kingCannotStepOntoOwnPiecesButCanCaptureEnemies() {
        Set<String> targets = pseudoTargets(boardOf("Kd4", "Pd5", "pe5"), "d4");
        assertFalse(targets.contains("d5"));
        assertTrue(targets.contains("e5"));
        assertEquals(7, targets.size());
    }

    @Test
    void kingOffItsHomeSquareNeverGeneratesCastling() {
        assertTrue(pseudoMoves(boardOf("Ke2", "Ra1", "Rh1"), "e2").stream().noneMatch(m -> m.isCastling()));
    }

    // ---- Factory ---------------------------------------------------------------------------------------

    @Test
    void factoryHandsOutTheRuleOfEachPieceType() {
        assertTrue(MoveRuleFactory.forType(PieceType.KING) instanceof KingMoveRule);
        assertTrue(MoveRuleFactory.forType(PieceType.QUEEN) instanceof QueenMoveRule);
        assertTrue(MoveRuleFactory.forType(PieceType.ROOK) instanceof RookMoveRule);
        assertTrue(MoveRuleFactory.forType(PieceType.BISHOP) instanceof BishopMoveRule);
        assertTrue(MoveRuleFactory.forType(PieceType.KNIGHT) instanceof KnightMoveRule);
        assertTrue(MoveRuleFactory.forType(PieceType.PAWN) instanceof PawnMoveRule);
    }

    @Test
    void factoryReusesTheSameStatelessRules() {
        assertSame(MoveRuleFactory.forType(PieceType.ROOK), MoveRuleFactory.forType(PieceType.ROOK));
    }

    @Test
    void factoryCanChooseByPiece() {
        assertSame(MoveRuleFactory.forType(PieceType.KNIGHT), MoveRuleFactory.forPiece(new Knight(Team.BLACK)));
        assertSame(MoveRuleFactory.forType(PieceType.KING), MoveRuleFactory.forPiece(new King(Team.WHITE)));
        assertSame(MoveRuleFactory.forType(PieceType.BISHOP), MoveRuleFactory.forPiece(new Bishop(Team.WHITE)));
        assertSame(MoveRuleFactory.forType(PieceType.QUEEN), MoveRuleFactory.forPiece(new Queen(Team.WHITE)));
        assertSame(MoveRuleFactory.forType(PieceType.ROOK), MoveRuleFactory.forPiece(new Rook(Team.WHITE)));
        assertSame(MoveRuleFactory.forType(PieceType.PAWN), MoveRuleFactory.forPiece(new Pawn(Team.WHITE)));
    }
}
