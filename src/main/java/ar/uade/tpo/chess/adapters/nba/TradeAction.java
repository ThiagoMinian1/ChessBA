package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.game.ChessGame;
import ar.uade.tpo.chess.core.game.GameEvent;
import ar.uade.tpo.chess.core.game.GameState;
import ar.uade.tpo.chess.core.game.IllegalMoveException;
import ar.uade.tpo.chess.core.game.TurnAction;
import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Team;
import ar.uade.tpo.chess.core.rules.MoveGenerator;

import java.util.List;
import java.util.Objects;

/**
 * Mechanic 2 - trade / substitution (pattern: Command).
 *
 * <p>Once per game, each team may swap one of its pieces on the board (never the king) for one of its own
 * pieces on the bench: the bench piece takes the square, and the piece that was on the board goes to the bench.
 * It uses up the team's turn. It is a {@link TurnAction} like a normal move, so the core's undo/redo handles it
 * with no special case.
 *
 * <p>Decisions, so they can be defended:
 * <ul>
 *   <li>The bench is every piece of the team that is off the board: captured pieces and traded-out pieces.</li>
 *   <li>A trade is not allowed while the team's own king is in check (the incoming piece takes the very square
 *       of the outgoing one, so it can never block or capture anything new). It can, however, give check.</li>
 *   <li>A pawn cannot enter on the first or last rank.</li>
 *   <li>The incoming piece counts as having moved, so a rook or king brought in has no castling rights.</li>
 *   <li>A trade is not a capture, but it resets the fifty-move counter and the repetition history, because
 *       the position can no longer be reached by moves alone.</li>
 * </ul>
 *
 * <p>Mandatory TP rules touched: none of the standard rules are changed, but the action adds a way of changing
 * the board that is not a piece movement. It is opt-in: only an adapter that offers it can submit it.
 */
public final class TradeAction implements TurnAction {

    static final String KEY = "nba.trade";

    private final Position square;
    private final PieceType benchPiece;

    /**
     * @param square     where the team's own piece on the board stands (it leaves the board)
     * @param benchPiece which kind of piece from the bench comes in on that square
     */
    public TradeAction(Position square, PieceType benchPiece) {
        this.square = Objects.requireNonNull(square, "square");
        this.benchPiece = Objects.requireNonNull(benchPiece, "benchPiece");
    }

    @Override
    public void execute(GameState state) {
        Team team = state.turn();
        TradeLedger ledger = state.extension(KEY, TradeLedger.class).orElseGet(TradeLedger::fresh);
        if (ledger.hasUsed(team)) {
            throw new IllegalMoveException(team + " has already used its trade");
        }

        Board board = state.board();
        Piece leaving = board.pieceAt(square)
                .filter(p -> p.team() == team)
                .orElseThrow(() -> new IllegalMoveException("There is no " + team + " piece on " + square));
        if (leaving.type() == PieceType.KING) {
            throw new IllegalMoveException("The king cannot be traded");
        }

        List<Piece> bench = state.bench(team);
        Piece entering = bench.stream()
                .filter(p -> p.type() == benchPiece)
                .findFirst()
                .orElseThrow(() -> new IllegalMoveException(team + " has no " + benchPiece + " on the bench"));
        if (entering.type() == PieceType.PAWN && (square.rank() == 0 || square.rank() == Position.SIZE - 1)) {
            throw new IllegalMoveException("A pawn cannot enter on the first or last rank");
        }

        Piece incoming = entering.withMoved(true);
        Board trial = board.copy();
        trial.place(square, incoming);
        if (MoveGenerator.isInCheck(trial, team)) {
            throw new IllegalMoveException("The trade would leave the " + team + " king in check");
        }

        board.place(square, incoming);
        bench.remove(entering);
        bench.add(leaving);
        state.putExtension(KEY, ledger.markUsed(team));
        state.setHalfmoveClock(0);
        state.clearPositionHistory();
        state.setEnPassantTarget(null);
        state.setTurn(team.opposite());
        state.emit(new GameEvent.Notice(
                team + " trades " + leaving.type() + " on " + square + " for " + benchPiece + " from the bench"));
    }

    @Override
    public String describe() {
        return "Trade " + square + " for bench " + benchPiece;
    }

    /** Whether {@code team} has already used its trade in this game. */
    public static boolean hasTraded(ChessGame game, Team team) {
        return game.extension(KEY, TradeLedger.class).orElseGet(TradeLedger::fresh).hasUsed(team);
    }
}
