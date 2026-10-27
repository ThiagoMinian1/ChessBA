package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.MoveType;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static ar.uade.tpo.chess.TestSupport.gameOf;
import static ar.uade.tpo.chess.TestSupport.play;
import static ar.uade.tpo.chess.TestSupport.pos;
import static ar.uade.tpo.chess.TestSupport.targetsFrom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnPassantTest {

    @Test
    void whiteCapturesEnPassantRightAfterABlackDoublePush() {
        Game game = gameOf(Team.BLACK, "Ke1", "ke8", "Pe5", "pd7");
        game.play(pos("d7"), pos("d5"));
        assertEquals(Set.of("d6", "e6"), targetsFrom(game, "e5"));
        Move capture = game.legalMovesFrom(pos("e5")).stream()
                .filter(m -> m.to().equals(pos("d6"))).findFirst().orElseThrow();
        assertEquals(MoveType.EN_PASSANT, capture.type());

        game.play(pos("e5"), pos("d6"));
        assertEquals(PieceType.PAWN, game.board().pieceAt(pos("d6")).orElseThrow().type());
        assertEquals(Team.WHITE, game.board().pieceAt(pos("d6")).orElseThrow().team());
        assertTrue(game.board().isEmpty(pos("d5")), "the captured pawn is removed from d5, not from d6");
        assertTrue(game.board().isEmpty(pos("e5")));
        assertEquals(1, game.bench(Team.BLACK).size());
        assertEquals(PieceType.PAWN, game.bench(Team.BLACK).get(0).type());
    }

    @Test
    void blackCapturesEnPassantAfterAWhiteDoublePush() {
        Game game = gameOf(Team.WHITE, "Ke1", "ke8", "Pe2", "pd4");
        game.play(pos("e2"), pos("e4"));
        assertEquals(Set.of("d3", "e3"), targetsFrom(game, "d4"));
        game.play(pos("d4"), pos("e3"));
        assertEquals(Team.BLACK, game.board().pieceAt(pos("e3")).orElseThrow().team());
        assertTrue(game.board().isEmpty(pos("e4")));
        assertEquals(1, game.bench(Team.WHITE).size());
    }

    @Test
    void theWindowClosesAfterOneMove() {
        Game game = gameOf(Team.BLACK, "Ke1", "ke8", "Pe5", "pd7");
        game.play(pos("d7"), pos("d5"));
        assertTrue(targetsFrom(game, "e5").contains("d6"));

        play(game, "e1e2", "e8e7");
        assertEquals(Set.of("e6"), targetsFrom(game, "e5"));
        assertThrows(IllegalMoveException.class, () -> game.play(pos("e5"), pos("d6")));
    }

    @Test
    void noEnPassantAfterASingleStep() {
        Game game = gameOf(Team.BLACK, "Ke1", "ke8", "Pe5", "pd7");
        play(game, "d7d6");
        // d6 is now simply occupied; there was no double push, so only an ordinary capture on d6 remains.
        Move capture = game.legalMovesFrom(pos("e5")).stream()
                .filter(m -> m.to().equals(pos("d6"))).findFirst().orElseThrow();
        assertEquals(MoveType.NORMAL, capture.type());
    }

    @Test
    void noEnPassantWhenThePawnWasAlreadyThereWithoutDoublePushing() {
        Game game = gameOf(Team.WHITE, "Ke1", "ke8", "Pe5", "pd5");
        assertEquals(Set.of("e6"), targetsFrom(game, "e5"));
        assertThrows(IllegalMoveException.class, () -> game.play(pos("e5"), pos("d6")));
    }

    @Test
    void onlyAPawnMayCaptureEnPassant() {
        Game game = gameOf(Team.BLACK, "Ke1", "ka8", "Re5", "pd7");
        game.play(pos("d7"), pos("d5"));
        assertTrue(!targetsFrom(game, "e5").contains("d6"));
        assertTrue(targetsFrom(game, "e5").contains("d5"), "the rook may take the pawn the ordinary way");
    }

    @Test
    void anEnPassantCaptureThatExposesTheKingIsIllegal() {
        // After ...d5, taking en passant would remove both pawns from the fifth rank and open it to the rook.
        Game game = gameOf(Team.BLACK, "Ka5", "Pe5", "ke8", "pd7", "rh5");
        game.play(pos("d7"), pos("d5"));
        assertEquals(Set.of("e6"), targetsFrom(game, "e5"));
        assertThrows(IllegalMoveException.class, () -> game.play(pos("e5"), pos("d6")));
    }

    @Test
    void enPassantCanAnswerACheckByCapturingTheCheckingPawn() {
        // The black pawn that just double-pushed gives check to the king on e4; capturing it en passant resolves it.
        Game game = gameOf(Team.BLACK, "Ke4", "Pe5", "ke8", "pd7");
        // d7-d5 attacks e4, putting the white king in check; the d5 pawn can be taken en passant by e5.
        game.play(pos("d7"), pos("d5"));
        assertTrue(game.isInCheck());
        assertTrue(targetsFrom(game, "e5").contains("d6"));
        game.play(pos("e5"), pos("d6"));
        assertTrue(!game.isInCheck());
    }

    @Test
    void enPassantResetsTheFiftyMoveCounter() {
        Game game = gameOf(Team.BLACK, "Ke1", "ke8", "Pe5", "pd7");
        play(game, "d7d5", "e5d6");
        assertEquals(0, game.halfmoveClock());
    }
}
