package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Team;

import java.util.Optional;

/**
 * Answers "is this square attacked by that team?". It works from the target square outwards using plain
 * geometry (it does not call the {@link MoveRule}s), so castling can use it without recursion.
 * Being attacked ignores whether the attacker is pinned: a pinned piece still gives check.
 */
public final class AttackDetector {

    private AttackDetector() {
    }

    public static boolean isAttacked(Board board, Position target, Team attacker) {
        return attackedByPawn(board, target, attacker)
                || attackedByStepper(board, target, attacker, Directions.KNIGHT, PieceType.KNIGHT)
                || attackedByStepper(board, target, attacker, Directions.ALL, PieceType.KING)
                || attackedBySlider(board, target, attacker, Directions.ORTHOGONAL, PieceType.ROOK)
                || attackedBySlider(board, target, attacker, Directions.DIAGONAL, PieceType.BISHOP);
    }

    private static boolean attackedByPawn(Board board, Position target, Team attacker) {
        // An attacking pawn stands one rank "behind" the target from the attacker's point of view.
        int pawnRank = target.rank() - attacker.forward();
        for (int fileStep : new int[] {-1, 1}) {
            Optional<Position> source = Position.tryOf(target.file() + fileStep, pawnRank);
            if (source.isPresent() && isPiece(board, source.get(), attacker, PieceType.PAWN)) {
                return true;
            }
        }
        return false;
    }

    private static boolean attackedByStepper(Board board, Position target, Team attacker,
                                             int[][] offsets, PieceType type) {
        for (int[] offset : offsets) {
            Optional<Position> source = target.offset(offset[0], offset[1]);
            if (source.isPresent() && isPiece(board, source.get(), attacker, type)) {
                return true;
            }
        }
        return false;
    }

    /** A queen counts as both a rook-type and a bishop-type slider. */
    private static boolean attackedBySlider(Board board, Position target, Team attacker,
                                            int[][] directions, PieceType sliderType) {
        for (int[] direction : directions) {
            int file = target.file() + direction[0];
            int rank = target.rank() + direction[1];
            while (Position.inBounds(file, rank)) {
                Optional<Piece> occupant = board.pieceAt(new Position(file, rank));
                if (occupant.isPresent()) {
                    Piece piece = occupant.get();
                    if (piece.team() == attacker
                            && (piece.type() == sliderType || piece.type() == PieceType.QUEEN)) {
                        return true;
                    }
                    break;
                }
                file += direction[0];
                rank += direction[1];
            }
        }
        return false;
    }

    private static boolean isPiece(Board board, Position position, Team team, PieceType type) {
        return board.pieceAt(position).filter(p -> p.team() == team && p.type() == type).isPresent();
    }
}
