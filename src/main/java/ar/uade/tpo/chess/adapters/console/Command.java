package ar.uade.tpo.chess.adapters.console;

import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Position;

/** Significado de una línea ingresada en la consola. El análisis crea uno de estos comandos y el runner lo ejecuta. */
public sealed interface Command {

    /** Movimiento por coordenadas; {@code promotion} es null si el jugador todavía no eligió una pieza (por ejemplo, "e7 e8 q"). */
    record MoveCommand(Position from, Position to, PieceType promotion) implements Command {
    }

    record Undo() implements Command {
    }

    record Redo() implements Command {
    }

    /** Intercambio: la pieza propia de {@code square} pasa al banco y entra una pieza del tipo {@code benchPiece}. */
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

    /** No se pudo interpretar la línea; se muestra {@code reason} al jugador. */
    record Invalid(String reason) implements Command {
    }
}
