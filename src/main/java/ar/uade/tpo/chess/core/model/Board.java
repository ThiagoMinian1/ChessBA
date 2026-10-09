package ar.uade.tpo.chess.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * An 8x8 chess board. Owns every mutation of piece placement so that the
 * rest of the core never reaches into a Square directly.
 */
public final class Board {

    private final Square[][] squares = new Square[8][8];

    private Board(boolean startingPosition) {
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                squares[row][column] = new Square(new Position(row, column), null);
            }
        }
        if (startingPosition) {
            setUpStandardPosition();
        }
    }

    /** A board with the standard chess starting position. */
    public static Board standard() {
        return new Board(true);
    }

    /** An empty board, useful for isolating a single piece's rules in tests. */
    public static Board empty() {
        return new Board(false);
    }

    private void setUpStandardPosition() {
        placeBackRank(0, Team.WHITE);
        placePawnRow(1, Team.WHITE);
        placePawnRow(6, Team.BLACK);
        placeBackRank(7, Team.BLACK);
    }

    private void placeBackRank(int row, Team team) {
        placePiece(new Position(row, 0), new Rook(team));
        placePiece(new Position(row, 1), new Knight(team));
        placePiece(new Position(row, 2), new Bishop(team));
        placePiece(new Position(row, 3), new Queen(team));
        placePiece(new Position(row, 4), new King(team));
        placePiece(new Position(row, 5), new Bishop(team));
        placePiece(new Position(row, 6), new Knight(team));
        placePiece(new Position(row, 7), new Rook(team));
    }

    private void placePawnRow(int row, Team team) {
        for (int column = 0; column < 8; column++) {
            placePiece(new Position(row, column), new Pawn(team));
        }
    }

    public Square squareAt(Position position) {
        return squares[position.row()][position.column()];
    }

    public Piece pieceAt(Position position) {
        return squareAt(position).getPiece();
    }

    public boolean isEmpty(Position position) {
        return squareAt(position).isEmpty();
    }

    public void placePiece(Position position, Piece piece) {
        squareAt(position).setPiece(piece);
    }

    /**
     * Moves whatever piece is on {@code from} to {@code to}, overwriting
     * (capturing) anything that was there.
     *
     * @return the piece that was captured, or {@code null} if the destination was empty
     */
    public Piece applyMove(Position from, Position to) {
        Square originSquare = squareAt(from);
        Piece movingPiece = originSquare.getPiece();
        Square destinationSquare = squareAt(to);
        Piece capturedPiece = destinationSquare.getPiece();

        destinationSquare.setPiece(movingPiece);
        originSquare.setPiece(null);
        return capturedPiece;
    }

    /** All squares currently holding a piece of the given team. */
    public List<Square> squaresWithPieceOf(Team team) {
        List<Square> result = new ArrayList<>();
        for (Square[] row : squares) {
            for (Square square : row) {
                if (square.isOccupiedBy(team)) {
                    result.add(square);
                }
            }
        }
        return result;
    }

    /** A deep copy: same piece placement, but independent of this board. */
    public Board copy() {
        Board copy = Board.empty();
        for (Square[] row : squares) {
            for (Square square : row) {
                if (!square.isEmpty()) {
                    copy.placePiece(square.getPosition(), square.getPiece().copy());
                }
            }
        }
        return copy;
    }
}
