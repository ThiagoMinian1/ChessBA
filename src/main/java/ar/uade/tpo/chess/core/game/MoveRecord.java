package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Team;

/**
 * Resultado de la última jugada, guardado en {@link GameState} para que las acciones que la envuelven
 * (decoradores) puedan reaccionar sin tener que calcularlo de nuevo.
 *
 * @param team      bando que jugó
 * @param movedType tipo de pieza que se movió (sigue siendo peón aunque haya promocionado)
 * @param capture   indica si hubo una captura, incluida la captura al paso
 */
public record MoveRecord(Team team, Move move, PieceType movedType, boolean capture) {

    public boolean isPawnMove() {
        return movedType == PieceType.PAWN;
    }
}
