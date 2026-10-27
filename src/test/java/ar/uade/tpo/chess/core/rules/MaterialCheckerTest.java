package ar.uade.tpo.chess.core.rules;

import org.junit.jupiter.api.Test;

import static ar.uade.tpo.chess.TestSupport.boardOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialCheckerTest {

    @Test
    void kingAgainstKingIsInsufficient() {
        assertTrue(MaterialChecker.isInsufficientMaterial(boardOf("Ke1", "ke8")));
    }

    @Test
    void kingAndOneMinorPieceAgainstKingIsInsufficient() {
        assertTrue(MaterialChecker.isInsufficientMaterial(boardOf("Ke1", "Bc1", "ke8")));
        assertTrue(MaterialChecker.isInsufficientMaterial(boardOf("Ke1", "Nc1", "ke8")));
        assertTrue(MaterialChecker.isInsufficientMaterial(boardOf("Ke1", "ke8", "bc8")));
        assertTrue(MaterialChecker.isInsufficientMaterial(boardOf("Ke1", "ke8", "nc8")));
    }

    @Test
    void bishopsOnTheSameColourAreInsufficientEvenOnOppositeSides() {
        // c1 and f8 are both dark squares.
        assertTrue(MaterialChecker.isInsufficientMaterial(boardOf("Ke1", "Bc1", "ke8", "bf8")));
    }

    @Test
    void bishopsOnOppositeColoursCanStillMate() {
        // c1 is dark, c8 is light.
        assertFalse(MaterialChecker.isInsufficientMaterial(boardOf("Ke1", "Bc1", "ke8", "bc8")));
    }

    @Test
    void anyPawnRookOrQueenIsEnoughMaterial() {
        assertFalse(MaterialChecker.isInsufficientMaterial(boardOf("Ke1", "Pa2", "ke8")));
        assertFalse(MaterialChecker.isInsufficientMaterial(boardOf("Ke1", "Ra1", "ke8")));
        assertFalse(MaterialChecker.isInsufficientMaterial(boardOf("Ke1", "Qd1", "ke8")));
        assertFalse(MaterialChecker.isInsufficientMaterial(boardOf("Ke1", "ke8", "pa7")));
    }

    @Test
    void bishopAgainstKnightIsEnoughMaterial() {
        assertFalse(MaterialChecker.isInsufficientMaterial(boardOf("Ke1", "Bc1", "ke8", "nb8")));
    }

    @Test
    void twoKnightsAreEnoughMaterial() {
        assertFalse(MaterialChecker.isInsufficientMaterial(boardOf("Ke1", "Nc1", "Nb1", "ke8")));
    }

    @Test
    void bishopPlusKnightIsEnoughMaterial() {
        assertFalse(MaterialChecker.isInsufficientMaterial(boardOf("Ke1", "Bc1", "Nb1", "ke8")));
    }

    @Test
    void theStartingPositionIsNotInsufficient() {
        assertFalse(MaterialChecker.isInsufficientMaterial(ar.uade.tpo.chess.core.model.Board.standard()));
    }
}
