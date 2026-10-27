package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.MoveType;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Team;
import ar.uade.tpo.chess.core.rules.MoveApplier;
import ar.uade.tpo.chess.core.rules.MoveGenerator;
import ar.uade.tpo.chess.core.rules.MoveOutcome;

import java.util.Objects;

/**
 * The standard chess action: play one legal move and pass the turn to the other side.
 * This is where turn alternation lives in the core.
 */
public final class MoveAction implements TurnAction {

    private final Move move;

    public MoveAction(Move move) {
        this.move = Objects.requireNonNull(move, "move");
    }

    public Move move() {
        return move;
    }

    @Override
    public void execute(GameState state) {
        Team mover = state.turn();
        boolean legal = MoveGenerator
                .legalMoves(state.board(), mover, state.enPassantTarget().orElse(null))
                .contains(move);
        if (!legal) {
            throw new IllegalMoveException("Illegal move for " + mover + ": " + move);
        }

        MoveOutcome outcome = MoveApplier.apply(state.board(), move);

        boolean capture = outcome.isCapture();
        boolean pawnMove = outcome.movingPiece().type() == PieceType.PAWN;
        state.setHalfmoveClock(capture || pawnMove ? 0 : state.halfmoveClock() + 1);
        state.setEnPassantTarget(move.type() == MoveType.PAWN_DOUBLE_PUSH ? skippedSquare() : null);
        outcome.captured().ifPresent(piece -> state.bench(piece.team()).add(piece));
        state.setLastMove(new MoveRecord(mover, move, outcome.movingPiece().type(), capture));
        state.setTurn(mover.opposite());

        state.emit(new GameEvent.MovePlayed(mover, move, outcome.movingPiece().type()));
        outcome.captured().ifPresent(piece ->
                state.emit(new GameEvent.PieceCaptured(mover, piece, outcome.capturedAt().orElse(move.to()))));
        if (move.isPromotion()) {
            state.emit(new GameEvent.PiecePromoted(mover, move.to(), move.promotion()));
        }
    }

    /** The square a double-pushing pawn passed over: same file, halfway between the two ranks. */
    private Position skippedSquare() {
        return new Position(move.from().file(), (move.from().rank() + move.to().rank()) / 2);
    }

    @Override
    public String describe() {
        return move.toString();
    }
}
