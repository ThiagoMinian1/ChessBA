package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Peón: avanza una casilla (o dos desde su fila inicial, si ambas están vacías), captura en diagonal hacia
 * adelante, puede capturar al paso y promociona al llegar a la última fila. Se generan cuatro movimientos de
 * promoción, uno por cada pieza permitida.
 */
public final class PawnMoveRule implements MoveRule {

    private static final PieceType[] PROMOTION_TARGETS = {
        PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT
    };

    @Override
    public List<Move> pseudoLegalMoves(MoveContext context, Position from, Piece piece) {
        Board board = context.board();
        Team team = piece.team();
        List<Move> moves = new ArrayList<>();

        int nextRank = from.rank() + team.forward();
        if (!Position.inBounds(from.file(), nextRank)) {
            return moves;
        }
        boolean promoting = nextRank == team.promotionRank();

        Position oneStep = new Position(from.file(), nextRank);
        if (board.isEmpty(oneStep)) {
            addAdvance(moves, from, oneStep, promoting);
            if (from.rank() == team.pawnStartRank()) {
                Position twoSteps = new Position(from.file(), from.rank() + 2 * team.forward());
                if (board.isEmpty(twoSteps)) {
                    moves.add(Move.doublePush(from, twoSteps));
                }
            }
        }

        for (int fileStep : new int[] {-1, 1}) {
            Optional<Position> target = from.offset(fileStep, team.forward());
            if (target.isEmpty()) {
                continue;
            }
            Optional<Piece> occupant = board.pieceAt(target.get());
            if (occupant.isPresent()) {
                if (occupant.get().team() != team) {
                    addAdvance(moves, from, target.get(), promoting);
                }
            } else if (isEnPassantAvailable(context, from, target.get(), team)) {
                moves.add(Move.enPassant(from, target.get()));
            }
        }
        return moves;
    }

    private boolean isEnPassantAvailable(MoveContext context, Position from, Position target, Team team) {
        if (context.enPassantTarget().filter(target::equals).isEmpty()) {
            return false;
        }
        // El peón que se captura está al lado, en la columna de la casilla objetivo.
        Position victimPosition = new Position(target.file(), from.rank());
        return context.board().pieceAt(victimPosition)
                .filter(p -> p.type() == PieceType.PAWN && p.team() != team)
                .isPresent();
    }

    private void addAdvance(List<Move> moves, Position from, Position to, boolean promoting) {
        if (promoting) {
            for (PieceType target : PROMOTION_TARGETS) {
                moves.add(Move.promotion(from, to, target));
            }
        } else {
            moves.add(Move.normal(from, to));
        }
    }
}
