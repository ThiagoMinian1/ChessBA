package ar.uade.tpo.chess.adapters.console;

import ar.uade.tpo.chess.core.model.PieceType;
import org.junit.jupiter.api.Test;

import static ar.uade.tpo.chess.TestSupport.pos;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandParserTest {

    private static void assertInvalid(String line) {
        assertTrue(CommandParser.parse(line) instanceof Command.Invalid, "should be invalid: '" + line + "'");
    }

    @Test
    void parsesMovesWithOrWithoutASpace() {
        Command expected = new Command.MoveCommand(pos("e2"), pos("e4"), null);
        assertEquals(expected, CommandParser.parse("e2 e4"));
        assertEquals(expected, CommandParser.parse("e2e4"));
        assertEquals(expected, CommandParser.parse("E2 E4"));
        assertEquals(expected, CommandParser.parse("  e2    e4  "));
    }

    @Test
    void parsesAPromotionPieceInEveryAcceptedForm() {
        Command expected = new Command.MoveCommand(pos("e7"), pos("e8"), PieceType.QUEEN);
        assertEquals(expected, CommandParser.parse("e7 e8 q"));
        assertEquals(expected, CommandParser.parse("e7e8q"));
        assertEquals(expected, CommandParser.parse("e7e8=Q"));
        assertEquals(new Command.MoveCommand(pos("a7"), pos("a8"), PieceType.KNIGHT), CommandParser.parse("a7 a8 n"));
    }

    @Test
    void aPromotionPieceMustBeQueenRookBishopOrKnight() {
        assertInvalid("e7 e8 k");
        assertInvalid("e7 e8 p");
        assertInvalid("e7 e8 x");
    }

    @Test
    void parsesTheSimpleCommands() {
        assertTrue(CommandParser.parse("undo") instanceof Command.Undo);
        assertTrue(CommandParser.parse("redo") instanceof Command.Redo);
        assertTrue(CommandParser.parse("bench") instanceof Command.Bench);
        assertTrue(CommandParser.parse("resign") instanceof Command.Resign);
        assertTrue(CommandParser.parse("draw") instanceof Command.Draw);
        assertTrue(CommandParser.parse("help") instanceof Command.Help);
        assertTrue(CommandParser.parse("?") instanceof Command.Help);
        assertTrue(CommandParser.parse("quit") instanceof Command.Quit);
        assertTrue(CommandParser.parse("exit") instanceof Command.Quit);
    }

    @Test
    void commandsAreCaseInsensitive() {
        assertTrue(CommandParser.parse("UNDO") instanceof Command.Undo);
        assertTrue(CommandParser.parse("Quit") instanceof Command.Quit);
    }

    @Test
    void parsesATrade() {
        assertEquals(new Command.Trade(pos("e4"), PieceType.QUEEN), CommandParser.parse("trade e4 q"));
        assertEquals(new Command.Trade(pos("a2"), PieceType.PAWN), CommandParser.parse("TRADE A2 P"));
    }

    @Test
    void rejectsMalformedTrades() {
        assertInvalid("trade");
        assertInvalid("trade e4");
        assertInvalid("trade z9 q");
        assertInvalid("trade e4 x");
        assertInvalid("trade e4 queen");
        assertInvalid("trade e4 q now");
    }

    @Test
    void rejectsCommandsWithUnexpectedArguments() {
        assertInvalid("undo now");
        assertInvalid("redo 2");
        assertInvalid("bench all");
        assertInvalid("resign please");
        assertInvalid("draw now");
    }

    @Test
    void rejectsNonsense() {
        assertInvalid(null);
        assertInvalid("");
        assertInvalid("   ");
        assertInvalid("hello");
        assertInvalid("e2");
        assertInvalid("e2 e9");
        assertInvalid("i2 i4");
        assertInvalid("a1 a2 a3 b4");
    }

    @Test
    void invalidCommandsExplainWhatWentWrong() {
        Command.Invalid invalid = (Command.Invalid) CommandParser.parse("hello");
        assertTrue(invalid.reason().contains("hello"));
        assertTrue(((Command.Invalid) CommandParser.parse("")).reason().contains("help"));
    }
}
