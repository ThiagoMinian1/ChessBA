package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static ar.uade.tpo.chess.TestSupport.gameOf;
import static ar.uade.tpo.chess.TestSupport.play;
import static ar.uade.tpo.chess.TestSupport.pos;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UndoRedoTest {

    /** Reúne lo observable de una partida para comprobar con un solo equals() que el estado sea idéntico. */
    private record Snapshot(Board board, Team turn, GameStatus status, int clock,
                            List<Piece> whiteBench, List<Piece> blackBench,
                            List<String> history, Set<String> legalMoves, boolean canUndo, boolean canRedo) {
    }

    private static Snapshot snap(ChessGame game) {
        Set<String> legal = new TreeSet<>();
        for (Move move : game.legalMoves()) {
            legal.add(move.toString());
        }
        return new Snapshot(game.board(), game.currentTurn(), game.status(), game.halfmoveClock(),
                game.bench(Team.WHITE), game.bench(Team.BLACK), game.history(), legal,
                game.canUndo(), game.canRedo());
    }

    /** Como {@link #snap}, pero ignora los indicadores de deshacer y rehacer para comparar antes y después del ciclo. */
    private static Snapshot position(ChessGame game) {
        Snapshot s = snap(game);
        return new Snapshot(s.board(), s.turn(), s.status(), s.clock(), s.whiteBench(), s.blackBench(),
                s.history(), s.legalMoves(), false, false);
    }

    @Test
    void undoingANormalMoveRestoresTheBoardAndTheTurn() {
        Game game = new Game();
        Snapshot start = position(game);
        play(game, "e2e4");
        assertTrue(game.undo());
        assertEquals(start, position(game));
        assertEquals(Board.standard(), game.board());
        assertEquals(Team.WHITE, game.currentTurn());
        assertFalse(game.canUndo());
        assertTrue(game.canRedo());
    }

    @Test
    void redoingBringsBackExactlyTheSameState() {
        Game game = new Game();
        play(game, "e2e4");
        Snapshot afterMove = snap(game);
        game.undo();
        assertTrue(game.redo());
        assertEquals(afterMove, snap(game));
    }

    @Test
    void undoingACaptureBringsTheCapturedPieceBack() {
        Game game = gameOf(Team.WHITE, "Ke1", "Ra1", "ke8", "pa5");
        Snapshot before = position(game);
        play(game, "a1a5");
        assertEquals(1, game.bench(Team.BLACK).size());
        game.undo();
        assertEquals(before, position(game));
        assertTrue(game.bench(Team.BLACK).isEmpty());
        assertEquals(PieceType.PAWN, game.board().pieceAt(pos("a5")).orElseThrow().type());
        assertEquals(PieceType.ROOK, game.board().pieceAt(pos("a1")).orElseThrow().type());
    }

    @Test
    void undoingACastleRestoresKingRookAndTheRightToCastleAgain() {
        Game game = gameOf(Team.WHITE, "Ke1", "Rh1", "ke8");
        Snapshot before = position(game);
        play(game, "e1g1");
        assertEquals(PieceType.ROOK, game.board().pieceAt(pos("f1")).orElseThrow().type());
        game.undo();
        assertEquals(before, position(game));
        assertEquals(PieceType.KING, game.board().pieceAt(pos("e1")).orElseThrow().type());
        assertEquals(PieceType.ROOK, game.board().pieceAt(pos("h1")).orElseThrow().type());
        assertTrue(game.legalMovesFrom(pos("e1")).stream().anyMatch(Move::isCastling), "castling is available again");
    }

    @Test
    void redoingACastleCastlesAgain() {
        Game game = gameOf(Team.WHITE, "Ke1", "Ra1", "Rh1", "ke8");
        play(game, "e1c1");
        Snapshot castled = snap(game);
        game.undo();
        game.redo();
        assertEquals(castled, snap(game));
        assertEquals(PieceType.KING, game.board().pieceAt(pos("c1")).orElseThrow().type());
        assertEquals(PieceType.ROOK, game.board().pieceAt(pos("d1")).orElseThrow().type());
    }

    @Test
    void undoingEnPassantRestoresTheCapturedPawnAndTheChanceToCaptureAgain() {
        Game game = gameOf(Team.BLACK, "Ke1", "ke8", "Pe5", "pd7");
        play(game, "d7d5");
        Snapshot beforeCapture = position(game);
        play(game, "e5d6");
        assertTrue(game.board().isEmpty(pos("d5")));
        game.undo();
        assertEquals(beforeCapture, position(game));
        assertEquals(Team.BLACK, game.board().pieceAt(pos("d5")).orElseThrow().team());
        assertTrue(game.legalMovesFrom(pos("e5")).stream().anyMatch(m -> m.to().equals(pos("d6"))),
                "the en passant window is restored too");
        assertTrue(game.bench(Team.BLACK).isEmpty());
    }

    @Test
    void redoingEnPassantCapturesTheSamePawn() {
        Game game = gameOf(Team.BLACK, "Ke1", "ke8", "Pe5", "pd7");
        play(game, "d7d5", "e5d6");
        Snapshot captured = snap(game);
        game.undo();
        game.redo();
        assertEquals(captured, snap(game));
        assertTrue(game.board().isEmpty(pos("d5")));
    }

    @Test
    void undoingAPromotionBringsTheOriginalPawnBack() {
        Game game = gameOf(Team.WHITE, "Ka1", "kh6", "Pe7");
        Snapshot before = position(game);
        play(game, "e7e8q");
        assertEquals(PieceType.QUEEN, game.board().pieceAt(pos("e8")).orElseThrow().type());
        game.undo();
        assertEquals(before, position(game));
        assertEquals(PieceType.PAWN, game.board().pieceAt(pos("e7")).orElseThrow().type());
        assertTrue(game.board().isEmpty(pos("e8")));
    }

    @Test
    void redoingAPromotionPromotesToTheSamePieceAgain() {
        Game game = gameOf(Team.WHITE, "Ka1", "kh6", "Pe7");
        play(game, "e7e8n");
        game.undo();
        game.redo();
        assertEquals(PieceType.KNIGHT, game.board().pieceAt(pos("e8")).orElseThrow().type());
    }

    @Test
    void undoingAPromotionByCaptureRestoresBothPieces() {
        Game game = gameOf(Team.WHITE, "Ka1", "kh6", "Pe7", "rd8");
        Snapshot before = position(game);
        play(game, "e7d8r");
        game.undo();
        assertEquals(before, position(game));
        assertEquals(PieceType.ROOK, game.board().pieceAt(pos("d8")).orElseThrow().type());
        assertEquals(Team.BLACK, game.board().pieceAt(pos("d8")).orElseThrow().team());
    }

    @Test
    void undoingACheckmateReopensTheGame() {
        Game game = new Game();
        play(game, "f2f3", "e7e5", "g2g4");
        Snapshot beforeMate = position(game);
        play(game, "d8h4");
        assertEquals(GameStatus.CHECKMATE, game.status());
        game.undo();
        assertEquals(GameStatus.ONGOING, game.status());
        assertEquals(beforeMate, position(game));
        game.redo();
        assertEquals(GameStatus.CHECKMATE, game.status());
        assertEquals(Team.BLACK, game.winner().orElseThrow());
    }

    @Test
    void undoingAResignationOrAgreedDrawReopensTheGame() {
        Game resigned = new Game();
        play(resigned, "e2e4");
        resigned.resign(Team.BLACK);
        resigned.undo();
        assertEquals(GameStatus.ONGOING, resigned.status());
        assertTrue(resigned.winner().isEmpty());
        play(resigned, "e7e5");

        Game drawn = new Game();
        drawn.agreeDraw();
        drawn.undo();
        assertEquals(GameStatus.ONGOING, drawn.status());
    }

    @Test
    void undoingAThreefoldDrawKeepsTheRepetitionCountsCorrect() {
        Game game = new Game();
        play(game, "g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8");
        assertEquals(GameStatus.DRAW_THREEFOLD_REPETITION, game.status());
        game.undo();
        assertEquals(GameStatus.ONGOING, game.status());
        play(game, "f6g8");
        assertEquals(GameStatus.DRAW_THREEFOLD_REPETITION, game.status(), "the count survived the undo");
    }

    @Test
    void undoingRestoresTheFiftyMoveCounter() {
        GameState state = ar.uade.tpo.chess.TestSupport.stateOf(Team.WHITE, "Ke1", "Ra1", "ke8", "rh8");
        state.setHalfmoveClock(40);
        Game game = new Game(state);
        play(game, "e1e2");
        assertEquals(41, game.halfmoveClock());
        game.undo();
        assertEquals(40, game.halfmoveClock());
    }

    @Test
    void nothingToUndoOrRedoReturnsFalse() {
        Game game = new Game();
        assertFalse(game.undo());
        assertFalse(game.redo());
        play(game, "e2e4");
        assertFalse(game.redo());
        assertTrue(game.undo());
        assertFalse(game.undo());
    }

    @Test
    void aNewMoveAfterUndoDiscardsTheRedoHistory() {
        Game game = new Game();
        play(game, "e2e4");
        game.undo();
        play(game, "d2d4");
        assertFalse(game.canRedo());
        assertFalse(game.redo());
        assertEquals(List.of("d2d4"), game.history());
    }

    @Test
    void historyOnlyListsTheActionsCurrentlyApplied() {
        Game game = new Game();
        play(game, "e2e4", "e7e5", "g1f3");
        game.undo();
        assertEquals(List.of("e2e4", "e7e5"), game.history());
        game.redo();
        assertEquals(List.of("e2e4", "e7e5", "g1f3"), game.history());
    }

    @Test
    void walkingAllTheWayBackAndForthThroughAWholeGameIsExact() {
        Game game = new Game();
        List<Snapshot> states = new ArrayList<>();
        states.add(position(game));
        String[] moves = {"e2e4", "e7e5", "d1h5", "b8c6", "f1c4", "g8f6", "h5f7"};
        for (String move : moves) {
            play(game, move);
            states.add(position(game));
        }
        assertEquals(GameStatus.CHECKMATE, game.status());

        for (int i = moves.length - 1; i >= 0; i--) {
            assertTrue(game.undo());
            assertEquals(states.get(i), position(game), "after undoing move " + (i + 1));
        }
        assertEquals(Board.standard(), game.board());
        assertFalse(game.canUndo());

        for (int i = 1; i <= moves.length; i++) {
            assertTrue(game.redo());
            assertEquals(states.get(i), position(game), "after redoing move " + i);
        }
        assertFalse(game.canRedo());
    }

    @Test
    void undoingAndRedoingAMixOfSpecialMovesIsExact() {
        // Enroque, captura y promoción en una misma partida; se deshacen y rehacen varias veces.
        Game game = gameOf(Team.WHITE, "Ke1", "Rh1", "Pa7", "ke8", "rb8", "pc3");
        List<Snapshot> states = new ArrayList<>();
        states.add(position(game));
        for (String move : new String[] {"e1g1", "c3c2", "a7b8q", "e8d7"}) {
            play(game, move);
            states.add(position(game));
        }
        for (int i = 3; i >= 0; i--) {
            game.undo();
            assertEquals(states.get(i), position(game));
        }
        for (int i = 1; i <= 4; i++) {
            game.redo();
            assertEquals(states.get(i), position(game));
        }
    }
}
