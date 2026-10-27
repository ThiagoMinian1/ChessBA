package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Position;

import java.util.Objects;
import java.util.Optional;

/** Plain value implementation of {@link MoveContext}; {@code target} is the en passant target or null. */
record BasicMoveContext(Board board, Position target) implements MoveContext {

    BasicMoveContext {
        Objects.requireNonNull(board, "board");
    }

    @Override
    public Optional<Position> enPassantTarget() {
        return Optional.ofNullable(target);
    }
}
