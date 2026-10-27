package ar.uade.tpo.chess.adapters.console;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.Position;

/**
 * Draws the board as plain ASCII from white's side: uppercase letters for white, lowercase for black,
 * {@code .} for an empty square. ASCII on purpose: it renders the same in every terminal. The themed piece
 * names are shown separately, in a legend.
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
