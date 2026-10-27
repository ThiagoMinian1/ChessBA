package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.TestSupport;
import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * "Perft" counts every legal line of play to a fixed depth. The numbers below are the well-known reference
 * values for these positions, so matching them is strong evidence that move generation (castling, en passant,
 * promotion, pins and checks included) is exactly right.
 */
class PerftTest {

    @Test
    void startingPosition() {
        Board board = Board.standard();
        assertEquals(20, TestSupport.perft(board, Team.WHITE, null, 1));
        assertEquals(400, TestSupport.perft(board, Team.WHITE, null, 2));
        assertEquals(8902, TestSupport.perft(board, Team.WHITE, null, 3));
        assertEquals(197281, TestSupport.perft(board, Team.WHITE, null, 4));
    }

    @Test
    void kiwipeteStressesCastlingPinsAndEnPassant() {
        String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
        assertEquals(48, TestSupport.perft(fen, 1));
        assertEquals(2039, TestSupport.perft(fen, 2));
        assertEquals(97862, TestSupport.perft(fen, 3));
    }

    @Test
    void endgameWithPinsAndEnPassantDiscoveredChecks() {
        String fen = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1";
        assertEquals(14, TestSupport.perft(fen, 1));
        assertEquals(191, TestSupport.perft(fen, 2));
        assertEquals(2812, TestSupport.perft(fen, 3));
        assertEquals(43238, TestSupport.perft(fen, 4));
    }

    @Test
    void promotionsAndCastlingWhileInCheck() {
        String fen = "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1";
        assertEquals(6, TestSupport.perft(fen, 1));
        assertEquals(264, TestSupport.perft(fen, 2));
        assertEquals(9467, TestSupport.perft(fen, 3));
    }

    @Test
    void promotionAndCheckEvasion() {
        String fen = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        assertEquals(44, TestSupport.perft(fen, 1));
        assertEquals(1486, TestSupport.perft(fen, 2));
        assertEquals(62379, TestSupport.perft(fen, 3));
    }

    @Test
    void middlegameWithoutCastlingRights() {
        String fen = "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10";
        assertEquals(46, TestSupport.perft(fen, 1));
        assertEquals(2079, TestSupport.perft(fen, 2));
        assertEquals(89890, TestSupport.perft(fen, 3));
    }
}
