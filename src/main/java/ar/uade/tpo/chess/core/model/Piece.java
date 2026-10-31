package ar.uade.tpo.chess.core.model;

import java.util.Objects;

/**
 * Pieza de ajedrez inmutable. Al moverla se crea otra con {@link #withMoved(boolean)}, lo que permite guardar
 * estados (Memento) sin copiar cada pieza para deshacer o rehacer una jugada.
 *
 * <p>El movimiento se define en una estrategia ({@code MoveRule}) que se obtiene a partir de {@link #type()}.
 * Así, el modelo no depende de las reglas y se evita una referencia circular con ese paquete.
 */
public abstract class Piece {

    private final Team team;
    private final boolean moved;

    protected Piece(Team team, boolean moved) {
        this.team = Objects.requireNonNull(team, "team");
        this.moved = moved;
    }

    public abstract PieceType type();

    /** Copia de esta pieza con el indicador de movimiento actualizado (afecta al derecho de enroque). */
    public abstract Piece withMoved(boolean moved);

    /** Valor material habitual: peón 1, pieza menor 3, torre 5 y dama 9; el rey no tiene valor de intercambio. */
    public abstract int materialValue();

    public Team team() {
        return team;
    }

    public boolean hasMoved() {
        return moved;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Piece other = (Piece) o;
        return team == other.team && moved == other.moved;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), team, moved);
    }

    /** Letra para el diagrama: mayúscula para blancas y minúscula para negras. */
    public char symbol() {
        char letter = type().letter();
        return team == Team.WHITE ? letter : Character.toLowerCase(letter);
    }

    @Override
    public String toString() {
        return team + " " + type();
    }
}
