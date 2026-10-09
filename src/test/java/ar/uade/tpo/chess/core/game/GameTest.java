package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.King;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Queen;
import ar.uade.tpo.chess.core.model.Rook;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTest {

    @Test
    void whiteMovesFirst() {
        Game game = new Game();

        assertEquals(Team.WHITE, game.getCurrentTurn());
    }

    @Test
    void aValidMoveSwitchesTheTurnAndIsRecordedInHistory() {
        Game game = new Game();

        game.move(new Position(1, 4), new Position(3, 4)); // e2-e4

        assertEquals(Team.BLACK, game.getCurrentTurn());
        assertEquals(1, game.getHistory().size());
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
    }

    @Test
    void movingAnOpponentsPieceIsRejected() {
        Game game = new Game();

        assertThrows(IllegalArgumentException.class,
                () -> game.move(new Position(6, 4), new Position(5, 4))); // trying to move a black pawn as White
    }

    @Test
    void movingThroughABlockedPathIsRejected() {
        Game game = new Game(); // standard setup: every rook is boxed in by its own pawns

        assertThrows(IllegalArgumentException.class,
                () -> game.move(new Position(0, 0), new Position(3, 0)));
    }

    @Test
    void aMoveThatWouldLeaveOwnKingInCheckIsIllegal() {
        // White king on e1, white rook pinned on e2, black rook attacking down the e-file.
        Board board = Board.empty();
        board.placePiece(new Position(0, 4), new King(Team.WHITE));
        board.placePiece(new Position(1, 4), new Rook(Team.WHITE));
        board.placePiece(new Position(7, 4), new Rook(Team.BLACK));
        Game game = new Game(board, Team.WHITE);

        // Sliding sideways would expose the king, so it must not be offered as legal...
        assertFalse(game.legalMoves(new Position(1, 4)).contains(new Position(1, 0)));
        // ...while staying on the e-file (still blocking the check) remains legal.
        assertTrue(game.legalMoves(new Position(1, 4)).contains(new Position(3, 4)));
        assertThrows(IllegalArgumentException.class,
                () -> game.move(new Position(1, 4), new Position(1, 0)));
    }

    @Test
    void capturingAPieceNotifiesCaptureAndRemovesItFromTheBoard() {
        Board board = Board.empty();
        board.placePiece(new Position(0, 4), new King(Team.WHITE));
        board.placePiece(new Position(7, 4), new King(Team.BLACK));
        board.placePiece(new Position(3, 3), new Rook(Team.WHITE));
        board.placePiece(new Position(3, 6), new Rook(Team.BLACK));
        Game game = new Game(board, Team.WHITE);

        game.move(new Position(3, 3), new Position(3, 6));

        assertTrue(game.getHistory().get(0).isCapture());
        assertTrue(game.getBoard().pieceAt(new Position(3, 6)) instanceof Rook);
    }

    @Test
    void foolsMateEndsTheGameInCheckmate() {
        Game game = new Game();

        game.move(new Position(1, 5), new Position(2, 5)); // 1. f2-f3
        game.move(new Position(6, 4), new Position(4, 4)); // 1... e7-e5
        game.move(new Position(1, 6), new Position(3, 6)); // 2. g2-g4
        game.move(new Position(7, 3), new Position(3, 7)); // 2... Qd8-h4#

        assertEquals(GameStatus.CHECKMATE, game.getStatus());
        assertTrue(game.isInCheck(Team.WHITE));
    }

    @Test
    void theClassicKingAndQueenEndgamePositionIsStalemate() {
        // Textbook stalemate: White king h1, Black king f2, Black queen one
        // step away from delivering it (g4), about to play Qg4-g3#.
        Board board = Board.empty();
        board.placePiece(new Position(0, 7), new King(Team.WHITE));
        board.placePiece(new Position(1, 5), new King(Team.BLACK));
        board.placePiece(new Position(3, 6), new Queen(Team.BLACK));
        Game game = new Game(board, Team.BLACK);

        game.move(new Position(3, 6), new Position(2, 6)); // Qg4-g3

        assertEquals(GameStatus.STALEMATE, game.getStatus());
        assertFalse(game.isInCheck(Team.WHITE));
    }

    @Test
    void noFurtherMovesAreAllowedOnceTheGameHasEnded() {
        Game game = new Game();
        game.move(new Position(1, 5), new Position(2, 5));
        game.move(new Position(6, 4), new Position(4, 4));
        game.move(new Position(1, 6), new Position(3, 6));
        game.move(new Position(7, 3), new Position(3, 7)); // checkmate

        assertThrows(IllegalStateException.class,
                () -> game.move(new Position(0, 4), new Position(1, 5)));
    }
}
