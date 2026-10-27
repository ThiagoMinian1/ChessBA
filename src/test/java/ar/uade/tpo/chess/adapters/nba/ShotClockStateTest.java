package ar.uade.tpo.chess.adapters.nba;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShotClockStateTest {

    private static final int LIMIT = 10;

    @Test
    void aFreshClockIsRunningWithNoMovesUsed() {
        assertEquals(new ShotClockState.Running(0), ShotClockState.fresh());
        assertEquals(0, ShotClockState.fresh().movesWithoutProgress());
    }

    @Test
    void movesWithoutProgressAdvanceTheClock() {
        assertEquals(new ShotClockState.Running(1), new ShotClockState.Running(0).afterOwnMove(false, LIMIT));
        assertEquals(new ShotClockState.Running(6), new ShotClockState.Running(5).afterOwnMove(false, LIMIT));
    }

    @Test
    void theClockWarnsWhenOnlyThreeMovesRemain() {
        assertEquals(new ShotClockState.Warning(7), new ShotClockState.Running(6).afterOwnMove(false, LIMIT));
        assertEquals(new ShotClockState.Warning(8), new ShotClockState.Warning(7).afterOwnMove(false, LIMIT));
        assertEquals(new ShotClockState.Warning(9), new ShotClockState.Warning(8).afterOwnMove(false, LIMIT));
    }

    @Test
    void theClockExpiresOnReachingTheLimit() {
        assertEquals(new ShotClockState.Expired(10), new ShotClockState.Warning(9).afterOwnMove(false, LIMIT));
    }

    @Test
    void anExpiredClockStaysExpiredUntilProgress() {
        ShotClockState next = new ShotClockState.Expired(10).afterOwnMove(false, LIMIT);
        assertEquals(new ShotClockState.Expired(11), next);
        assertTrue(next.isExpired());
    }

    @Test
    void progressSendsAnyStateBackToAFreshClock() {
        assertEquals(ShotClockState.fresh(), new ShotClockState.Running(4).afterOwnMove(true, LIMIT));
        assertEquals(ShotClockState.fresh(), new ShotClockState.Warning(8).afterOwnMove(true, LIMIT));
        assertEquals(ShotClockState.fresh(), new ShotClockState.Expired(12).afterOwnMove(true, LIMIT));
    }

    @Test
    void aLimitOfOneExpiresOnTheFirstMoveWithoutProgress() {
        assertTrue(ShotClockState.fresh().afterOwnMove(false, 1).isExpired());
    }

    @Test
    void aShortLimitStartsWarningImmediately() {
        ShotClockState state = ShotClockState.fresh().afterOwnMove(false, 2);
        assertTrue(state.isWarning());
        assertFalse(state.isExpired());
    }

    @Test
    void predicatesDistinguishTheThreeStates() {
        assertFalse(new ShotClockState.Running(1).isExpired());
        assertFalse(new ShotClockState.Running(1).isWarning());
        assertTrue(new ShotClockState.Warning(8).isWarning());
        assertFalse(new ShotClockState.Warning(8).isExpired());
        assertTrue(new ShotClockState.Expired(10).isExpired());
        assertFalse(new ShotClockState.Expired(10).isWarning());
    }
}
