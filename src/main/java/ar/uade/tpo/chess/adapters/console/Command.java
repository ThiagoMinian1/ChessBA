package ar.uade.tpo.chess.adapters.console;

import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Position;

/** What a line typed in the console means. Parsing produces one of these; running them is the runner's job. */
public sealed interface Command {

    /** A move in coordinates; {@code promotion} is null unless the player already chose a piece (e.g. "e7 e8 q"). */
    record MoveCommand(Position from, Position to, PieceType promotion) implements Command {
    }

    record Undo() implements Command {
    }

    record Redo() implements Command {
    }

    /** Trade: the own piece on {@code square} goes to the bench and a bench piece of type {@code benchPiece} comes in. */
    record Trade(Position square, PieceType benchPiece) implements Command {
    }

    record Bench() implements Command {
    }

    record Resign() implements Command {
    }

    record Draw() implements Command {
    }

    record Help() implements Command {
    }

    record Quit() implements Command {
    }

    /** The line could not be understood; {@code reason} is shown to the player. */
    record Invalid(String reason) implements Command {
    }
}
