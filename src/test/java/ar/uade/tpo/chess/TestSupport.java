package ar.uade.tpo.chess;

import ar.uade.tpo.chess.core.game.ChessGame;
import ar.uade.tpo.chess.core.game.Game;
import ar.uade.tpo.chess.core.game.GameState;
import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.MoveType;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.PieceFactory;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Team;
import ar.uade.tpo.chess.core.rules.MoveApplier;
import ar.uade.tpo.chess.core.rules.MoveContext;
import ar.uade.tpo.chess.core.rules.MoveGenerator;
import ar.uade.tpo.chess.core.rules.MoveRuleFactory;
import ar.uade.tpo.chess.ports.GameObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/** Ayudantes compartidos por las pruebas. No forman parte del producto. */
public final class TestSupport {

    private TestSupport() {
    }

    public static Position pos(String algebraic) {
        return Position.of(algebraic);
    }

    /**
     * Crea un tablero a partir de piezas como {@code "Ke1"} (rey blanco en e1) o {@code "pd5"} (peón negro en d5):
     * las mayúsculas representan blancas y las minúsculas, negras. Todas empiezan sin mover.
     */
    public static Board boardOf(String... placements) {
        Board board = Board.empty();
        for (String placement : placements) {
            char letter = placement.charAt(0);
            PieceType type = PieceType.fromLetter(letter).orElseThrow();
            Team team = Character.isUpperCase(letter) ? Team.WHITE : Team.BLACK;
            board.place(Position.of(placement.substring(1)), PieceFactory.create(type, team));
        }
        return board;
    }

    public static GameState stateOf(Team turn, String... placements) {
        return GameState.of(boardOf(placements), turn);
    }

    public static Game gameOf(Team turn, String... placements) {
        return new Game(stateOf(turn, placements));
    }

    /** Juega movimientos por coordenadas, como {@code "e2e4"} o, para promocionar, {@code "e7e8q"}. */
    public static void play(ChessGame game, String... moves) {
        for (String move : moves) {
            Position from = Position.of(move.substring(0, 2));
            Position to = Position.of(move.substring(2, 4));
            PieceType promotion = move.length() == 5 ? PieceType.fromLetter(move.charAt(4)).orElseThrow() : null;
            game.play(from, to, promotion);
        }
    }

    /** Casillas de destino de los movimientos, ordenadas. */
    public static Set<String> targets(List<Move> moves) {
        Set<String> result = new TreeSet<>();
        for (Move move : moves) {
            result.add(move.to().toString());
        }
        return result;
    }

    public static Set<String> targetsFrom(ChessGame game, String from) {
        return targets(game.legalMovesFrom(pos(from)));
    }

    /** Movimientos pseudo-legales de la pieza en {@code from} (sin filtrar jaques ni incluir capturas al paso). */
    public static List<Move> pseudoMoves(Board board, String from) {
        Position at = Position.of(from);
        Piece piece = board.pieceAt(at).orElseThrow();
        return MoveRuleFactory.forPiece(piece).pseudoLegalMoves(MoveContext.of(board), at, piece);
    }

    public static List<Move> pseudoMoves(Board board, String from, String enPassantTarget) {
        Position at = Position.of(from);
        Piece piece = board.pieceAt(at).orElseThrow();
        return MoveRuleFactory.forPiece(piece)
                .pseudoLegalMoves(MoveContext.of(board, Position.of(enPassantTarget)), at, piece);
    }

    public static Set<String> pseudoTargets(Board board, String from) {
        return targets(pseudoMoves(board, from));
    }

    /** Registra en orden cada evento recibido como una cadena breve. */
    public static final class Recorder implements GameObserver {

        public final List<String> events = new ArrayList<>();

        @Override
        public void onMovePlayed(Team team, Move move, PieceType piece) {
            events.add("move " + move);
        }

        @Override
        public void onPieceCaptured(Team capturer, Piece captured, Position at) {
            events.add("capture " + captured.type() + "@" + at);
        }

        @Override
        public void onPiecePromoted(Team team, Position at, PieceType promotedTo) {
            events.add("promote " + at + "=" + promotedTo);
        }

        @Override
        public void onCheck(Team checkedTeam) {
            events.add("check " + checkedTeam);
        }

        @Override
        public void onGameEnded(GameStatus status, Optional<Team> winner) {
            events.add("end " + status + " " + winner.map(Enum::name).orElse("-"));
        }

        @Override
        public void onNotice(String message) {
            events.add("notice " + message);
        }
    }

    // ---- Perft ---------------------------------------------------------------------------------------

    /** Crea un estado a partir de una cadena FEN (piezas, turno, derechos de enroque y casilla de captura al paso). */
    public static GameState fromFen(String fen) {
        String[] fields = fen.split(" ");
        String castling = fields[2];
        Board board = Board.empty();
        String[] rows = fields[0].split("/");
        for (int row = 0; row < 8; row++) {
            int rank = 7 - row;
            int file = 0;
            for (char c : rows[row].toCharArray()) {
                if (Character.isDigit(c)) {
                    file += c - '0';
                    continue;
                }
                PieceType type = PieceType.fromLetter(c).orElseThrow();
                Team team = Character.isUpperCase(c) ? Team.WHITE : Team.BLACK;
                board.place(new Position(file, rank), PieceFactory.create(type, team, hasMoved(type, team, file, rank, castling)));
                file++;
            }
        }
        Team turn = fields[1].equals("w") ? Team.WHITE : Team.BLACK;
        GameState state = GameState.of(board, turn);
        state.setEnPassantTarget(fields[3].equals("-") ? null : Position.of(fields[3]));
        return state;
    }

    /** El rey o la torre cuentan como movidos salvo que la FEN les otorgue el derecho a enrocar correspondiente. */
    private static boolean hasMoved(PieceType type, Team team, int file, int rank, String castling) {
        boolean kingside = castling.contains(team == Team.WHITE ? "K" : "k");
        boolean queenside = castling.contains(team == Team.WHITE ? "Q" : "q");
        if (type == PieceType.KING) {
            return !(kingside || queenside);
        }
        if (type == PieceType.ROOK && rank == team.homeRank()) {
            if (file == 7) {
                return !kingside;
            }
            if (file == 0) {
                return !queenside;
            }
        }
        return false;
    }

    /** Cuenta las posiciones finales del árbol de jugadas legales hasta la profundidad indicada. */
    public static long perft(Board board, Team turn, Position enPassant, int depth) {
        List<Move> moves = MoveGenerator.legalMoves(board, turn, enPassant);
        if (depth == 1) {
            return moves.size();
        }
        long total = 0;
        for (Move move : moves) {
            Board next = board.copy();
            MoveApplier.apply(next, move);
            Position nextEnPassant = move.type() == MoveType.PAWN_DOUBLE_PUSH
                    ? new Position(move.from().file(), (move.from().rank() + move.to().rank()) / 2)
                    : null;
            total += perft(next, turn.opposite(), nextEnPassant, depth - 1);
        }
        return total;
    }

    public static long perft(String fen, int depth) {
        GameState state = fromFen(fen);
        return perft(state.board(), state.turn(), state.enPassantTarget().orElse(null), depth);
    }
}
