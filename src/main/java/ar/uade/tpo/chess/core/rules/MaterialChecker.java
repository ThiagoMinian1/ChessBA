package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.PlacedPiece;

import java.util.List;

/**
 * Detecta las posiciones de «material insuficiente», en las que ninguna secuencia de jugadas legales puede dar
 * jaque mate. Según chess.com, son: rey contra rey; rey y una pieza menor (alfil o caballo) contra rey; y
 * posiciones donde, aparte de los reyes, solo quedan alfiles ubicados en casillas del mismo color.
 * Esto incluye rey y alfil contra rey y alfil cuando ambos alfiles son del mismo color.
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
