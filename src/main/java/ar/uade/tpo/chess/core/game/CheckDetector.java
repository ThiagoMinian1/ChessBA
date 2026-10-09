package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.King;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Square;
import ar.uade.tpo.chess.core.model.Team;
import ar.uade.tpo.chess.core.rules.MoveRuleFactory;

/**
 * A single responsibility, deliberately kept separate from {@link Game}:
 * is a given team's king currently attacked? It only ever looks at
 * pseudo-legal moves of the opponent, so it never recurses into legality
 * filtering — that filtering is what {@code Game} builds on top of this.
 */
public final class CheckDetector {

    private CheckDetector() {
    }

    public static boolean isInCheck(Board board, Team team) {
        Position kingPosition = findKing(board, team);
        Team opponent = team.opponent();

        for (Square square : board.squaresWithPieceOf(opponent)) {
            var attacks = MoveRuleFactory.ruleFor(square.getPiece().getType())
                    .possibleMoves(square.getPosition(), board);
            if (attacks.contains(kingPosition)) {
                return true;
            }
        }
        return false;
    }

    public static Position findKing(Board board, Team team) {
        for (Square square : board.squaresWithPieceOf(team)) {
            if (square.getPiece().getType() == PieceType.KING) {
                return square.getPosition();
            }
        }
        throw new IllegalStateException("Board has no " + team + " " + King.class.getSimpleName());
    }
}
