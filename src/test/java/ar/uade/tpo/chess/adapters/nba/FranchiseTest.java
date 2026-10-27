package ar.uade.tpo.chess.adapters.nba;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FranchiseTest {

    @Test
    void thereAreThirtyFranchises() {
        assertEquals(30, Franchise.values().length);
    }

    @Test
    void abbreviationsAndNicknamesAreUnique() {
        Set<String> abbreviations = new HashSet<>();
        Set<String> nicknames = new HashSet<>();
        for (Franchise franchise : Franchise.values()) {
            assertEquals(3, franchise.abbreviation().length(), franchise.name());
            assertTrue(abbreviations.add(franchise.abbreviation()), "duplicate " + franchise.abbreviation());
            assertTrue(nicknames.add(franchise.nickname()), "duplicate " + franchise.nickname());
        }
    }

    @Test
    void displayNameIsCityPlusNickname() {
        assertEquals("Los Angeles Lakers", Franchise.LOS_ANGELES_LAKERS.displayName());
        assertEquals("Golden State Warriors", Franchise.GOLDEN_STATE_WARRIORS.displayName());
        assertEquals("Lakers", Franchise.LOS_ANGELES_LAKERS.nickname());
        assertEquals("LAL", Franchise.LOS_ANGELES_LAKERS.abbreviation());
    }

    @Test
    void findsAFranchiseByNicknameCodeOrFullNameIgnoringCase() {
        assertEquals(Franchise.LOS_ANGELES_LAKERS, Franchise.find("lakers").orElseThrow());
        assertEquals(Franchise.LOS_ANGELES_LAKERS, Franchise.find("LAKERS").orElseThrow());
        assertEquals(Franchise.LOS_ANGELES_LAKERS, Franchise.find("lal").orElseThrow());
        assertEquals(Franchise.LOS_ANGELES_LAKERS, Franchise.find("Los Angeles Lakers").orElseThrow());
        assertEquals(Franchise.MIAMI_HEAT, Franchise.find("heat").orElseThrow());
        assertEquals(Franchise.BROOKLYN_NETS, Franchise.find("nets").orElseThrow());
    }

    @Test
    void punctuationAndSpacingDoNotMatter() {
        assertEquals(Franchise.PORTLAND_TRAIL_BLAZERS, Franchise.find("trail blazers").orElseThrow());
        assertEquals(Franchise.PORTLAND_TRAIL_BLAZERS, Franchise.find("Portland  Trail-Blazers").orElseThrow());
        assertEquals(Franchise.PHILADELPHIA_76ERS, Franchise.find("76ers").orElseThrow());
    }

    @Test
    void aFragmentThatIdentifiesExactlyOneFranchiseWorks() {
        assertEquals(Franchise.BOSTON_CELTICS, Franchise.find("celt").orElseThrow());
        assertEquals(Franchise.GOLDEN_STATE_WARRIORS, Franchise.find("golden").orElseThrow());
    }

    @Test
    void ambiguousOrUnknownTextFindsNothing() {
        assertTrue(Franchise.find("los angeles").isEmpty(), "Lakers and Clippers share the city");
        assertTrue(Franchise.find("la").isEmpty());
        assertTrue(Franchise.find("xyz").isEmpty());
        assertTrue(Franchise.find("").isEmpty());
        assertTrue(Franchise.find("   ").isEmpty());
        assertTrue(Franchise.find(null).isEmpty());
    }

    @Test
    void everyFranchiseCanBeFoundByItsOwnNames() {
        for (Franchise franchise : Franchise.values()) {
            assertEquals(franchise, Franchise.find(franchise.displayName()).orElseThrow());
            assertEquals(franchise, Franchise.find(franchise.abbreviation()).orElseThrow());
            assertEquals(franchise, Franchise.find(franchise.nickname()).orElseThrow());
        }
    }
}
