package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.MoveType;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Square;
import ar.uade.tpo.chess.core.model.Team;
import ar.uade.tpo.chess.core.rules.MoveRuleFactory;
import ar.uade.tpo.chess.ports.GameObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Orchestrates a single chess game: whose turn it is, which moves are
 * legal, applying a move, and noticing check / checkmate / stalemate.
 * This class has no dependency on any framework or UI — it can be
 * exercised entirely with plain JUnit tests.
 */
public final class Game {

    private final Board board;
    private final List<GameObserver> observers = new ArrayList<>();
    private final List<Move> history = new ArrayList<>();
    private Team currentTurn;
    private GameStatus status = GameStatus.IN_PROGRESS;

    public Game() {
        this(Board.standard(), Team.WHITE);
    }

    /** Lets tests (or an adapter restoring a saved game) start from a specific position. */
    public Game(Board board, Team startingTurn) {
        this.board = board;
        this.currentTurn = startingTurn;
    }

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    public Board getBoard() {
        return board;
    }

    public Team getCurrentTurn() {
        return currentTurn;
    }

    public GameStatus getStatus() {
        return status;
    }

    public List<Move> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public boolean isInCheck(Team team) {
        return CheckDetector.isInCheck(board, team);
    }

    /**
     * Pseudo-legal moves of the piece on {@code from}, filtered down to
     * the ones that do not leave the mover's own king in check. Returns
     * an empty list if there is no piece there, it isn't that piece's
     * team's turn, or the game has already ended.
     */
    public List<Position> legalMoves(Position from) {
        if (status != GameStatus.IN_PROGRESS || board.isEmpty(from)) {
            return List.of();
        }
        Piece piece = board.pieceAt(from);
        if (piece.getTeam() != currentTurn) {
            return List.of();
        }

        List<Position> pseudoLegal = MoveRuleFactory.ruleFor(piece.getType()).possibleMoves(from, board);
        List<Position> legal = new ArrayList<>();
        for (Position candidate : pseudoLegal) {
            Board simulation = board.copy();
            simulation.applyMove(from, candidate);
            if (!CheckDetector.isInCheck(simulation, piece.getTeam())) {
                legal.add(candidate);
            }
        }
        return legal;
    }

    public void move(Position from, Position to) {
        if (status != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("The game has already ended: " + status);
        }
        if (!legalMoves(from).contains(to)) {
            throw new IllegalArgumentException("Illegal move from " + from + " to " + to);
        }

        Piece movingPiece = board.pieceAt(from);
        MoveType type = board.isEmpty(to) ? MoveType.NORMAL : MoveType.CAPTURE;
        Piece capturedPiece = board.applyMove(from, to);

        Move move = new Move(from, to, movingPiece, capturedPiece, type);
        history.add(move);
        notifyMove(move);
        if (capturedPiece != null) {
            notifyCapture(capturedPiece);
        }

        currentTurn = currentTurn.opponent();
        updateStatusAfterMove();
    }

    private void updateStatusAfterMove() {
        boolean inCheck = CheckDetector.isInCheck(board, currentTurn);
        boolean anyLegalMove = hasAnyLegalMove();

        if (inCheck && !anyLegalMove) {
            status = GameStatus.CHECKMATE;
            notifyGameEnded(status);
        } else if (!inCheck && !anyLegalMove) {
            status = GameStatus.STALEMATE;
            notifyGameEnded(status);
        } else {
            status = GameStatus.IN_PROGRESS;
            if (inCheck) {
                notifyCheck(currentTurn);
            }
        }
    }

    private boolean hasAnyLegalMove() {
        for (Square square : board.squaresWithPieceOf(currentTurn)) {
            if (!legalMoves(square.getPosition()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void notifyMove(Move move) {
        for (GameObserver observer : observers) {
            observer.onMove(move);
        }
    }

    private void notifyCapture(Piece capturedPiece) {
        for (GameObserver observer : observers) {
            observer.onCapture(capturedPiece);
        }
    }

    private void notifyCheck(Team teamInCheck) {
        for (GameObserver observer : observers) {
            observer.onCheck(teamInCheck);
        }
    }

    private void notifyGameEnded(GameStatus result) {
        for (GameObserver observer : observers) {
            observer.onGameEnded(result);
        }
    }
}
