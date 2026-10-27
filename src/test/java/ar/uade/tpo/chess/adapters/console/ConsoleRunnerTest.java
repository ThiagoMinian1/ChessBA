package ar.uade.tpo.chess.adapters.console;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Drives the whole console flow with scripted input, exactly as a player would type it. */
class ConsoleRunnerTest {

    private static String run(String... lines) {
        return runRaw(String.join("\n", lines) + "\n");
    }

    private static String runRaw(String input) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        BufferedReader in = new BufferedReader(new StringReader(input));
        new ConsoleRunner(in, out).run();
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static void assertContains(String output, String expected) {
        assertTrue(output.contains(expected), "expected output to contain: " + expected + "\n--- output ---\n" + output);
    }

    // ---- A full game -----------------------------------------------------------------------------------

    @Test
    void aWholeStandardGameCanBePlayedToCheckmate() {
        String output = run("1", "f2 f3", "e7 e5", "g2 g4", "d8 h4", "quit");
        assertContains(output, "White Pawn moves f2 -> f3");
        assertContains(output, "Black Queen moves d8 -> h4");
        assertContains(output, "Checkmate! Black wins");
        assertContains(output, "Bye!");
    }

    @Test
    void movesCanBeTypedWithoutASpace() {
        String output = run("1", "e2e4", "quit");
        assertContains(output, "White Pawn moves e2 -> e4");
    }

    @Test
    void illegalMovesAndUnknownCommandsAreExplainedAndPlayContinues() {
        String output = run("1", "e2 e5", "xyz", "e2 e4", "quit");
        assertContains(output, "Not allowed");
        assertContains(output, "Unrecognized command");
        assertContains(output, "White Pawn moves e2 -> e4");
    }

    @Test
    void thePositionAndWhoseTurnItIsAreShown() {
        String output = run("1", "quit");
        assertContains(output, "8  r n b q k b n r");
        assertContains(output, "   a b c d e f g h");
        assertContains(output, "White (white) >");
    }

    @Test
    void aLegendSaysWhichCaseBelongsToWhichSide() {
        assertContains(run("1", "quit"), "UPPERCASE = White  |  lowercase = Black");
        assertContains(run("4", "lakers", "celtics", "quit"),
                "UPPERCASE = White (Los Angeles Lakers)  |  lowercase = Black (Boston Celtics)");
    }

    @Test
    void checkIsAnnounced() {
        String output = run("1", "e2 e4", "f7 f6", "d1 h5", "quit");
        assertContains(output, "Check! Black is in check.");
    }

    // ---- Undo, redo, resign, draw ----------------------------------------------------------------------

    @Test
    void undoAndRedoWork() {
        String output = run("1", "e2 e4", "undo", "undo", "redo", "redo", "quit");
        assertContains(output, "Last action undone.");
        assertContains(output, "Nothing to undo.");
        assertContains(output, "Action redone.");
        assertContains(output, "Nothing to redo.");
    }

    @Test
    void resigning() {
        String output = run("1", "resign", "quit");
        assertContains(output, "Resignation: Black wins");
    }

    @Test
    void aDrawOfferCanBeDeclinedOrAccepted() {
        String output = run("1", "draw", "n", "draw", "y", "quit");
        assertContains(output, "Draw declined.");
        assertContains(output, "Draw by agreement");
    }

    @Test
    void afterTheGameEndsOnlyUndoOrQuitMakeSense() {
        String output = run("1", "f2 f3", "e7 e5", "g2 g4", "d8 h4", "a2 a3", "undo", "quit");
        assertContains(output, "The game is over");
        assertContains(output, "Not allowed");
        assertContains(output, "Last action undone.");
    }

    // ---- Promotion -------------------------------------------------------------------------------------

    private static final String[] PROMOTION_SETUP = {
        "1", "h2 h4", "g7 g5", "h4 g5", "h7 h6", "g5 h6", "a7 a6", "h6 h7", "a6 a5"
    };

    private static String[] withPromotionSetup(String... rest) {
        String[] all = new String[PROMOTION_SETUP.length + rest.length];
        System.arraycopy(PROMOTION_SETUP, 0, all, 0, PROMOTION_SETUP.length);
        System.arraycopy(rest, 0, all, PROMOTION_SETUP.length, rest.length);
        return all;
    }

    @Test
    void thePlayerIsAskedWhichPieceToPromoteToAndBadAnswersAreRepeated() {
        String output = run(withPromotionSetup("h7 g8", "k", "x", "n", "bench", "quit"));
        assertContains(output, "Promote to (Q/R/B/N):");
        assertContains(output, "Choose Q (queen), R (rook), B (bishop) or N (knight).");
        assertContains(output, "Promotion on g8: the pawn becomes White Knight");
        assertContains(output, "N = Black Knight");
    }

    @Test
    void thePromotionPieceCanBeGivenUpFront() {
        String output = run(withPromotionSetup("h7 g8 q", "quit"));
        assertFalse(output.contains("Promote to (Q/R/B/N):"));
        assertContains(output, "the pawn becomes White Queen");
    }

