package ar.uade.tpo.chess.core.model;

import org.junit.jupiter.api.Test;

import static ar.uade.tpo.chess.TestSupport.pos;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PieceTest {

    @Test
    void everyPieceReportsItsTypeAndTeam() {
        assertEquals(PieceType.PAWN, new Pawn(Team.WHITE).type());
        assertEquals(PieceType.ROOK, new Rook(Team.WHITE).type());
        assertEquals(PieceType.KNIGHT, new Knight(Team.BLACK).type());
        assertEquals(PieceType.BISHOP, new Bishop(Team.BLACK).type());
        assertEquals(PieceType.QUEEN, new Queen(Team.WHITE).type());
        assertEquals(PieceType.KING, new King(Team.BLACK).type());
        assertEquals(Team.BLACK, new King(Team.BLACK).team());
    }

    @Test
    void materialValuesAreTheConventionalOnes() {
        assertEquals(1, new Pawn(Team.WHITE).materialValue());
        assertEquals(3, new Knight(Team.WHITE).materialValue());
        assertEquals(3, new Bishop(Team.WHITE).materialValue());
        assertEquals(5, new Rook(Team.WHITE).materialValue());
        assertEquals(9, new Queen(Team.WHITE).materialValue());
        assertEquals(0, new King(Team.WHITE).materialValue());
    }

    @Test
    void piecesStartUnmovedAndWithMovedReturnsACopy() {
        Piece rook = new Rook(Team.WHITE);
        assertFalse(rook.hasMoved());
        Piece moved = rook.withMoved(true);
        assertTrue(moved.hasMoved());
        assertFalse(rook.hasMoved());
        assertEquals(PieceType.ROOK, moved.type());
        assertEquals(Team.WHITE, moved.team());
    }

    @Test
    void withMovedKeepsTheConcreteClassForEveryType() {
        for (PieceType type : PieceType.values()) {
            Piece piece = PieceFactory.create(type, Team.BLACK);
            assertEquals(piece.getClass(), piece.withMoved(true).getClass());
        }
    }

    @Test
    void equalityConsidersTeamTypeAndMovedFlag() {
        assertEquals(new Pawn(Team.WHITE), new Pawn(Team.WHITE));
        assertEquals(new Pawn(Team.WHITE).hashCode(), new Pawn(Team.WHITE).hashCode());
        assertNotEquals(new Pawn(Team.WHITE), new Pawn(Team.BLACK));
        assertNotEquals(new Pawn(Team.WHITE), new Rook(Team.WHITE));
        assertNotEquals(new Pawn(Team.WHITE), new Pawn(Team.WHITE, true));
    }

    @Test
    void symbolIsUppercaseForWhiteAndLowercaseForBlack() {
        assertEquals('K', new King(Team.WHITE).symbol());
        assertEquals('k', new King(Team.BLACK).symbol());
        assertEquals('N', new Knight(Team.WHITE).symbol());
        assertEquals('p', new Pawn(Team.BLACK).symbol());
    }

    @Test
    void factoryCreatesTheMatchingClass() {
        assertTrue(PieceFactory.create(PieceType.KING, Team.WHITE) instanceof King);
        assertTrue(PieceFactory.create(PieceType.QUEEN, Team.WHITE) instanceof Queen);
        assertTrue(PieceFactory.create(PieceType.ROOK, Team.WHITE) instanceof Rook);
        assertTrue(PieceFactory.create(PieceType.BISHOP, Team.WHITE) instanceof Bishop);
        assertTrue(PieceFactory.create(PieceType.KNIGHT, Team.WHITE) instanceof Knight);
        assertTrue(PieceFactory.create(PieceType.PAWN, Team.WHITE) instanceof Pawn);
        assertTrue(PieceFactory.create(PieceType.PAWN, Team.WHITE, true).hasMoved());
    }

    @Test
    void pieceTypeLettersAndPromotionTargets() {
        assertEquals(PieceType.KNIGHT, PieceType.fromLetter('n').orElseThrow());
        assertEquals(PieceType.QUEEN, PieceType.fromLetter('Q').orElseThrow());
        assertTrue(PieceType.fromLetter('x').isEmpty());
        assertTrue(PieceType.QUEEN.isPromotionTarget());
        assertTrue(PieceType.ROOK.isPromotionTarget());
        assertTrue(PieceType.BISHOP.isPromotionTarget());
        assertTrue(PieceType.KNIGHT.isPromotionTarget());
        assertFalse(PieceType.KING.isPromotionTarget());
        assertFalse(PieceType.PAWN.isPromotionTarget());
    }

    @Test
    void teamGeometryIsMirrored() {
        assertEquals(Team.BLACK, Team.WHITE.opposite());
        assertEquals(Team.WHITE, Team.BLACK.opposite());
        assertEquals(0, Team.WHITE.homeRank());
        assertEquals(7, Team.BLACK.homeRank());
        assertEquals(1, Team.WHITE.pawnStartRank());
        assertEquals(6, Team.BLACK.pawnStartRank());
        assertEquals(7, Team.WHITE.promotionRank());
        assertEquals(0, Team.BLACK.promotionRank());
        assertEquals(1, Team.WHITE.forward());
        assertEquals(-1, Team.BLACK.forward());
    }

    @Test
    void movesCarryAPromotionPieceOnlyWhenPromoting() {
        assertThrows(IllegalArgumentException.class,
                () -> new Move(pos("e7"), pos("e8"), MoveType.PROMOTION, null));
        assertThrows(IllegalArgumentException.class, () -> Move.promotion(pos("e7"), pos("e8"), PieceType.KING));
        assertThrows(IllegalArgumentException.class, () -> Move.promotion(pos("e7"), pos("e8"), PieceType.PAWN));
        assertThrows(IllegalArgumentException.class,
                () -> new Move(pos("e2"), pos("e4"), MoveType.NORMAL, PieceType.QUEEN));
        assertDoesNotThrowFor(Move.promotion(pos("e7"), pos("e8"), PieceType.KNIGHT));
    }

    private static void assertDoesNotThrowFor(Move move) {
        assertTrue(move.isPromotion());
        assertEquals(PieceType.KNIGHT, move.promotion());
    }

    @Test
    void moveToStringUsesCoordinates() {
        assertEquals("e2e4", Move.normal(pos("e2"), pos("e4")).toString());
        assertEquals("e7e8=Q", Move.promotion(pos("e7"), pos("e8"), PieceType.QUEEN).toString());
        assertTrue(Move.castleKingside(pos("e1"), pos("g1")).isCastling());
        assertTrue(Move.castleQueenside(pos("e1"), pos("c1")).isCastling());
        assertFalse(Move.normal(pos("e2"), pos("e4")).isCastling());
        assertFalse(Move.enPassant(pos("e5"), pos("d6")).isPromotion());
    }

    @Test
    void gameStatusKnowsWhichResultsEndTheGameAndWhichAreDraws() {
        assertFalse(GameStatus.ONGOING.isOver());
        for (GameStatus status : GameStatus.values()) {
            if (status != GameStatus.ONGOING) {
                assertTrue(status.isOver());
            }
        }
        assertTrue(GameStatus.STALEMATE.isDraw());
        assertTrue(GameStatus.DRAW_BY_AGREEMENT.isDraw());
        assertTrue(GameStatus.DRAW_FIFTY_MOVE_RULE.isDraw());
        assertTrue(GameStatus.DRAW_THREEFOLD_REPETITION.isDraw());
        assertTrue(GameStatus.DRAW_INSUFFICIENT_MATERIAL.isDraw());
        assertFalse(GameStatus.CHECKMATE.isDraw());
        assertFalse(GameStatus.RESIGNATION.isDraw());
        assertFalse(GameStatus.ONGOING.isDraw());
    }
}
