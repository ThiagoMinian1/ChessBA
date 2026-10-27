package ar.uade.tpo.chess.adapters.nba;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The 30 NBA franchises a player can pick. This is an adapter concept: the core only knows
 * {@code Team.WHITE} and {@code Team.BLACK}, which is why this is not called {@code Team}.
 */
public enum Franchise {
    ATLANTA_HAWKS("Atlanta", "Hawks", "ATL"),
    BOSTON_CELTICS("Boston", "Celtics", "BOS"),
    BROOKLYN_NETS("Brooklyn", "Nets", "BKN"),
    CHARLOTTE_HORNETS("Charlotte", "Hornets", "CHA"),
    CHICAGO_BULLS("Chicago", "Bulls", "CHI"),
    CLEVELAND_CAVALIERS("Cleveland", "Cavaliers", "CLE"),
    DALLAS_MAVERICKS("Dallas", "Mavericks", "DAL"),
    DENVER_NUGGETS("Denver", "Nuggets", "DEN"),
    DETROIT_PISTONS("Detroit", "Pistons", "DET"),
    GOLDEN_STATE_WARRIORS("Golden State", "Warriors", "GSW"),
    HOUSTON_ROCKETS("Houston", "Rockets", "HOU"),
    INDIANA_PACERS("Indiana", "Pacers", "IND"),
    LOS_ANGELES_CLIPPERS("Los Angeles", "Clippers", "LAC"),
    LOS_ANGELES_LAKERS("Los Angeles", "Lakers", "LAL"),
    MEMPHIS_GRIZZLIES("Memphis", "Grizzlies", "MEM"),
    MIAMI_HEAT("Miami", "Heat", "MIA"),
    MILWAUKEE_BUCKS("Milwaukee", "Bucks", "MIL"),
    MINNESOTA_TIMBERWOLVES("Minnesota", "Timberwolves", "MIN"),
    NEW_ORLEANS_PELICANS("New Orleans", "Pelicans", "NOP"),
    NEW_YORK_KNICKS("New York", "Knicks", "NYK"),
    OKLAHOMA_CITY_THUNDER("Oklahoma City", "Thunder", "OKC"),
    ORLANDO_MAGIC("Orlando", "Magic", "ORL"),
    PHILADELPHIA_76ERS("Philadelphia", "76ers", "PHI"),
    PHOENIX_SUNS("Phoenix", "Suns", "PHX"),
    PORTLAND_TRAIL_BLAZERS("Portland", "Trail Blazers", "POR"),
    SACRAMENTO_KINGS("Sacramento", "Kings", "SAC"),
    SAN_ANTONIO_SPURS("San Antonio", "Spurs", "SAS"),
    TORONTO_RAPTORS("Toronto", "Raptors", "TOR"),
    UTAH_JAZZ("Utah", "Jazz", "UTA"),
    WASHINGTON_WIZARDS("Washington", "Wizards", "WAS");

    private final String city;
    private final String nickname;
    private final String abbreviation;

    Franchise(String city, String nickname, String abbreviation) {
        this.city = city;
        this.nickname = nickname;
        this.abbreviation = abbreviation;
    }

    public String city() {
        return city;
    }

    /** The team name without the city, e.g. "Lakers". */
    public String nickname() {
        return nickname;
    }

    /** Three-letter code, e.g. "LAL". */
    public String abbreviation() {
        return abbreviation;
    }

    /** City and nickname, e.g. "Los Angeles Lakers". */
    public String displayName() {
        return city + " " + nickname;
    }

    /**
     * Finds a franchise by abbreviation, nickname or full name (case and punctuation insensitive), or by a
     * fragment of the full name when that fragment identifies exactly one franchise.
     * "lakers", "LAL" and "los angeles lakers" all find the Lakers; "los angeles" is ambiguous and finds none.
     */
    public static Optional<Franchise> find(String query) {
        if (query == null) {
            return Optional.empty();
        }
        String wanted = normalize(query);
        if (wanted.isEmpty()) {
            return Optional.empty();
        }
        for (Franchise franchise : values()) {
            if (normalize(franchise.abbreviation).equals(wanted)
                    || normalize(franchise.nickname).equals(wanted)
                    || normalize(franchise.displayName()).equals(wanted)) {
                return Optional.of(franchise);
            }
        }
        List<Franchise> partial = Arrays.stream(values())
                .filter(f -> normalize(f.displayName()).contains(wanted))
                .toList();
        return partial.size() == 1 ? Optional.of(partial.get(0)) : Optional.empty();
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