    @Test
    void aPromotionCanBeUndoneAndRedone() {
        String output = run(withPromotionSetup("h7 g8", "r", "undo", "redo", "quit"));
        assertContains(output, "the pawn becomes White Rook");
        assertContains(output, "Last action undone.");
        assertContains(output, "Action redone.");
    }

    // ---- Setup -----------------------------------------------------------------------------------------

    @Test
    void anInvalidModeIsAskedAgain() {
        String output = run("9", "abc", "1", "quit");
        assertContains(output, "Enter a number between 1 and 4.");
        assertContains(output, "Bye!");
    }

    @Test
    void endingTheInputDuringSetupIsHandledGracefully() {
        assertDoesNotThrow(() -> runRaw(""));
        assertDoesNotThrow(() -> runRaw("2\n"));
        assertDoesNotThrow(() -> runRaw("4\nlakers\n"));
    }

    @Test
    void endingTheInputDuringTheGameIsHandledGracefully() {
        assertDoesNotThrow(() -> runRaw("1\ne2 e4\n"));
    }

    @Test
    void theNbaThemeNamesPiecesAfterTheChosenFranchises() {
        String output = run("4", "lakers", "celtics", "e2 e4", "d7 d5", "quit");
        assertContains(output, "Lakers Rookie moves e2 -> e4");
        assertContains(output, "Celtics Rookie moves d7 -> d5");
        assertContains(output, "Los Angeles Lakers (white) >");
    }

    @Test
    void franchisesCanBePickedByNumberNameOrCode() {
        String output = run("4", "14", "BOS", "e2 e4", "quit");
        assertContains(output, "Lakers Rookie moves e2 -> e4");
    }

    @Test
    void bothPlayersCannotPickTheSameFranchise() {
        String output = run("4", "lakers", "lakers", "nonsense", "celtics", "e2 e4", "quit");
        assertContains(output, "Los Angeles Lakers is already taken.");
        assertContains(output, "Not a franchise.");
        assertContains(output, "Lakers Rookie moves e2 -> e4");
    }

    // ---- Mechanics through the console -----------------------------------------------------------------

    @Test
    void allMechanicsModeShowsTheShotClockAndAllowsTheTrade() {
        String output = run("2", "", "lakers", "celtics", "e2 e4", "d7 d5", "e4 d5", "trade c7 p", "bench", "quit");
        assertContains(output, "Shot clock (moves without progress / limit):");
        assertContains(output, "/10");
        assertContains(output, "Boston Celtics trades PAWN on c7 for PAWN from the bench");
    }

    @Test
    void theShotClockLimitCanBeChosenInAllMechanicsMode() {
        String output = run("2", "6", "lakers", "celtics", "quit");
        assertContains(output, "/6");
    }

    @Test
    void customModeOnlyEnablesWhatWasAnsweredYesTo() {
        String output = run("3", "y", "5", "y", "n", "lakers", "celtics", "help", "quit");
        assertContains(output, "/5");
        assertContains(output, "trade e4 q");
    }

    @Test
    void customModeWithEverythingOffIsStandardChess() {
        String output = run("3", "n", "n", "n", "lakers", "celtics", "trade e2 p", "quit");
        assertFalse(output.contains("Shot clock ("));
        assertContains(output, "The trade is not enabled in this game.");
    }

    @Test
    void theTradeIsRefusedWhenTheModeDoesNotIncludeIt() {
        String output = run("4", "lakers", "celtics", "trade e2 p", "quit");
        assertContains(output, "The trade is not enabled in this game.");
    }

    @Test
    void anIllegalTradeIsExplained() {
        String output = run("2", "", "lakers", "celtics", "trade e2 q", "quit");
        assertContains(output, "Not allowed");
        assertContains(output, "bench");
    }

    @Test
    void theHotHandAnnouncesTheExtraMoveWithTeamNames() {
        // Black captures twice on consecutive own moves (Qxd5, then Qxg2) and so moves again (Qxh1).
        String output = run("2", "", "lakers", "celtics",
                "e2 e4", "d7 d5", "e4 d5", "d8 d5", "b1 c3", "d5 g2", "g2 h1", "c3 e4", "quit");
        assertContains(output, "Boston Celtics moves again");
    }

    @Test
    void benchShowsCapturedPiecesOfBothSides() {
        String output = run("1", "e2 e4", "d7 d5", "e4 d5", "bench", "quit");
        assertContains(output, "White bench: (empty)");
        assertContains(output, "Black bench: P = Black Pawn");
    }

    // ---- Help ------------------------------------------------------------------------------------------

    @Test
    void helpListsTheCommandsAndOmitsTheTradeWhenItIsOff() {
        String output = run("1", "help", "quit");
        assertContains(output, "Commands:");
        assertContains(output, "undo / redo");
        assertContains(output, "resign");
        assertFalse(output.contains("trade e4 q"));
    }

    @Test
    void helpInAThemedGameExplainsTheRoles() {
        String output = run("4", "lakers", "celtics", "help", "quit");
        assertContains(output, "Q=Point Guard");
        assertContains(output, "P=Rookie");
    }
}
