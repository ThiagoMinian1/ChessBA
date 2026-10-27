package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.King;
import ar.uade.tpo.chess.core.model.Pawn;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import static ar.uade.tpo.chess.TestSupport.pos;
import static ar.uade.tpo.chess.TestSupport.stateOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameStateTest {

    @Test
    void theInitialStateIsTheStandardPositionWithWhiteToMove() {
        GameState state = GameState.initial();
        assertEquals(Board.standard(), state.board());
        assertEquals(Team.WHITE, state.turn());
        assertEquals(GameStatus.ONGOING, state.status());
        assertEquals(0, state.halfmoveClock());
        assertTrue(state.enPassantTarget().isEmpty());
        assertTrue(state.winner().isEmpty());
        assertTrue(state.lastMove().isEmpty());
    }

    @Test
    void ofCopiesTheBoardItIsGiven() {
        Board board = Board.standard();
        GameState state = GameState.of(board, Team.BLACK);
        board.remove(pos("e2"));
        assertTrue(state.board().pieceAt(pos("e2")).isPresent());
        assertEquals(Team.BLACK, state.turn());
    }

    @Test
    void aCopyIsFullyIndependent() {
        GameState original = GameState.initial();
        original.putExtension("k", "v");
        original.bench(Team.WHITE).add(new Pawn(Team.WHITE));
        GameState copy = original.copy();

        copy.board().remove(pos("e2"));
        copy.setTurn(Team.BLACK);
        copy.setHalfmoveClock(9);
        copy.setEnPassantTarget(pos("e3"));
        copy.bench(Team.WHITE).clear();
        copy.putExtension("k", "changed");

        assertTrue(original.board().pieceAt(pos("e2")).isPresent());
        assertEquals(Team.WHITE, original.turn());
        assertEquals(0, original.halfmoveClock());
        assertTrue(original.enPassantTarget().isEmpty());
        assertEquals(1, original.bench(Team.WHITE).size());
        assertEquals("v", original.extension("k", String.class).orElseThrow());
    }

    @Test
    void aMementoFreezesTheStateAtTheMomentOfSaving() {
        GameState state = GameState.initial();
        GameMemento memento = state.save();
        state.board().remove(pos("e2"));
        state.setHalfmoveClock(5);
        state.finish(GameStatus.DRAW_BY_AGREEMENT, null);

        GameState restored = memento.restore();
        assertEquals(Board.standard(), restored.board());
        assertEquals(0, restored.halfmoveClock());
        assertEquals(GameStatus.ONGOING, restored.status());
    }

    @Test
    void everyRestoreYieldsAFreshCopy() {
        GameMemento memento = GameState.initial().save();
        GameState first = memento.restore();
        GameState second = memento.restore();
        assertNotSame(first, second);
        first.board().remove(pos("e2"));
        assertTrue(second.board().pieceAt(pos("e2")).isPresent());
        assertTrue(memento.restore().board().pieceAt(pos("e2")).isPresent());
    }

    @Test
    void extensionsAreTypedAndMissingKeysAreEmpty() {
        GameState state = GameState.initial();
        state.putExtension("number", 7);
        assertEquals(Integer.valueOf(7), state.extension("number", Integer.class).orElseThrow());
        assertTrue(state.extension("number", String.class).isEmpty());
        assertTrue(state.extension("absent", Integer.class).isEmpty());
        assertThrows(NullPointerException.class, () -> state.putExtension("k", null));
        assertThrows(NullPointerException.class, () -> state.putExtension(null, "v"));
    }

    @Test
    void invalidValuesAreRejected() {
        GameState state = GameState.initial();
        assertThrows(IllegalArgumentException.class, () -> state.setHalfmoveClock(-1));
        assertThrows(IllegalArgumentException.class, () -> state.finish(GameStatus.ONGOING, null));
    }

    @Test
    void positionKeyIncludesCastlingRightsOnlyWhileTheyHold() {
        GameState state = stateOf(Team.WHITE, "Ke1", "Ra1", "Rh1", "ke8", "ra8", "rh8");
        assertTrue(state.positionKey().contains(" KQkq "));
        state.board().place(pos("e1"), new King(Team.WHITE, true));
        assertTrue(state.positionKey().contains(" kq "));
    }

    @Test
    void positionKeyMentionsEnPassantOnlyWhenTheCaptureIsPossible() {
        GameState possible = stateOf(Team.WHITE, "Ke1", "ke8", "Pe5", "pd5");
        possible.setEnPassantTarget(pos("d6"));
        assertTrue(possible.positionKey().endsWith(" d6"));

        GameState impossible = stateOf(Team.WHITE, "Ke1", "ke8", "Pa5", "pd5");
        impossible.setEnPassantTarget(pos("d6"));
        assertTrue(impossible.positionKey().endsWith(" -"));
    }

    @Test
    void positionKeyDistinguishesTheSideToMove() {
        GameState white = stateOf(Team.WHITE, "Ke1", "ke8", "Pa2");
        GameState black = stateOf(Team.BLACK, "Ke1", "ke8", "Pa2");
        assertTrue(!white.positionKey().equals(black.positionKey()));
    }

    @Test
    void recordingPositionsCountsOccurrences() {
        GameState state = stateOf(Team.WHITE, "Ke1", "ke8", "Pa2");
        assertEquals(1, state.recordCurrentPosition());
        assertEquals(2, state.recordCurrentPosition());
        state.clearPositionHistory();
        assertEquals(1, state.recordCurrentPosition());
    }

    @Test
    void theHistoryOfPositionsTravelsWithACopy() {
        GameState state = stateOf(Team.WHITE, "Ke1", "ke8", "Pa2");
        state.recordCurrentPosition();
        GameState copy = state.copy();
        assertEquals(2, copy.recordCurrentPosition());
        assertEquals(2, state.recordCurrentPosition(), "the original keeps its own count");
    }
}
