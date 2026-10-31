package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.MoveType;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Team;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static ar.uade.tpo.chess.TestSupport.gameOf;
import static ar.uade.tpo.chess.TestSupport.play;
import static ar.uade.tpo.chess.TestSupport.pos;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reglas de enroque (chess.com): ni el rey ni la torre se movieron, las casillas entre ambos están vacías,
 * el rey no está en jaque ni atraviesa o termina en una casilla atacada. Cada condición se prueba por separado.
 * En estas posiciones, ninguna pieza blanca ataca al rey negro de d7.
 */
class CastlingTest {

    private static final String[] BASE = {"Ke1", "Ra1", "Rh1", "kd7"};

    /** Casillas de destino de los enroques disponibles para las blancas desde e1, con BASE y las piezas adicionales. */
    private static Set<String> whiteCastles(String... extra) {
        List<String> placements = new ArrayList<>(List.of(BASE));
        placements.addAll(List.of(extra));
        Game game = gameOf(Team.WHITE, placements.toArray(new String[0]));
        Set<String> result = new TreeSet<>();
        for (Move move : game.legalMovesFrom(pos("e1"))) {
            if (move.isCastling()) {
                result.add(move.to().toString());
            }
        }
        return result;
    }

    // ---- Los dos enroques ------------------------------------------------------------------------------

    @Test
    void bothCastlesAreAvailableWhenNothingGetsInTheWay() {
        assertEquals(Set.of("c1", "g1"), whiteCastles());
    }

    @Test
    void shortCastleMovesKingToGAndRookToF() {
        Game game = gameOf(Team.WHITE, BASE);
        game.play(pos("e1"), pos("g1"));
        assertEquals(PieceType.KING, game.board().pieceAt(pos("g1")).orElseThrow().type());
        assertEquals(PieceType.ROOK, game.board().pieceAt(pos("f1")).orElseThrow().type());
        assertTrue(game.board().isEmpty(pos("e1")));
        assertTrue(game.board().isEmpty(pos("h1")));
        assertEquals(PieceType.ROOK, game.board().pieceAt(pos("a1")).orElseThrow().type());
        assertEquals(Team.BLACK, game.currentTurn());
    }

    @Test
    void longCastleMovesKingToCAndRookToD() {
        Game game = gameOf(Team.WHITE, BASE);
        game.play(pos("e1"), pos("c1"));
        assertEquals(PieceType.KING, game.board().pieceAt(pos("c1")).orElseThrow().type());
        assertEquals(PieceType.ROOK, game.board().pieceAt(pos("d1")).orElseThrow().type());
        assertTrue(game.board().isEmpty(pos("e1")));
        assertTrue(game.board().isEmpty(pos("a1")));
        assertTrue(game.board().isEmpty(pos("b1")));
        assertEquals(PieceType.ROOK, game.board().pieceAt(pos("h1")).orElseThrow().type());
    }

    @Test
    void blackCanCastleBothWaysToo() {
        Game kingside = gameOf(Team.BLACK, "ke8", "ra8", "rh8", "Kd2");
        kingside.play(pos("e8"), pos("g8"));
        assertEquals(PieceType.KING, kingside.board().pieceAt(pos("g8")).orElseThrow().type());
        assertEquals(PieceType.ROOK, kingside.board().pieceAt(pos("f8")).orElseThrow().type());

        Game queenside = gameOf(Team.BLACK, "ke8", "ra8", "rh8", "Kd2");
        queenside.play(pos("e8"), pos("c8"));
        assertEquals(PieceType.KING, queenside.board().pieceAt(pos("c8")).orElseThrow().type());
        assertEquals(PieceType.ROOK, queenside.board().pieceAt(pos("d8")).orElseThrow().type());
    }

    @Test
    void castlingMovesAreTaggedWithTheirType() {
        Game game = gameOf(Team.WHITE, BASE);
        List<Move> castles = game.legalMovesFrom(pos("e1")).stream().filter(Move::isCastling).toList();
        assertEquals(2, castles.size());
        assertTrue(castles.stream().anyMatch(m -> m.type() == MoveType.CASTLE_KINGSIDE));
        assertTrue(castles.stream().anyMatch(m -> m.type() == MoveType.CASTLE_QUEENSIDE));
    }

