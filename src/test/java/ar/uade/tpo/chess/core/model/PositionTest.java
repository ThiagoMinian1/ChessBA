package ar.uade.tpo.chess.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PositionTest {

    @Test
    void parsesCornersAndMiddle() {
        assertEquals(new Position(0, 0), Position.of("a1"));
        assertEquals(new Position(7, 7), Position.of("h8"));
        assertEquals(new Position(4, 3), Position.of("e4"));
    }

    @Test
    void parsingIsCaseInsensitiveOnTheFile() {
        assertEquals(Position.of("e4"), Position.of("E4"));
    }

    @Test
    void rejectsMalformedSquares() {
        for (String bad : new String[] {"i1", "a9", "a0", "a", "abc", "", "11", "aa", null}) {
            assertThrows(IllegalArgumentException.class, () -> Position.of(bad), "should reject " + bad);
        }
    }

    @Test
    void rejectsCoordinatesOutsideTheBoard() {
        assertThrows(IllegalArgumentException.class, () -> new Position(8, 0));
        assertThrows(IllegalArgumentException.class, () -> new Position(0, 8));
        assertThrows(IllegalArgumentException.class, () -> new Position(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Position(0, -1));
    }

    @Test
    void toStringRoundTripsForAllSixtyFourSquares() {
        int count = 0;
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Position position = new Position(file, rank);
                assertEquals(position, Position.of(position.toString()));
                count++;
            }
        }
        assertEquals(64, count);
    }

    @Test
    void offsetStaysOnTheBoardOrIsEmpty() {
        assertEquals(Position.of("f5"), Position.of("e4").offset(1, 1).orElseThrow());
        assertTrue(Position.of("a1").offset(-1, 0).isEmpty());
        assertTrue(Position.of("h8").offset(0, 1).isEmpty());
        assertTrue(Position.of("a1").offset(0, -1).isEmpty());
    }

    @Test
    void tryOfReportsBounds() {
        assertTrue(Position.tryOf(3, 4).isPresent());
        assertTrue(Position.tryOf(8, 4).isEmpty());
        assertTrue(Position.tryOf(3, -1).isEmpty());
    }

    @Test
    void squareColoursFollowTheChessboard() {
        assertFalse(Position.of("a1").isLightSquare());
        assertTrue(Position.of("h1").isLightSquare());
        assertTrue(Position.of("a8").isLightSquare());
        assertFalse(Position.of("h8").isLightSquare());
        assertTrue(Position.of("d4").isLightSquare() == Position.of("e5").isLightSquare());
        assertFalse(Position.of("d4").isLightSquare() == Position.of("d5").isLightSquare());
    }
}
