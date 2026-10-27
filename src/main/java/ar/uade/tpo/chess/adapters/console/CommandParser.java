package ar.uade.tpo.chess.adapters.console;

import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Position;

import java.util.Locale;
import java.util.Optional;

/**
 * Turns a typed line into a {@link Command}.
 *
 * <p>Moves are plain coordinates, with or without a space: {@code e2 e4}, {@code e2e4}. A promotion piece may be
 * appended: {@code e7 e8 q}, {@code e7e8q} or {@code e7e8=q}. Other commands: {@code undo}, {@code redo},
 * {@code trade <square> <piece>}, {@code bench}, {@code resign}, {@code draw}, {@code help}, {@code quit}.
 */
public final class CommandParser {

    private CommandParser() {
    }

    public static Command parse(String line) {
        if (line == null || line.isBlank()) {
            return new Command.Invalid("Type a move like 'e2 e4', or 'help' for the commands.");
        }
        String[] parts = line.trim().split("\\s+");
        String head = parts[0].toLowerCase(Locale.ROOT);

        switch (head) {
            case "undo":
                return parts.length == 1 ? new Command.Undo() : new Command.Invalid("'undo' takes no arguments.");
            case "redo":
                return parts.length == 1 ? new Command.Redo() : new Command.Invalid("'redo' takes no arguments.");
            case "bench":
                return parts.length == 1 ? new Command.Bench() : new Command.Invalid("'bench' takes no arguments.");
            case "resign":
                return parts.length == 1 ? new Command.Resign() : new Command.Invalid("'resign' takes no arguments.");
            case "draw":
                return parts.length == 1 ? new Command.Draw() : new Command.Invalid("'draw' takes no arguments.");
            case "help":
            case "?":
                return new Command.Help();
            case "quit":
            case "exit":
                return new Command.Quit();
            case "trade":
                return parseTrade(parts);
            default:
                return parseMove(parts);
        }
    }

    private static Command parseTrade(String[] parts) {
        if (parts.length != 3) {
            return new Command.Invalid("Usage: trade <square> <piece from your bench>, e.g. 'trade e4 q'.");
        }
        Position square;
        try {
            square = Position.of(parts[1]);
        } catch (IllegalArgumentException e) {
            return new Command.Invalid("'" + parts[1] + "' is not a square (use a1..h8).");
        }
        Optional<PieceType> piece = parts[2].length() == 1
                ? PieceType.fromLetter(parts[2].charAt(0))
                : Optional.empty();
        if (piece.isEmpty()) {
            return new Command.Invalid("'" + parts[2] + "' is not a piece letter (K, Q, R, B, N or P).");
        }
        return new Command.Trade(square, piece.get());
    }

    private static Command parseMove(String[] parts) {
        String text = String.join("", parts).toLowerCase(Locale.ROOT).replace("=", "");
        if (text.length() != 4 && text.length() != 5) {
            return new Command.Invalid("Unrecognized command '" + String.join(" ", parts)
                    + "'. Type a move like 'e2 e4', or 'help'.");
        }
        Position from;
        Position to;
        try {
            from = Position.of(text.substring(0, 2));
            to = Position.of(text.substring(2, 4));
        } catch (IllegalArgumentException e) {
            return new Command.Invalid("Unrecognized command '" + String.join(" ", parts)
                    + "'. Type a move like 'e2 e4', or 'help'.");
        }
        PieceType promotion = null;
        if (text.length() == 5) {
            Optional<PieceType> chosen = PieceType.fromLetter(text.charAt(4));
            if (chosen.isEmpty() || !chosen.get().isPromotionTarget()) {
                return new Command.Invalid("A pawn can promote to Q, R, B or N only.");
            }
            promotion = chosen.get();
        }
        return new Command.MoveCommand(from, to, promotion);
    }
}