    // ---- Piezas que ya se movieron ---------------------------------------------------------------------

    @Test
    void aKingThatMovedAndCameBackCannotCastle() {
        Game game = gameOf(Team.WHITE, BASE);
        play(game, "e1e2", "d7d6", "e2e1", "d6d7");
        assertEquals(Set.of(), castlesOf(game));
    }

    @Test
    void aKingsideRookThatMovedAndCameBackLosesOnlyTheShortCastle() {
        Game game = gameOf(Team.WHITE, BASE);
        play(game, "h1h2", "d7d6", "h2h1", "d6d7");
        assertEquals(Set.of("c1"), castlesOf(game));
    }

    @Test
    void aQueensideRookThatMovedAndCameBackLosesOnlyTheLongCastle() {
        Game game = gameOf(Team.WHITE, BASE);
        play(game, "a1a2", "d7d6", "a2a1", "d6d7");
        assertEquals(Set.of("g1"), castlesOf(game));
    }

    private static Set<String> castlesOf(Game game) {
        Set<String> result = new TreeSet<>();
        for (Move move : game.legalMovesFrom(pos("e1"))) {
            if (move.isCastling()) {
                result.add(move.to().toString());
            }
        }
        return result;
    }

    @Test
    void castlingIsUnavailableOnceTheKingHasCastled() {
        Game game = gameOf(Team.WHITE, BASE);
        play(game, "e1g1", "d7d6");
        assertTrue(game.legalMovesFrom(pos("g1")).stream().noneMatch(Move::isCastling));
    }

    // ---- Casillas entre el rey y la torre ---------------------------------------------------------------

    @Test
    void pieceBetweenKingAndRookBlocksThatCastleOnly() {
        assertEquals(Set.of("g1"), whiteCastles("Nb1"));
        assertEquals(Set.of("g1"), whiteCastles("Bc1"));
        assertEquals(Set.of("g1"), whiteCastles("Qd1"));
        assertEquals(Set.of("c1"), whiteCastles("Bf1"));
        assertEquals(Set.of("c1"), whiteCastles("Ng1"));
    }

    @Test
    void anEnemyPieceBetweenKingAndRookAlsoBlocks() {
        assertEquals(Set.of("g1"), whiteCastles("nd1"));
        assertEquals(Set.of("c1"), whiteCastles("ng1"));
    }

    // ---- El rey no puede enrocar desde, a través de ni hacia un jaque ----------------------------------

    @Test
    void cannotCastleOutOfCheck() {
        assertEquals(Set.of(), whiteCastles("re8"));
        assertEquals(Set.of(), whiteCastles("nd3"));
        assertEquals(Set.of(), whiteCastles("pd2"));
    }

    @Test
    void cannotCastleThroughAnAttackedSquare() {
        assertEquals(Set.of("c1"), whiteCastles("rf8"));
        assertEquals(Set.of("g1"), whiteCastles("bg4"));
        assertEquals(Set.of("c1"), whiteCastles("pg2"));
        assertEquals(Set.of("c1"), whiteCastles("ba6"));
    }

    @Test
    void cannotCastleIntoAnAttackedSquare() {
        assertEquals(Set.of("c1"), whiteCastles("rg8"));
        assertEquals(Set.of("g1"), whiteCastles("rc8"));
        assertEquals(Set.of("c1"), whiteCastles("nh3"));
    }

    @Test
    void theRookMayBeAttackedAndTheBFileSquareMayBeAttacked() {
        assertEquals(Set.of("c1", "g1"), whiteCastles("rh8"));
        assertEquals(Set.of("c1", "g1"), whiteCastles("rb8"));
    }

    @Test
    void illegalCastlingAttemptsAreRejected() {
        Game game = gameOf(Team.WHITE, "Ke1", "Ra1", "Rh1", "kd7", "Nb1", "rf8");
        assertThrows(IllegalMoveException.class, () -> game.play(pos("e1"), pos("c1")));
        assertThrows(IllegalMoveException.class, () -> game.play(pos("e1"), pos("g1")));
        assertEquals(Team.WHITE, game.currentTurn());
    }
}
