package ar.uade.tpo.chess.ports;

import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Team;

/**
 * Puerto de salida que define cómo se muestran las piezas y los bandos. El núcleo solo trabaja con {@link Team}
 * y {@link PieceType}; cada adaptador los traduce a los nombres e iconos del tema elegido, sin que el núcleo
 * tenga que conocerlo.
 */
public interface PieceTheme {

    /** Nombre visible de un bando, por ejemplo «Blancas» o el de una franquicia. */
    String teamName(Team team);

    /** Nombre visible de una pieza, por ejemplo «Caballo blanco» o «Escolta de los Lakers». */
    String pieceName(PieceType type, Team team);

    /** Icono o código breve para mostrar la pieza en espacios reducidos. */
    String icon(PieceType type, Team team);
}
