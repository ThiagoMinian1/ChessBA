package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.PlacedPiece;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Team;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns pseudo-legal moves into <em>legal</em> ones: a move is legal when, after making it, the mover's own
 * king is not attacked. This one filter covers pins, moving into check, and having to answer a check.
 */
public final class MoveGenerator {

    private MoveGenerator() {
    }

    public static List<Move> legalMoves(Board board, Team team, Position enPassantTarget) {
        List<Move> legal = new ArrayList<>();
        for (PlacedPiece placed : board.pieces(team)) {
            collectLegalMoves(board, team, enPassantTarget, placed, legal, false);
        }
        return legal;
    }

    public static List<Move> legalMovesFrom(Board board, Team team, Position enPassantTarget, Position from) {
        List<Move> legal = new ArrayList<>();
        board.pieceAt(from)
                .filter(piece -> piece.team() == team)
                .ifPresent(piece -> collectLegalMoves(
                        board, team, enPassantTarget, new PlacedPiece(from, piece), legal, false));
        return legal;
    }

    /** Cheaper than {@code !legalMoves(...).isEmpty()}: stops at the first legal move found. */
    public static boolean hasLegalMove(Board board, Team team, Position enPassantTarget) {
        List<Move> found = new ArrayList<>();
        for (PlacedPiece placed : board.pieces(team)) {
            collectLegalMoves(board, team, enPassantTarget, placed, found, true);
            if (!found.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** True if {@code team}'s king is attacked. A board with no king for that team is never "in check". */
    public static boolean isInCheck(Board board, Team team) {
        return board.findKing(team)
                .map(king -> AttackDetector.isAttacked(board, king, team.opposite()))
                .orElse(false);
    }

    private static void collectLegalMoves(Board board, Team team, Position enPassantTarget,
                                          PlacedPiece placed, List<Move> into, boolean stopAtFirst) {
        MoveContext context = MoveContext.of(board, enPassantTarget);
        List<Move> candidates = MoveRuleFactory.forPiece(placed.piece())
                .pseudoLegalMoves(context, placed.position(), placed.piece());
        for (Move move : candidates) {
            if (!leavesKingInCheck(board, team, move)) {
                into.add(move);
                if (stopAtFirst) {
                    return;
                }
            }
        }
    }

    private static boolean leavesKingInCheck(Board board, Team team, Move move) {
        Board trial = board.copy();
        MoveApplier.apply(trial, move);
        return isInCheck(trial, team);
    }
}
