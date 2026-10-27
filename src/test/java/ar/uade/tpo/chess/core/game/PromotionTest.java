package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static ar.uade.tpo.chess.TestSupport.gameOf;
import static ar.uade.tpo.chess.TestSupport.pos;
import static ar.uade.tpo.chess.TestSupport.targets;
import static ar.uade.tpo.chess.TestSupport.targetsFrom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromotionTest {

    private static Game whitePawnOnSeventh() {
        // The extra pawn keeps the game going when the pawn promotes to a lone knight or bishop.
        return gameOf(Team.WHITE, "Ka1", "kh6", "Pe7", "Pa3");
    }

    @Test
    void aPawnCanPromoteToEachOfTheFourPieces() {
        for (PieceType choice : new PieceType[] {
            PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT}) {
            Game game = whitePawnOnSeventh();
            game.play(pos("e7"), pos("e8"), choice);
            Piece promoted = game.board().pieceAt(pos("e8")).orElseThrow();
            assertEquals(choice, promoted.type());
            assertEquals(Team.WHITE, promoted.team());
            assertTrue(game.board().isEmpty(pos("e7")));
            assertEquals(Team.BLACK, game.currentTurn());
        }
    }

    @Test
    void blackPawnsPromoteOnTheFirstRank() {
        Game game = gameOf(Team.BLACK, "Ka8", "kh6", "pe2");
        game.play(pos("e2"), pos("e1"), PieceType.ROOK);
        Piece promoted = game.board().pieceAt(pos("e1")).orElseThrow();
        assertEquals(PieceType.ROOK, promoted.type());
        assertEquals(Team.BLACK, promoted.team());
    }

    @Test
    void thePromotionPieceMustBeChosen() {
        Game game = whitePawnOnSeventh();
        assertTrue(game.requiresPromotion(pos("e7"), pos("e8")));
        assertThrows(PromotionRequiredException.class, () -> game.play(pos("e7"), pos("e8")));
        assertEquals(Team.WHITE, game.currentTurn(), "a rejected move does not consume the turn");
        assertEquals(PieceType.PAWN, game.board().pieceAt(pos("e7")).orElseThrow().type());
    }

    @Test
    void aPawnCannotPromoteToAKingOrToAPawn() {
        Game game = whitePawnOnSeventh();
        assertThrows(IllegalMoveException.class, () -> game.play(pos("e7"), pos("e8"), PieceType.KING));
        assertThrows(IllegalMoveException.class, () -> game.play(pos("e7"), pos("e8"), PieceType.PAWN));
        assertEquals(PieceType.PAWN, game.board().pieceAt(pos("e7")).orElseThrow().type());
        assertThrows(IllegalArgumentException.class, () -> Move.promotion(pos("e7"), pos("e8"), PieceType.KING));
        assertThrows(IllegalArgumentException.class, () -> Move.promotion(pos("e7"), pos("e8"), PieceType.PAWN));
    }

    @Test
    void theGameOffersExactlyFourPromotionMovesPerDestination() {
        Game game = whitePawnOnSeventh();
        List<Move> moves = game.legalMovesFrom(pos("e7"));
        assertEquals(4, moves.size());
        assertTrue(moves.stream().allMatch(Move::isPromotion));
        assertEquals(Set.of("e8"), targets(moves));
    }

    @Test
    void promotionByCapture() {
        Game game = gameOf(Team.WHITE, "Ka1", "kh6", "Pe7", "rd8");
        assertEquals(8, game.legalMovesFrom(pos("e7")).size());
        game.play(pos("e7"), pos("d8"), PieceType.ROOK);
        assertEquals(PieceType.ROOK, game.board().pieceAt(pos("d8")).orElseThrow().type());
        assertEquals(Team.WHITE, game.board().pieceAt(pos("d8")).orElseThrow().team());
        assertEquals(1, game.bench(Team.BLACK).size());
        assertEquals(PieceType.ROOK, game.bench(Team.BLACK).get(0).type());
    }

    @Test
    void aPromotedPieceThatIsCapturedGoesToTheBenchAsItsPromotedType() {
        Game game = gameOf(Team.WHITE, "Ka1", "kh6", "Pe7", "Pa3", "rh8");
        game.play(pos("e7"), pos("e8"), PieceType.QUEEN);
        game.play(pos("h8"), pos("e8"));
        assertEquals(1, game.bench(Team.WHITE).size());
        assertEquals(PieceType.QUEEN, game.bench(Team.WHITE).get(0).type(), "it is a queen now, not a pawn");
    }

    @Test
    void theNewPieceMovesLikeItsNewType() {
        Game game = whitePawnOnSeventh();
        game.play(pos("e7"), pos("e8"), PieceType.KNIGHT);
        game.play(pos("h6"), pos("h5"));
        assertEquals(Set.of("c7", "d6", "f6", "g7"), targetsFrom(game, "e8"));
    }

    @Test
    void anOrdinaryMoveCannotCarryAPromotionPiece() {
        Game game = new Game();
        assertThrows(IllegalMoveException.class, () -> game.play(pos("e2"), pos("e4"), PieceType.QUEEN));
        assertEquals(Team.WHITE, game.currentTurn());
    }

    @Test
    void aMovePastTheLastRankIsNotARequirementForOrdinaryMoves() {
        Game game = new Game();
        assertTrue(!game.requiresPromotion(pos("e2"), pos("e4")));
    }
}
