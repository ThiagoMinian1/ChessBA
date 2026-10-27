package ar.uade.tpo.chess.adapters.console;

import ar.uade.tpo.chess.core.model.Board;
import org.junit.jupiter.api.Test;

import static ar.uade.tpo.chess.TestSupport.boardOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsoleBoardRendererTest {

    private final ConsoleBoardRenderer renderer = new ConsoleBoardRenderer();

    @Test
    void drawsTheStartingPositionFromWhitesSide() {
        String expected = String.join("\n",
                "8  r n b q k b n r",
                "7  p p p p p p p p",
                "6  . . . . . . . .",
                "5  . . . . . . . .",
                "4  . . . . . . . .",
                "3  . . . . . . . .",
                "2  P P P P P P P P",
                "1  R N B Q K B N R",
                "",
                "   a b c d e f g h");
        assertEquals(expected, renderer.render(Board.standard()));
    }

    @Test
    void drawsAnArbitraryPosition() {
        String expected = String.join("\n",
                "8  . . . . k . . .",
                "7  . . . . . . . .",
                "6  . . . . . . . .",
                "5  . . . . . . . .",
                "4  . . . . . . . .",
                "3  . . . . . . . .",
                "2  . . . . . . . .",
                "1  R . . . K . . .",
                "",
                "   a b c d e f g h");
        assertEquals(expected, renderer.render(boardOf("Ra1", "Ke1", "ke8")));
    }
}
