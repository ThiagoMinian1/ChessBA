package ar.uade.tpo.chess.adapters.console;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.Position;

/**
 * Dibuja el tablero en ASCII desde el lado de las blancas: mayúsculas para blancas, minúsculas para negras
 * y {@code .} para las casillas vacías. Se usa ASCII para que se vea igual en cualquier terminal. Los nombres
 * del tema elegido aparecen aparte, en una leyenda.
 */
public final class ConsoleBoardRenderer {

    public String render(Board board) {
        StringBuilder out = new StringBuilder();
        for (int rank = Position.SIZE - 1; rank >= 0; rank--) {
            out.append(rank + 1).append("  ");
            for (int file = 0; file < Position.SIZE; file++) {
                Piece piece = board.pieceAt(new Position(file, rank)).orElse(null);
                out.append(piece == null ? '.' : piece.symbol());
                if (file < Position.SIZE - 1) {
                    out.append(' ');
                }
            }
            out.append('\n');
        }
        out.append("\n   a b c d e f g h");
        return out.toString();
    }
}
