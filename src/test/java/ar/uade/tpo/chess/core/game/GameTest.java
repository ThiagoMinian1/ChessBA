package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static ar.uade.tpo.chess.TestSupport.gameOf;
import static ar.uade.tpo.chess.TestSupport.play;
import static ar.uade.tpo.chess.TestSupport.pos;
import static ar.uade.tpo.chess.TestSupport.stateOf;
import static ar.uade.tpo.chess.TestSupport.targetsFrom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTest {

    // ---- Inicio y turnos ------------------------------------------------------------------------------

    @Test
    void aNewGameStartsFromTheStandardPositionWithWhiteToMove() {
        Game game = new Game();
        assertEquals(Board.standard(), game.board());
        assertEquals(Team.WHITE, game.currentTurn());
        assertEquals(GameStatus.ONGOING, game.status());
        assertEquals(20, game.legalMoves().size());
        assertEquals(0, game.halfmoveClock());
        assertTrue(game.winner().isEmpty());
        assertFalse(game.isInCheck());
        assertFalse(game.canUndo());
        assertFalse(game.canRedo());
        assertTrue(game.history().isEmpty());
    }

    @Test
    void turnsAlternateAfterEveryMove() {
        Game game = new Game();
        play(game, "e2e4");
        assertEquals(Team.BLACK, game.currentTurn());
        play(game, "e7e5");
        assertEquals(Team.WHITE, game.currentTurn());
        play(game, "g1f3");
        assertEquals(Team.BLACK, game.currentTurn());
    }

    @Test
    void aPlayerCannotMoveTheOpponentsPieces() {
        Game game = new Game();
        assertThrows(IllegalMoveException.class, () -> game.play(pos("e7"), pos("e5")));
        play(game, "e2e4");
        assertThrows(IllegalMoveException.class, () -> game.play(pos("d2"), pos("d4")));
    }

    @Test
    void movingFromAnEmptySquareIsRejected() {
        assertThrows(IllegalMoveException.class, () -> new Game().play(pos("e4"), pos("e5")));
    }

    @Test
    void movingToTheSameSquareIsRejected() {
        assertThrows(IllegalMoveException.class, () -> new Game().play(pos("e2"), pos("e2")));
    }

    @Test
    void piecesCannotMoveOntoOwnPiecesOrThroughOthers() {
        Game game = new Game();
        assertThrows(IllegalMoveException.class, () -> game.play(pos("a1"), pos("a2")));
        assertThrows(IllegalMoveException.class, () -> game.play(pos("a1"), pos("a3")));
        assertThrows(IllegalMoveException.class, () -> game.play(pos("c1"), pos("e3")));
        assertThrows(IllegalMoveException.class, () -> game.play(pos("d1"), pos("d3")));
    }

    @Test
    void eachPieceTypeRejectsMovesOutsideItsPattern() {
        Game game = new Game();
        assertThrows(IllegalMoveException.class, () -> game.play(pos("e2"), pos("e5")), "pawn three squares");
        assertThrows(IllegalMoveException.class, () -> game.play(pos("e2"), pos("d3")), "pawn diagonal, no capture");
        assertThrows(IllegalMoveException.class, () -> game.play(pos("b1"), pos("b3")), "knight straight");
        assertThrows(IllegalMoveException.class, () -> game.play(pos("e1"), pos("e3")), "king two squares");
    }

    @Test
    void aRejectedMoveChangesNothing() {
        Game game = new Game();
        Board before = game.board();
        assertThrows(IllegalMoveException.class, () -> game.play(pos("e2"), pos("e5")));
        assertEquals(before, game.board());
        assertEquals(Team.WHITE, game.currentTurn());
        assertFalse(game.canUndo());
        assertTrue(game.history().isEmpty());
    }

    @Test
    void aMoveBuiltByHandIsCheckedAgainstTheRulesToo() {
        Game game = new Game();
        assertThrows(IllegalMoveException.class, () -> game.play(Move.normal(pos("e2"), pos("e5"))));
        assertThrows(IllegalMoveException.class, () -> game.play(Move.enPassant(pos("e2"), pos("d3"))));
        game.play(Move.doublePush(pos("e2"), pos("e4")));
        assertEquals(Team.BLACK, game.currentTurn());
    }

    @Test
    void theBoardReturnedIsACopy() {
        Game game = new Game();
        Board board = game.board();
        board.remove(pos("e2"));
        assertTrue(game.board().pieceAt(pos("e2")).isPresent());
        assertNotSame(game.board(), game.board());
    }

    @Test
    void historyListsTheMovesPlayed() {
        Game game = new Game();
        play(game, "e2e4", "e7e5");
        assertEquals(List.of("e2e4", "e7e5"), game.history());
    }

    // ---- Capturas y contador de jugadas ----------------------------------------------------------------

    @Test
    void aCaptureRemovesThePieceAndSendsItToTheBench() {
        Game game = new Game();
        play(game, "e2e4", "d7d5", "e4d5");
        assertEquals(Team.WHITE, game.board().pieceAt(pos("d5")).orElseThrow().team());
        assertEquals(1, game.bench(Team.BLACK).size());
        assertEquals(PieceType.PAWN, game.bench(Team.BLACK).get(0).type());
        assertTrue(game.bench(Team.WHITE).isEmpty());
        assertEquals(31, game.board().pieces().size());
    }

    @Test
    void theBenchIsAnUnmodifiableCopy() {
        Game game = new Game();
        play(game, "e2e4", "d7d5", "e4d5");
        assertThrows(UnsupportedOperationException.class, () -> game.bench(Team.BLACK).clear());
        assertEquals(1, game.bench(Team.BLACK).size());
    }

    @Test
    void theHalfmoveClockCountsQuietMovesAndResetsOnCaptureOrPawnMove() {
        Game game = new Game();
        play(game, "g1f3");
        assertEquals(1, game.halfmoveClock());
        play(game, "g8f6");
        assertEquals(2, game.halfmoveClock());
        play(game, "e2e4");
        assertEquals(0, game.halfmoveClock());
        play(game, "f6e4");
        assertEquals(0, game.halfmoveClock(), "a capture resets it too");
        play(game, "f3g1");
        assertEquals(1, game.halfmoveClock());
    }

    // ---- Jaque -----------------------------------------------------------------------------------------

    @Test
    void detectsCheck() {
        Game game = new Game();
        play(game, "e2e4", "f7f6", "d1h5");
        assertTrue(game.isInCheck());
        assertEquals(Team.BLACK, game.currentTurn());
        assertEquals(GameStatus.ONGOING, game.status());
    }

    @Test
    void whileInCheckOnlyMovesThatEndTheCheckAreAccepted() {
        Game game = new Game();
        play(game, "e2e4", "f7f6", "d1h5");
        assertTrue(game.legalMovesFrom(pos("g7")).stream().anyMatch(m -> m.to().equals(pos("g6"))));
        assertThrows(IllegalMoveException.class, () -> game.play(pos("a7"), pos("a6")));
        game.play(pos("g7"), pos("g6"));
        assertEquals(Team.WHITE, game.currentTurn());
        assertFalse(game.status().isOver());
    }

    @Test
    void aPlayerCannotLeaveTheirOwnKingInCheck() {
        Game game = gameOf(Team.WHITE, "Ke1", "Be2", "re8", "ka8");
        assertTrue(game.legalMovesFrom(pos("e2")).isEmpty(), "the bishop is pinned");
        assertThrows(IllegalMoveException.class, () -> game.play(pos("e2"), pos("f3")));
    }

    @Test
    void theKingCannotMoveIntoCheck() {
        Game game = gameOf(Team.WHITE, "Ke1", "rd8", "ka8");
        assertThrows(IllegalMoveException.class, () -> game.play(pos("e1"), pos("d1")));
        assertThrows(IllegalMoveException.class, () -> game.play(pos("e1"), pos("d2")));
        game.play(pos("e1"), pos("e2"));
    }

    @Test
    void twoKingsCannotStandNextToEachOther() {
        Game game = gameOf(Team.WHITE, "Ke1", "ke3", "Pa2");
        assertEquals(Set.of("d1", "f1"), targetsFrom(game, "e1"));
    }

    // ---- Jaque mate ------------------------------------------------------------------------------------

    @Test
    void foolsMate() {
        Game game = new Game();
        play(game, "f2f3", "e7e5", "g2g4", "d8h4");
        assertEquals(GameStatus.CHECKMATE, game.status());
        assertEquals(Team.BLACK, game.winner().orElseThrow());
        assertTrue(game.legalMoves().isEmpty());
        assertTrue(game.isInCheck());
    }

    @Test
    void scholarsMate() {
        Game game = new Game();
        play(game, "e2e4", "e7e5", "d1h5", "b8c6", "f1c4", "g8f6", "h5f7");
        assertEquals(GameStatus.CHECKMATE, game.status());
        assertEquals(Team.WHITE, game.winner().orElseThrow());
    }

    @Test
    void backRankMate() {
        Game game = gameOf(Team.WHITE, "kh8", "pg7", "ph7", "Ra1", "Kb1");
        play(game, "a1a8");
        assertEquals(GameStatus.CHECKMATE, game.status());
        assertEquals(Team.WHITE, game.winner().orElseThrow());
    }

    @Test
    void aPositionThatIsAlreadyMateIsRecognisedWhenTheGameStarts() {
        Game game = gameOf(Team.BLACK, "kh8", "pg7", "ph7", "Ra8", "Kh1");
        assertEquals(GameStatus.CHECKMATE, game.status());
        assertEquals(Team.WHITE, game.winner().orElseThrow());
    }

    @Test
    void aCheckThatCanBeBlockedOrCapturedIsNotMate() {
        // El caballo de e6 puede interponerse en d8 o f8, así que Ta8+ es jaque, pero no mate.
        Game game = gameOf(Team.WHITE, "kh8", "pg7", "ph7", "Ra1", "Kb1", "ne6");
        play(game, "a1a8");
        assertTrue(game.isInCheck());
        assertEquals(GameStatus.ONGOING, game.status());
        assertEquals(Set.of("d8", "f8"), targetsFrom(game, "e6").stream()
                .filter(s -> s.endsWith("8")).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void noMovesAreAcceptedAfterTheGameEnds() {
        Game game = new Game();
        play(game, "f2f3", "e7e5", "g2g4", "d8h4");
        assertThrows(IllegalMoveException.class, () -> game.play(pos("a2"), pos("a3")));
        assertThrows(IllegalMoveException.class, () -> game.resign(Team.WHITE));
        assertTrue(game.legalMovesFrom(pos("a2")).isEmpty());
    }

    // ---- Ahogado ---------------------------------------------------------------------------------------

    @Test
    void stalemateIsADrawNotAWin() {
        Game game = gameOf(Team.WHITE, "ka8", "Kb6", "Qc1");
        play(game, "c1c7");
        assertEquals(GameStatus.STALEMATE, game.status());
        assertTrue(game.winner().isEmpty());
        assertFalse(game.isInCheck());
        assertTrue(game.legalMoves().isEmpty());
    }

    @Test
    void aPositionThatIsAlreadyStalemateIsRecognisedWhenTheGameStarts() {
        assertEquals(GameStatus.STALEMATE, gameOf(Team.BLACK, "ka8", "Kb6", "Qc7").status());
    }

    // ---- Material insuficiente -------------------------------------------------------------------------

    @Test
    void capturingTheLastPieceLeavingOnlyKingsIsADraw() {
        Game game = gameOf(Team.WHITE, "Ke1", "ke8", "re2");
        play(game, "e1e2");
        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, game.status());
        assertTrue(game.winner().isEmpty());
    }

    @Test
    void kingAndBishopAgainstKingIsADrawFromTheStart() {
        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, gameOf(Team.WHITE, "Ke1", "Bc1", "ke8").status());
        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, gameOf(Team.WHITE, "Ke1", "Nc1", "ke8").status());
    }

    @Test
    void sufficientMaterialKeepsTheGameGoing() {
        assertEquals(GameStatus.ONGOING, gameOf(Team.WHITE, "Ke1", "Ra1", "ke8").status());
    }

    // ---- Regla de los cincuenta movimientos ------------------------------------------------------------

    @Test
    void theFiftyMoveRuleDrawsAfterOneHundredQuietHalfMoves() {
        GameState state = stateOf(Team.WHITE, "Ke1", "Ra1", "ke8", "rh8");
        state.setHalfmoveClock(99);
        Game game = new Game(state);
        assertEquals(GameStatus.ONGOING, game.status());
        play(game, "e1e2");
        assertEquals(100, game.halfmoveClock());
        assertEquals(GameStatus.DRAW_FIFTY_MOVE_RULE, game.status());
        assertTrue(game.winner().isEmpty());
    }

    @Test
    void ninetyNineHalfMovesIsNotYetADraw() {
        GameState state = stateOf(Team.WHITE, "Ke1", "Ra1", "ke8", "rh8");
        state.setHalfmoveClock(98);
        Game game = new Game(state);
        play(game, "e1e2");
        assertEquals(99, game.halfmoveClock());
        assertEquals(GameStatus.ONGOING, game.status());
    }

    @Test
    void aPawnMoveRestartsTheFiftyMoveCount() {
        GameState state = stateOf(Team.WHITE, "Ke1", "Pa2", "ke8", "rh8");
        state.setHalfmoveClock(99);
        Game game = new Game(state);
        play(game, "a2a3");
        assertEquals(0, game.halfmoveClock());
        assertEquals(GameStatus.ONGOING, game.status());
    }

    @Test
    void aCaptureRestartsTheFiftyMoveCount() {
        GameState state = stateOf(Team.WHITE, "Ke1", "Ra1", "ke8", "pa5");
        state.setHalfmoveClock(99);
        Game game = new Game(state);
        play(game, "a1a5");
        assertEquals(0, game.halfmoveClock());
        assertEquals(GameStatus.ONGOING, game.status());
    }

    @Test
    void checkmateOutranksTheFiftyMoveRule() {
        GameState state = stateOf(Team.WHITE, "kh8", "pg7", "ph7", "Ra1", "Kh1");
        state.setHalfmoveClock(99);
        Game game = new Game(state);
        play(game, "a1a8");
        assertEquals(GameStatus.CHECKMATE, game.status());
    }

    @Test
    void aStateAdoptedAtOneHundredHalfMovesIsDrawnImmediately() {
        GameState state = stateOf(Team.WHITE, "Ke1", "Ra1", "ke8", "rh8");
        state.setHalfmoveClock(100);
        assertEquals(GameStatus.DRAW_FIFTY_MOVE_RULE, new Game(state).status());
    }

    // ---- Repetición triple -----------------------------------------------------------------------------

    @Test
    void threefoldRepetitionDrawsOnTheThirdOccurrence() {
        Game game = new Game();
        play(game, "g1f3", "g8f6", "f3g1", "f6g8");
        assertEquals(GameStatus.ONGOING, game.status(), "second occurrence of the start position");
        play(game, "g1f3", "g8f6", "f3g1");
        assertEquals(GameStatus.ONGOING, game.status());
        play(game, "f6g8");
        assertEquals(GameStatus.DRAW_THREEFOLD_REPETITION, game.status());
        assertTrue(game.winner().isEmpty());
    }

    @Test
    void theSamePositionReachedByDifferentMovesStillCounts() {
        Game game = new Game();
        play(game, "g1f3", "g8f6", "f3g1", "f6g8");
        assertEquals(GameStatus.ONGOING, game.status(), "second occurrence of the start position");
        play(game, "b1c3", "b8c6", "c3b1", "c6b8");
        assertEquals(GameStatus.DRAW_THREEFOLD_REPETITION, game.status(), "third, through other knights");
    }

    @Test
    void aDifferentSideToMoveIsADifferentPosition() {
        Game game = new Game();
        play(game, "g1f3", "g8f6", "f3g1", "f6g8");
        assertEquals(Board.standard().toString(), game.board().toString());
        // En la tercera media jugada se repitió el tablero inicial, pero con negras al turno: es otra posición
        // y todavía no hay tablas en la cuarta media jugada.
        assertEquals(GameStatus.ONGOING, game.status());
        play(game, "g1f3", "g8f6", "f3g1", "f6g8");
        assertEquals(GameStatus.DRAW_THREEFOLD_REPETITION, game.status());
    }

    @Test
    void lostCastlingRightsMakeARepeatedBoardADifferentPosition() {
        Game game = gameOf(Team.WHITE, "Ke1", "Ra1", "kd7");
        // Tras cuatro medias jugadas, el tablero vuelve a la posición inicial, pero las blancas ya no pueden enrocar.
        play(game, "a1a2", "d7d6", "a2a1", "d6d7");
        play(game, "a1a2", "d7d6", "a2a1", "d6d7");
        assertEquals(GameStatus.ONGOING, game.status(), "the start position had different castling rights");
        play(game, "a1a2");
        assertEquals(GameStatus.DRAW_THREEFOLD_REPETITION, game.status(), "rook on a2, third time");
    }

    @Test
    void anEnPassantChanceOnlyCountsWhenTheCaptureIsPossible() {
        // Después de 1.e4, ningún peón negro puede capturar en e3 al paso. Esa casilla no debe hacer que la
        // posición sea distinta de la que se repita en los ciclos siguientes.
        Game game = new Game();
        play(game, "e2e4", "g8f6", "g1f3", "f6g8", "f3g1");
        assertEquals(GameStatus.ONGOING, game.status());
        play(game, "g8f6", "g1f3", "f6g8", "f3g1");
        assertEquals(GameStatus.DRAW_THREEFOLD_REPETITION, game.status());
    }

    // ---- Abandono y tablas acordadas -------------------------------------------------------------------

    @Test
    void resigningGivesTheWinToTheOpponent() {
        Game game = new Game();
        game.resign(Team.WHITE);
        assertEquals(GameStatus.RESIGNATION, game.status());
        assertEquals(Team.BLACK, game.winner().orElseThrow());
        assertTrue(game.legalMoves().isEmpty());
    }

    @Test
    void aDrawByAgreementHasNoWinner() {
        Game game = new Game();
        game.agreeDraw();
        assertEquals(GameStatus.DRAW_BY_AGREEMENT, game.status());
        assertTrue(game.winner().isEmpty());
    }

    @Test
    void theGameFinishesAtMostOnce() {
        Game game = new Game();
        game.agreeDraw();
        assertThrows(IllegalMoveException.class, game::agreeDraw);
        assertThrows(IllegalMoveException.class, () -> game.resign(Team.BLACK));
    }
}
