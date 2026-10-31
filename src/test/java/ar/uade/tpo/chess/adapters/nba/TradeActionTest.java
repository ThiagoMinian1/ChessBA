package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.TestSupport.Recorder;
import ar.uade.tpo.chess.core.game.Game;
import ar.uade.tpo.chess.core.game.GameState;
import ar.uade.tpo.chess.core.game.IllegalMoveException;
import ar.uade.tpo.chess.core.model.Knight;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.Pawn;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Queen;
import ar.uade.tpo.chess.core.model.Rook;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.uade.tpo.chess.TestSupport.play;
import static ar.uade.tpo.chess.TestSupport.pos;
import static ar.uade.tpo.chess.TestSupport.stateOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeActionTest {

    private static List<PieceType> types(List<Piece> pieces) {
        return pieces.stream().map(Piece::type).toList();
    }

    /** Peón blanco en e4 que puede cambiarse por una dama del banco; el rey negro está seguro en h8. */
    private static Game gameWithWhiteQueenOnTheBench() {
        GameState state = stateOf(Team.WHITE, "Ke1", "Pe4", "kh8");
        state.bench(Team.WHITE).add(new Queen(Team.WHITE));
        return new Game(state);
    }

    // ---- Intercambio exitoso ---------------------------------------------------------------------------

    @Test
    void theBenchPieceTakesTheSquareAndTheBoardPieceGoesToTheBench() {
        Game game = gameWithWhiteQueenOnTheBench();
        game.execute(new TradeAction(pos("e4"), PieceType.QUEEN));

        Piece onE4 = game.board().pieceAt(pos("e4")).orElseThrow();
        assertEquals(PieceType.QUEEN, onE4.type());
        assertEquals(Team.WHITE, onE4.team());
        assertEquals(List.of(PieceType.PAWN), types(game.bench(Team.WHITE)));
        assertTrue(game.bench(Team.BLACK).isEmpty(), "a trade is not a capture");
    }

    @Test
    void aTradeUsesUpTheTurn() {
        Game game = gameWithWhiteQueenOnTheBench();
        game.execute(new TradeAction(pos("e4"), PieceType.QUEEN));
        assertEquals(Team.BLACK, game.currentTurn());
        assertEquals(List.of("Trade e4 for bench QUEEN"), game.history());
    }

    @Test
    void aTradeIsAnnouncedAsANoticeButNotAsACapture() {
        Game game = gameWithWhiteQueenOnTheBench();
        Recorder recorder = new Recorder();
        game.addObserver(recorder);
        game.execute(new TradeAction(pos("e4"), PieceType.QUEEN));
        assertEquals(List.of("notice WHITE trades PAWN on e4 for QUEEN from the bench"), recorder.events);
    }

    @Test
    void eachTeamHasItsOwnTrade() {
        GameState state = stateOf(Team.WHITE, "Ke1", "Pe4", "kh8", "pa7");
        state.bench(Team.WHITE).add(new Queen(Team.WHITE));
        state.bench(Team.BLACK).add(new Knight(Team.BLACK));
        Game game = new Game(state);

        assertFalse(TradeAction.hasTraded(game, Team.WHITE));
        game.execute(new TradeAction(pos("e4"), PieceType.QUEEN));
        assertTrue(TradeAction.hasTraded(game, Team.WHITE));
        assertFalse(TradeAction.hasTraded(game, Team.BLACK));

        game.execute(new TradeAction(pos("a7"), PieceType.KNIGHT));
        assertTrue(TradeAction.hasTraded(game, Team.BLACK));
        assertEquals(PieceType.KNIGHT, game.board().pieceAt(pos("a7")).orElseThrow().type());
        assertEquals(List.of(PieceType.PAWN), types(game.bench(Team.BLACK)));
    }

    @Test
    void aTeamCanTradeOnlyOncePerGame() {
        Game game = gameWithWhiteQueenOnTheBench();
        game.execute(new TradeAction(pos("e4"), PieceType.QUEEN));
        play(game, "h8g8");
        IllegalMoveException error = assertThrows(IllegalMoveException.class,
                () -> game.execute(new TradeAction(pos("e4"), PieceType.PAWN)));
        assertTrue(error.getMessage().contains("already used"));
        assertEquals(Team.WHITE, game.currentTurn());
    }

    @Test
    void aTradeResetsTheFiftyMoveCounter() {
        GameState state = stateOf(Team.WHITE, "Ke1", "Pe4", "kh8");
        state.bench(Team.WHITE).add(new Queen(Team.WHITE));
        state.setHalfmoveClock(7);
        Game game = new Game(state);
        game.execute(new TradeAction(pos("e4"), PieceType.QUEEN));
        assertEquals(0, game.halfmoveClock());
    }

    @Test
    void anIncomingRookHasNoCastlingRights() {
        GameState state = stateOf(Team.WHITE, "Ke1", "Rh1", "ke8");
        state.bench(Team.WHITE).add(new Rook(Team.WHITE));
        Game game = new Game(state);
        assertTrue(game.legalMovesFrom(pos("e1")).stream().anyMatch(Move::isCastling), "available before the trade");

        game.execute(new TradeAction(pos("h1"), PieceType.ROOK));
        play(game, "e8d8");
        assertEquals(PieceType.ROOK, game.board().pieceAt(pos("h1")).orElseThrow().type());
        assertTrue(game.legalMovesFrom(pos("e1")).stream().noneMatch(Move::isCastling));
    }

    @Test
    void aTradeCanGiveCheck() {
        GameState state = stateOf(Team.WHITE, "Kg1", "Pe4", "ke8");
        state.bench(Team.WHITE).add(new Queen(Team.WHITE));
        Game game = new Game(state);
        Recorder recorder = new Recorder();
        game.addObserver(recorder);

        game.execute(new TradeAction(pos("e4"), PieceType.QUEEN));
        assertTrue(game.isInCheck());
        assertEquals("check BLACK", recorder.events.get(recorder.events.size() - 1));
    }

    // ---- Intercambios no permitidos --------------------------------------------------------------------

    private static IllegalMoveException rejected(GameState state, TradeAction action) {
        Game game = new Game(state);
        IllegalMoveException error = assertThrows(IllegalMoveException.class, () -> game.execute(action));
        assertEquals(Team.WHITE, game.currentTurn(), "a rejected trade does not consume the turn");
        assertFalse(game.canUndo());
        assertFalse(TradeAction.hasTraded(game, Team.WHITE), "and does not use up the trade");
        return error;
    }

    @Test
    void theKingCannotBeTraded() {
        GameState state = stateOf(Team.WHITE, "Ke1", "ke8", "Pa2");
        state.bench(Team.WHITE).add(new Queen(Team.WHITE));
        assertTrue(rejected(state, new TradeAction(pos("e1"), PieceType.QUEEN)).getMessage().contains("king"));
    }

    @Test
    void thereMustBeAnOwnPieceOnTheSquare() {
        GameState state = stateOf(Team.WHITE, "Ke1", "ke8", "Pa2");
        state.bench(Team.WHITE).add(new Queen(Team.WHITE));
        rejected(state, new TradeAction(pos("d4"), PieceType.QUEEN));
        rejected(state, new TradeAction(pos("e8"), PieceType.QUEEN));
    }

    @Test
    void theBenchMustHoldThePieceAskedFor() {
        GameState state = stateOf(Team.WHITE, "Ke1", "ke8", "Pa2");
        state.bench(Team.WHITE).add(new Queen(Team.WHITE));
        assertTrue(rejected(state, new TradeAction(pos("a2"), PieceType.ROOK)).getMessage().contains("bench"));
    }

    @Test
    void theBenchMustBelongToTheTeamTrading() {
        GameState state = stateOf(Team.WHITE, "Ke1", "ke8", "Pa2");
        state.bench(Team.BLACK).add(new Queen(Team.BLACK));
        rejected(state, new TradeAction(pos("a2"), PieceType.QUEEN));
    }

    @Test
    void aPawnCannotEnterOnTheFirstOrLastRank() {
        GameState first = stateOf(Team.WHITE, "Ke1", "Ra1", "kh8");
        first.bench(Team.WHITE).add(new Pawn(Team.WHITE));
        rejected(first, new TradeAction(pos("a1"), PieceType.PAWN));

        GameState last = stateOf(Team.WHITE, "Ke1", "Ra8", "kh7");
        last.bench(Team.WHITE).add(new Pawn(Team.WHITE));
        rejected(last, new TradeAction(pos("a8"), PieceType.PAWN));
    }

    @Test
    void blackPawnsCannotEnterOnTheFirstOrLastRankEither() {
        GameState first = stateOf(Team.BLACK, "Kh3", "ke8", "ra1");
        first.bench(Team.BLACK).add(new Pawn(Team.BLACK));
        Game game = new Game(first);
        assertThrows(IllegalMoveException.class, () -> game.execute(new TradeAction(pos("a1"), PieceType.PAWN)));
        assertEquals(Team.BLACK, game.currentTurn());
    }

    @Test
    void aPawnMayEnterOnAnyOtherRank() {
        GameState state = stateOf(Team.BLACK, "Kh3", "ke8", "ra2");
        state.bench(Team.BLACK).add(new Pawn(Team.BLACK));
        Game game = new Game(state);
        game.execute(new TradeAction(pos("a2"), PieceType.PAWN));
        assertEquals(PieceType.PAWN, game.board().pieceAt(pos("a2")).orElseThrow().type());
        assertEquals(List.of(PieceType.ROOK), types(game.bench(Team.BLACK)));
    }

    @Test
    void aTeamInCheckCannotTrade() {
        GameState state = stateOf(Team.WHITE, "Ke1", "Pa4", "re8", "ka8");
        state.bench(Team.WHITE).add(new Queen(Team.WHITE));
        assertTrue(rejected(state, new TradeAction(pos("a4"), PieceType.QUEEN)).getMessage().contains("check"));
    }

    // ---- Deshacer y rehacer ----------------------------------------------------------------------------

    @Test
    void undoRevertsTheTradeAndGivesTheTradeBack() {
        Game game = gameWithWhiteQueenOnTheBench();
        game.execute(new TradeAction(pos("e4"), PieceType.QUEEN));
        assertTrue(game.undo());

        assertEquals(PieceType.PAWN, game.board().pieceAt(pos("e4")).orElseThrow().type());
        assertEquals(List.of(PieceType.QUEEN), types(game.bench(Team.WHITE)));
        assertFalse(TradeAction.hasTraded(game, Team.WHITE));
        assertEquals(Team.WHITE, game.currentTurn());

        game.execute(new TradeAction(pos("e4"), PieceType.QUEEN));
        assertTrue(TradeAction.hasTraded(game, Team.WHITE));
    }

    @Test
    void redoAppliesTheSameTradeAgain() {
        Game game = gameWithWhiteQueenOnTheBench();
        game.execute(new TradeAction(pos("e4"), PieceType.QUEEN));
        game.undo();
        assertTrue(game.redo());
        assertEquals(PieceType.QUEEN, game.board().pieceAt(pos("e4")).orElseThrow().type());
        assertTrue(TradeAction.hasTraded(game, Team.WHITE));
        assertEquals(Team.BLACK, game.currentTurn());
    }

    @Test
    void theBenchGrowsWithCapturesAndFeedsTheTrade() {
        // Flujo completo: una captura deja una pieza en el banco y luego se la puede cambiar por otra.
        Game game = new Game();
        play(game, "e2e4", "d7d5", "e4d5", "g8f6");
        assertEquals(List.of(PieceType.PAWN), types(game.bench(Team.BLACK)));
        play(game, "d5d6");
        game.execute(new TradeAction(pos("c7"), PieceType.PAWN));
        assertTrue(TradeAction.hasTraded(game, Team.BLACK));
        assertEquals(List.of(PieceType.PAWN), types(game.bench(Team.BLACK)),
                "the pawn that left the board is now the one on the bench");
    }
}
