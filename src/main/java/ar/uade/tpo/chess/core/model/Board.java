package ar.uade.tpo.chess.core.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The 8x8 board. It only knows where pieces are; it knows nothing about turns or rules.
 * {@link #copy()} is cheap because {@link Piece}s are immutable and can be shared between copies.
 */
public final class Board {

    private final Piece[][] cells = new Piece[Position.SIZE][Position.SIZE];

    private Board() {
    }

    public static Board empty() {
        return new Board();
    }

    /** The standard starting position. */
    public static Board standard() {
        Board board = new Board();
        PieceType[] backRank = {
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        };
        for (Team team : Team.values()) {
            for (int file = 0; file < Position.SIZE; file++) {
                board.place(new Position(file, team.homeRank()), PieceFactory.create(backRank[file], team));
                board.place(new Position(file, team.pawnStartRank()), new Pawn(team));
            }
        }
        return board;
    }

    /**
     * Builds a board from 8 text rows, rank 8 first. Uppercase letters are white pieces, lowercase are black,
     * and {@code '.'} is an empty cell. All pieces start unmoved. Mainly useful for tests and debugging.
     */
    public static Board fromDiagram(String... rows) {
        if (rows == null || rows.length != Position.SIZE) {
            throw new IllegalArgumentException("A diagram needs exactly 8 rows");
        }
        Board board = new Board();
        for (int row = 0; row < Position.SIZE; row++) {
            String line = rows[row];
            if (line == null || line.length() != Position.SIZE) {
                throw new IllegalArgumentException("Diagram row " + (row + 1) + " must have exactly 8 characters");
            }
            int rank = Position.SIZE - 1 - row;
            for (int file = 0; file < Position.SIZE; file++) {
                char c = line.charAt(file);
                if (c == '.') {
                    continue;
                }
                PieceType type = PieceType.fromLetter(c)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown piece letter: " + c));
                Team team = Character.isUpperCase(c) ? Team.WHITE : Team.BLACK;
                board.place(new Position(file, rank), PieceFactory.create(type, team));
            }
        }
        return board;
    }

    public Optional<Piece> pieceAt(Position position) {
        return Optional.ofNullable(cells[position.file()][position.rank()]);
    }

    public Square squareAt(Position position) {
        return new Square(position, cells[position.file()][position.rank()]);
    }

    public boolean isEmpty(Position position) {
        return cells[position.file()][position.rank()] == null;
    }

    /** Puts a piece on a position, replacing whatever was there. */
    public void place(Position position, Piece piece) {
        Objects.requireNonNull(piece, "piece");
        cells[position.file()][position.rank()] = piece;
    }

    /** Empties a position and returns the piece that was there, if any. */
    public Optional<Piece> remove(Position position) {
        Piece removed = cells[position.file()][position.rank()];
        cells[position.file()][position.rank()] = null;
        return Optional.ofNullable(removed);
    }

    /**
     * Raw relocation of a piece with no rule checks: the piece on {@code from} ends on {@code to}.
     *
     * @return the piece that was captured on {@code to}, if any
     */
    public Optional<Piece> movePiece(Position from, Position to) {
        Piece moving = remove(from)
                .orElseThrow(() -> new IllegalArgumentException("No piece at " + from));
        Optional<Piece> captured = pieceAt(to);
        place(to, moving);
        return captured;
    }

    public Board copy() {
        Board copy = new Board();
        for (int file = 0; file < Position.SIZE; file++) {
            copy.cells[file] = Arrays.copyOf(cells[file], Position.SIZE);
        }
        return copy;
    }

    /** All pieces on the board, scanning a1, a2, ... h8. */
    public List<PlacedPiece> pieces() {
        List<PlacedPiece> result = new ArrayList<>();
        for (int file = 0; file < Position.SIZE; file++) {
            for (int rank = 0; rank < Position.SIZE; rank++) {
                Piece piece = cells[file][rank];
                if (piece != null) {
                    result.add(new PlacedPiece(new Position(file, rank), piece));
                }
            }
        }
        return result;
    }

    public List<PlacedPiece> pieces(Team team) {
        return pieces().stream().filter(p -> p.piece().team() == team).toList();
    }

    public Optional<Position> findKing(Team team) {
        for (PlacedPiece placed : pieces(team)) {
            if (placed.piece().type() == PieceType.KING) {
                return Optional.of(placed.position());
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Board other)) {
            return false;
        }
        for (int file = 0; file < Position.SIZE; file++) {
            if (!Arrays.equals(cells[file], other.cells[file])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(cells);
    }

    /** The same 8-row diagram format accepted by {@link #fromDiagram(String...)}, rank 8 first. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int rank = Position.SIZE - 1; rank >= 0; rank--) {
            for (int file = 0; file < Position.SIZE; file++) {
                Piece piece = cells[file][rank];
                sb.append(piece == null ? '.' : piece.symbol());
            }
            if (rank > 0) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
