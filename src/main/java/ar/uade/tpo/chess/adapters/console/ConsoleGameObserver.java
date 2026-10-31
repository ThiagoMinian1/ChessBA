package ar.uade.tpo.chess.adapters.console;

import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.MoveType;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Team;
import ar.uade.tpo.chess.ports.GameObserver;
import ar.uade.tpo.chess.ports.PieceTheme;

import java.io.PrintStream;
import java.util.Optional;

/** {@link GameObserver} que relata la partida en un {@link PrintStream} con el {@link PieceTheme} activo. */
public final class ConsoleGameObserver implements GameObserver {

    private final PrintStream out;
    private final PieceTheme theme;

    public ConsoleGameObserver(PrintStream out, PieceTheme theme) {
        this.out = out;
        this.theme = theme;
    }

    @Override
    public void onMovePlayed(Team team, Move move, PieceType piece) {
        String who = theme.pieceName(piece, team);
        if (move.type() == MoveType.CASTLE_KINGSIDE) {
            out.println(theme.teamName(team) + " castles kingside (" + move + ")");
        } else if (move.type() == MoveType.CASTLE_QUEENSIDE) {
            out.println(theme.teamName(team) + " castles queenside (" + move + ")");
        } else {
            out.println(who + " moves " + move.from() + " -> " + move.to());
        }
    }

    @Override
    public void onPieceCaptured(Team capturer, Piece captured, Position at) {
        out.println("  " + theme.pieceName(captured.type(), captured.team()) + " is captured on " + at);
    }

    @Override
    public void onPiecePromoted(Team team, Position at, PieceType promotedTo) {
        out.println("  Promotion on " + at + ": the pawn becomes " + theme.pieceName(promotedTo, team));
    }

    @Override
    public void onCheck(Team checkedTeam) {
        out.println("  Check! " + theme.teamName(checkedTeam) + " is in check.");
    }

    @Override
    public void onGameEnded(GameStatus status, Optional<Team> winner) {
        out.println();
        out.println("*** " + describeResult(status, winner) + " ***");
    }

    /** Las mecánicas usan nombres de bando neutros en sus mensajes; acá se muestran los del tema activo. */
    @Override
    public void onNotice(String message) {
        String themed = message
                .replace(Team.WHITE.name(), theme.teamName(Team.WHITE))
                .replace(Team.BLACK.name(), theme.teamName(Team.BLACK));
        out.println("  * " + themed);
    }

    private String describeResult(GameStatus status, Optional<Team> winner) {
        String winnerName = winner.map(theme::teamName).orElse("nobody");
        return switch (status) {
            case CHECKMATE -> "Checkmate! " + winnerName + " wins";
            case RESIGNATION -> "Resignation: " + winnerName + " wins";
            case STALEMATE -> "Draw by stalemate";
            case DRAW_THREEFOLD_REPETITION -> "Draw by threefold repetition";
            case DRAW_FIFTY_MOVE_RULE -> "Draw by the fifty-move rule";
            case DRAW_INSUFFICIENT_MATERIAL -> "Draw by insufficient material";
            case DRAW_BY_AGREEMENT -> "Draw by agreement";
            case ONGOING -> "Game in progress";
        };
    }
}
