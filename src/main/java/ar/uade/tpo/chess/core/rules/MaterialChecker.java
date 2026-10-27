package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.PlacedPiece;

import java.util.List;

/**
 * Detects "insufficient material": positions in which no sequence of legal moves can produce checkmate.
 * Following chess.com, these are: king vs king; king and one minor piece (bishop or knight) vs king; and
 * positions where the only pieces besides the kings are bishops that all stand on squares of the same colour
 * (which includes king and bishop vs king and bishop with same-coloured bishops).
 */
public final class MaterialChecker {

    private MaterialChecker() {
    }

    public static boolean isInsufficientMaterial(Board board) {
        List<PlacedPiece> others = board.pieces().stream()
                .filter(p -> p.piece().type() != PieceType.KING)
                .toList();

        if (others.isEmpty()) {
            return true;
        }
        if (others.size() == 1) {
            PieceType type = others.get(0).piece().type();
            return type == PieceType.BISHOP || type == PieceType.KNIGHT;
        }
        boolean onlyBishops = others.stream().allMatch(p -> p.piece().type() == PieceType.BISHOP);
        if (!onlyBishops) {
            return false;
        }
        boolean firstOnLight = others.get(0).position().isLightSquare();
        return others.stream().allMatch(p -> p.position().isLightSquare() == firstOnLight);
    }
}
