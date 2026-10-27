package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Team;
import ar.uade.tpo.chess.ports.GameObserver;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Decorator base: forwards everything to the wrapped game. A concrete decorator overrides only
 * {@link #execute(TurnAction)} (typically to wrap the action in one that adds behaviour) and leaves the rest.
 *
 * <p>It deliberately does not override {@code play(...)}: those are default methods that call
 * {@code execute} on {@code this}, so the outermost decorator always sees the action first.
 */
public abstract class GameDecorator implements ChessGame {

    protected final ChessGame delegate;

    protected GameDecorator(ChessGame delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public Team currentTurn() {
        return delegate.currentTurn();
    }

    @Override
    public Board board() {
        return delegate.board();
    }

    @Override
    public GameStatus status() {
        return delegate.status();
    }

    @Override
    public Optional<Team> winner() {
        return delegate.winner();
    }

    @Override
    public boolean isInCheck() {
        return delegate.isInCheck();
    }

    @Override
    public List<Move> legalMoves() {
        return delegate.legalMoves();
    }

    @Override
    public List<Move> legalMovesFrom(Position from) {
        return delegate.legalMovesFrom(from);
    }

    @Override
    public List<Piece> bench(Team team) {
        return delegate.bench(team);
    }

    @Override
    public int halfmoveClock() {
        return delegate.halfmoveClock();
    }

    @Override
    public <T> Optional<T> extension(String key, Class<T> type) {
        return delegate.extension(key, type);
    }

    @Override
    public void execute(TurnAction action) {
        delegate.execute(action);
    }

    @Override
    public boolean undo() {
        return delegate.undo();
    }

    @Override
    public boolean redo() {
        return delegate.redo();
    }

    @Override
    public boolean canUndo() {
        return delegate.canUndo();
    }

    @Override
    public boolean canRedo() {
        return delegate.canRedo();
    }

    @Override
    public List<String> history() {
        return delegate.history();
    }

    @Override
    public void addObserver(GameObserver observer) {
        delegate.addObserver(observer);
    }

    @Override
    public void removeObserver(GameObserver observer) {
        delegate.removeObserver(observer);
    }
}
