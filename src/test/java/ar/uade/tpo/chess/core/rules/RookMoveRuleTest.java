package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Pawn;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Rook;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RookMoveRuleTest {

    private final RookMoveRule rule = new RookMoveRule();

    @Test
    void onAnEmptyBoardARookOnTheCornerReachesTheWholeRankAndFile() {
        Board board = Board.empty();
        Position from = new Position(0, 0);
        board.placePiece(from, new Rook(Team.WHITE));

        List<Position> moves = rule.possibleMoves(from, board);

        assertEquals(14, moves.size());
    }

    @Test
    void aRookStopsBeforeAFriendlyPiece() {
        Board board = Board.empty();
        Position from = new Position(0, 0);
        board.placePiece(from, new Rook(Team.WHITE));
        board.placePiece(new Position(3, 0), new Pawn(Team.WHITE));

        List<Position> moves = rule.possibleMoves(from, board);

        assertTrue(moves.contains(new Position(2, 0)));
        assertFalse(moves.contains(new Position(3, 0)));
        assertFalse(moves.contains(new Position(4, 0)));
    }

    @Test
    void aRookCanCaptureAnEnemyPieceButNotJumpOverIt() {
        Board board = Board.empty();
        Position from = new Position(0, 0);
        board.placePiece(from, new Rook(Team.WHITE));
        board.placePiece(new Position(3, 0), new Pawn(Team.BLACK));

        List<Position> moves = rule.possibleMoves(from, board);

        assertTrue(moves.contains(new Position(3, 0)));
        assertFalse(moves.contains(new Position(4, 0)));
    }
}
