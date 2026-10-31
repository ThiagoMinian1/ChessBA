package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.Position;

import java.util.List;

/**
 * Estrategia que define cómo se mueve un tipo de pieza. Solo genera movimientos <em>pseudo-legales</em>:
 * respetan la forma de moverse y los bloqueos, pero todavía podrían dejar al rey propio en jaque.
 * {@link MoveGenerator} descarta esos movimientos para mantener cada regla sencilla e independiente.
 */
public interface MoveRule {

    /**
     * @param context tablero en el que se mueve y casilla de captura al paso, si corresponde
     * @param from    posición actual de la pieza
     * @param piece   pieza que se mueve; su bando determina la dirección y qué cuenta como captura
     */
    List<Move> pseudoLegalMoves(MoveContext context, Position from, Piece piece);
}
