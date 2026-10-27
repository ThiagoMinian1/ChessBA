package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.TestSupport.Recorder;
import ar.uade.tpo.chess.core.model.Team;
import ar.uade.tpo.chess.ports.GameObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.uade.tpo.chess.TestSupport.gameOf;
import static ar.uade.tpo.chess.TestSupport.play;
import static ar.uade.tpo.chess.TestSupport.pos;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObserverTest {

    private Game game;
    private Recorder recorder;

    @BeforeEach
    void setUp() {
        game = new Game();
        recorder = new Recorder();
        game.addObserver(recorder);
    }

    @Test
    void aQuietMoveIsReportedOnce() {
        play(game, "e2e4");
        assertEquals(List.of("move e2e4"), recorder.events);
    }

    @Test
    void aCaptureIsReportedAfterTheMove() {
        play(game, "e2e4", "d7d5", "e4d5");
        assertEquals(List.of("move e2e4", "move d7d5", "move e4d5", "capture PAWN@d5"), recorder.events);
    }

    @Test
    void castlingIsASingleMoveEventWithNoCapture() {
        Game castling = gameOf(Team.WHITE, "Ke1", "Rh1", "ke8");
        Recorder events = new Recorder();
        castling.addObserver(events);
        play(castling, "e1g1");
        assertEquals(List.of("move e1g1"), events.events);
    }

    @Test
    void anEnPassantCaptureIsReportedOnTheSquareOfTheCapturedPawn() {
        Game ep = gameOf(Team.BLACK, "Ke1", "ke8", "Pe5", "pd7");
        Recorder events = new Recorder();
        ep.addObserver(events);
        play(ep, "d7d5", "e5d6");
        assertEquals(List.of("move d7d5", "move e5d6", "capture PAWN@d5"), events.events);
    }

    @Test
    void aPromotionIsReported() {
        Game promo = gameOf(Team.WHITE, "Ka1", "kh6", "Pe7");
        Recorder events = new Recorder();
        promo.addObserver(events);
        play(promo, "e7e8q");
        assertEquals(List.of("move e7e8=Q", "promote e8=QUEEN"), events.events);
    }

    @Test
    void aPromotionByCaptureReportsMoveCaptureAndPromotionInThatOrder() {
        Game promo = gameOf(Team.WHITE, "Ka1", "kh6", "Pe7", "rd8");
        Recorder events = new Recorder();
        promo.addObserver(events);
        play(promo, "e7d8q");
        assertEquals(List.of("move e7d8=Q", "capture ROOK@d8", "promote d8=QUEEN"), events.events);
    }

    @Test
    void checkIsReportedForTheTeamInCheck() {
        play(game, "e2e4", "f7f6", "d1h5");
        assertEquals(List.of("move e2e4", "move f7f6", "move d1h5", "check BLACK"), recorder.events);
    }

    @Test
    void checkmateIsReportedAsTheEndOfTheGameNotAsACheck() {
        play(game, "f2f3", "e7e5", "g2g4", "d8h4");
        List<String> last = recorder.events.subList(recorder.events.size() - 2, recorder.events.size());
        assertEquals(List.of("move d8h4", "end CHECKMATE BLACK"), last);
        assertTrue(recorder.events.stream().noneMatch(e -> e.startsWith("check")));
    }

    @Test
    void stalemateIsReported() {
        Game stalemate = gameOf(Team.WHITE, "ka8", "Kb6", "Qc1");
        Recorder events = new Recorder();
        stalemate.addObserver(events);
        play(stalemate, "c1c7");
        assertEquals(List.of("move c1c7", "end STALEMATE -"), events.events);
    }

    @Test
    void resignationAndAgreedDrawAreReported() {
        game.resign(Team.WHITE);
        assertEquals(List.of("end RESIGNATION BLACK"), recorder.events);

        Game other = new Game();
        Recorder events = new Recorder();
        other.addObserver(events);
        other.agreeDraw();
        assertEquals(List.of("end DRAW_BY_AGREEMENT -"), events.events);
    }

    @Test
    void theEndOfTheGameIsReportedExactlyOnce() {
        play(game, "f2f3", "e7e5", "g2g4", "d8h4");
        assertThrows(IllegalMoveException.class, () -> game.play(pos("a2"), pos("a3")));
        assertEquals(1, recorder.events.stream().filter(e -> e.startsWith("end")).count());
    }

    @Test
    void rejectedMovesAreNotReported() {
        assertThrows(IllegalMoveException.class, () -> game.play(pos("e2"), pos("e5")));
        assertTrue(recorder.events.isEmpty());
    }

    @Test
    void undoAndRedoAreNotReported() {
        play(game, "e2e4");
        recorder.events.clear();
        game.undo();
        game.redo();
        assertTrue(recorder.events.isEmpty());
    }

    @Test
    void aRemovedObserverHearsNothingMore() {
        play(game, "e2e4");
        game.removeObserver(recorder);
        play(game, "e7e5");
        assertEquals(List.of("move e2e4"), recorder.events);
    }

    @Test
    void everyObserverIsNotified() {
        Recorder second = new Recorder();
        game.addObserver(second);
        play(game, "e2e4");
        assertEquals(List.of("move e2e4"), recorder.events);
        assertEquals(List.of("move e2e4"), second.events);
    }

    @Test
    void anObserverThatOverridesNothingIsHarmless() {
        game.addObserver(new GameObserver() { });
        play(game, "f2f3", "e7e5", "g2g4", "d8h4");
        assertEquals(Team.BLACK, game.winner().orElseThrow());
    }
}
