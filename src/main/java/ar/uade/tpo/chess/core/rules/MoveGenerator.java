package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.PlacedPiece;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Team;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtra los movimientos pseudo-legales para quedarse con los legales: después de mover, el rey propio no debe
 * quedar bajo ataque. Este filtro contempla las piezas clavadas, las jugadas que dejan al rey en jaque y las
 * respuestas necesarias cuando el rey ya está en jaque.
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

    /** Más eficiente que {@code !legalMoves(...).isEmpty()}: se detiene al encontrar una jugada legal. */
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

    /** Indica si el rey de {@code team} está bajo ataque. Si el bando no tiene rey, no está en jaque. */
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
