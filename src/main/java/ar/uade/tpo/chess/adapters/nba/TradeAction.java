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
 * Mecánica 2: intercambio o sustitución (patrón Command).
 *
 * <p>Una vez por partida, cada bando puede cambiar una pieza propia del tablero (excepto el rey) por una de las
 * piezas propias del banco. La pieza del banco ocupa la casilla y la otra pasa al banco. El intercambio consume
 * el turno y, al ser una {@link TurnAction}, se puede deshacer y rehacer como cualquier otra acción.
 *
 * <p>Detalles de la mecánica:
 * <ul>
 *   <li>El banco contiene todas las piezas del bando que están fuera del tablero, tanto las capturadas como las intercambiadas.</li>
 *   <li>No se puede intercambiar si el rey propio está en jaque: la pieza que entra ocupa la misma casilla que la
 *       que sale, así que no puede bloquear ni capturar otra pieza. Sí puede dar jaque.</li>
 *   <li>Un peón no puede entrar en la primera ni en la última fila.</li>
 *   <li>La pieza que entra cuenta como movida; por eso una torre o un rey no conserva el derecho a enrocar.</li>
 *   <li>El intercambio no cuenta como captura, pero reinicia el contador de cincuenta movimientos y el historial
 *       de repetición, porque la posición ya no puede alcanzarse solo con movimientos.</li>
 * </ul>
 *
 * <p>No modifica las reglas estándar, pero permite cambiar el tablero sin mover una pieza. Es opcional: solo
 * puede ejecutarse desde un adaptador que ofrezca esta mecánica.
 */
public final class TradeAction implements TurnAction {

    static final String KEY = "nba.trade";

    private final Position square;
    private final PieceType benchPiece;

    /**
     * @param square     casilla de la pieza propia que está en el tablero y se retira
     * @param benchPiece tipo de pieza del banco que ocupará esa casilla
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

    /** Indica si {@code team} ya usó su intercambio en esta partida. */
    public static boolean hasTraded(ChessGame game, Team team) {
        return game.extension(KEY, TradeLedger.class).orElseGet(TradeLedger::fresh).hasUsed(team);
    }
}
