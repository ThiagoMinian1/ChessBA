package ar.uade.tpo.chess.adapters.console;

import ar.uade.tpo.chess.adapters.StandardPieceTheme;
import ar.uade.tpo.chess.adapters.nba.Franchise;
import ar.uade.tpo.chess.adapters.nba.GameConfig;
import ar.uade.tpo.chess.adapters.nba.NbaGameFactory;
import ar.uade.tpo.chess.adapters.nba.NbaPieceTheme;
import ar.uade.tpo.chess.adapters.nba.ScoreboardGame;
import ar.uade.tpo.chess.adapters.nba.ShotClockGame;
import ar.uade.tpo.chess.adapters.nba.ShotClockListener;
import ar.uade.tpo.chess.adapters.nba.TradeAction;
import ar.uade.tpo.chess.core.game.ChessGame;
import ar.uade.tpo.chess.core.game.IllegalMoveException;
import ar.uade.tpo.chess.core.game.PromotionRequiredException;
import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Team;
import ar.uade.tpo.chess.ports.PieceTheme;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;

/**
 * Plays a whole game over text streams: setup menu, franchise selection, then the move loop.
 * Streams are injected, so the whole flow can be driven from a test with a scripted input.
 * Everything here is an adapter: it only talks to the core through {@link ChessGame} and the ports.
 */
public final class ConsoleRunner {

    private final BufferedReader in;
    private final PrintStream out;
    private final ConsoleBoardRenderer renderer = new ConsoleBoardRenderer();

    private ChessGame game;
    private GameConfig config = GameConfig.standard();
    private PieceTheme theme = new StandardPieceTheme();
    private boolean inputClosed;

    public ConsoleRunner(BufferedReader in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    /** Runs setup and then the game until it is quit, or input ends. */
    public void run() {
        out.println("=== Chess ===");
        if (!setup()) {
            return;
        }
        play();
    }

    // ---- Setup -----------------------------------------------------------------------------------------

    private boolean setup() {
        out.println("Choose a mode:");
        out.println("  1) Standard chess");
        out.println("  2) NBA chess: all mechanics (shot clock, trade, hot hand, scoreboard)");
        out.println("  3) NBA chess: pick the mechanics");
        out.println("  4) NBA theme only (standard rules)");
        Integer choice = askNumber("Mode [1-4]: ", 1, 4);
        if (choice == null) {
            return false;
        }
        switch (choice) {
            case 1 -> config = GameConfig.standard();
            case 2 -> config = GameConfig.builder().shotClock(askShotClockLimitOrDefault()).trade().hotHand().scoreboard().build();
            case 3 -> {
                Optional<GameConfig> custom = askCustomConfig();
                if (custom.isEmpty()) {
                    return false;
                }
                config = custom.get();
            }
            default -> config = GameConfig.standard();
        }
        if (choice != 1) {
            Franchise white = askFranchise(Team.WHITE, null);
            if (white == null) {
                return false;
            }
            Franchise black = askFranchise(Team.BLACK, white);
            if (black == null) {
                return false;
            }
            theme = new NbaPieceTheme(white, black);
        }
        ShotClockListener listener = new ShotClockListener() {
            @Override
            public void onShotClockWarning(Team team, int movesRemaining) {
                out.println("  * Shot clock: " + theme.teamName(team) + " has " + movesRemaining
                        + (movesRemaining == 1 ? " move" : " moves") + " left to capture or advance a pawn");
            }
        };
        game = NbaGameFactory.create(config, listener);
        game.addObserver(new ConsoleGameObserver(out, theme));
        return true;
    }

    private int askShotClockLimitOrDefault() {
        Integer limit = askNumber("Shot clock limit in own moves [default "
                + GameConfig.DEFAULT_SHOT_CLOCK_LIMIT + ", enter for default]: ", 1, 200, true);
        return limit == null ? GameConfig.DEFAULT_SHOT_CLOCK_LIMIT : limit;
    }

    private Optional<GameConfig> askCustomConfig() {
        GameConfig.Builder builder = GameConfig.builder();
        Boolean shotClock = askYesNo("Enable the shot clock? [y/n]: ");
        if (shotClock == null) {
            return Optional.empty();
        }
        if (shotClock) {
            builder.shotClock(askShotClockLimitOrDefault());
        }
        Boolean trade = askYesNo("Enable the trade (once per team)? [y/n]: ");
        if (trade == null) {
            return Optional.empty();
        }
        if (trade) {
            builder.trade();
        }
        Boolean hotHand = askYesNo("Enable the hot hand (2 captures in a row = extra move)? [y/n]: ");
        if (hotHand == null) {
            return Optional.empty();
        }
        if (hotHand) {
            builder.hotHand();
        }
        Boolean scoreboard = askYesNo("Enable the scoreboard (captures score points; a drawn game goes to the leader)? [y/n]: ");
        if (scoreboard == null) {
            return Optional.empty();
        }
        if (scoreboard) {
            builder.scoreboard();
        }
        return Optional.of(builder.build());
    }

    private Franchise askFranchise(Team team, Franchise taken) {
        Franchise[] all = Franchise.values();
        if (taken == null) {
            out.println("Franchises:");
            for (int i = 0; i < all.length; i++) {
                out.printf("  %2d) %s (%s)%n", i + 1, all[i].displayName(), all[i].abbreviation());
            }
        }
        String label = team == Team.WHITE ? "White" : "Black";
        while (true) {
            String line = prompt(label + " plays as (number, name or code): ");
            if (line == null) {
                return null;
            }
            Franchise chosen = parseFranchise(line.trim(), all);
            if (chosen == null) {
                out.println("Not a franchise. Use a number 1-" + all.length + ", a name like 'Lakers', or a code like 'LAL'.");
            } else if (chosen == taken) {
                out.println(chosen.displayName() + " is already taken. Pick another franchise.");
            } else {
                return chosen;
            }
        }
    }

    private static Franchise parseFranchise(String text, Franchise[] all) {
        try {
            int index = Integer.parseInt(text);
            return index >= 1 && index <= all.length ? all[index - 1] : null;
        } catch (NumberFormatException e) {
            return Franchise.find(text).orElse(null);
        }
    }

    // ---- Game loop -------------------------------------------------------------------------------------

    private void play() {
        out.println();
        out.println("Type a move like 'e2 e4'. Type 'help' for all commands.");
        boolean announcedEnd = false;
        while (true) {
            out.println();
            showPosition();
            if (game.status().isOver()) {
                if (!announcedEnd) {
                    announcedEnd = true;
                }
                announceScoreboardVerdict();
                out.println("The game is over. Type 'undo' to take back the last action, or 'quit' to leave.");
            }
            String line = prompt(promptText());
            if (line == null) {
                return;
            }
            Command command = CommandParser.parse(line);
            if (command instanceof Command.Quit) {
                out.println("Bye!");
                return;
            }
            handle(command);
        }
    }

    private String promptText() {
        if (game.status().isOver()) {
            return "> ";
        }
        return theme.teamName(game.currentTurn()) + " (" + game.currentTurn().name().toLowerCase() + ") > ";
    }

    private void handle(Command command) {
        try {
            switch (command) {
                case Command.MoveCommand move -> handleMove(move);
                case Command.Undo ignored -> out.println(game.undo() ? "Last action undone." : "Nothing to undo.");
                case Command.Redo ignored -> out.println(game.redo() ? "Action redone." : "Nothing to redo.");
                case Command.Trade trade -> handleTrade(trade);
                case Command.Bench ignored -> showBench();
                case Command.Resign ignored -> handleResign();
                case Command.Draw ignored -> handleDraw();
                case Command.Help ignored -> showHelp();
                case Command.Invalid invalid -> out.println(invalid.reason());
                case Command.Quit ignored -> { }
            }
        } catch (IllegalMoveException e) {
            out.println("Not allowed: " + e.getMessage());
        }
    }

    private void handleMove(Command.MoveCommand move) {
        PieceType promotion = move.promotion();
        if (promotion == null && game.requiresPromotion(move.from(), move.to())) {
            promotion = askPromotion();
            if (promotion == null) {
                return;
            }
        }
        try {
            game.play(move.from(), move.to(), promotion);
        } catch (PromotionRequiredException e) {
            out.println("Not allowed: " + e.getMessage());
        }
    }

    private PieceType askPromotion() {
        while (true) {
            String line = prompt("Promote to (Q/R/B/N): ");
            if (line == null) {
                return null;
            }
            String text = line.trim();
            Optional<PieceType> type = text.length() == 1 ? PieceType.fromLetter(text.charAt(0)) : Optional.empty();
            if (type.isPresent() && type.get().isPromotionTarget()) {
                return type.get();
            }
            out.println("Choose Q (queen), R (rook), B (bishop) or N (knight).");
        }
    }

    private void handleTrade(Command.Trade trade) {
        if (!config.tradeEnabled()) {
            out.println("The trade is not enabled in this game.");
            return;
        }
        game.execute(new TradeAction(trade.square(), trade.benchPiece()));
    }

    private void handleResign() {
        if (game.status().isOver()) {
            out.println("The game is already over.");
            return;
        }
        game.resign(game.currentTurn());
    }

    private void handleDraw() {
        if (game.status().isOver()) {
            out.println("The game is already over.");
            return;
        }
        Team offering = game.currentTurn();
        Team other = offering.opposite();
        Boolean accepted = askYesNo(theme.teamName(offering) + " offers a draw. "
                + theme.teamName(other) + ", do you accept? [y/n]: ");
        if (accepted != null && accepted) {
            game.agreeDraw();
        } else {
            out.println("Draw declined.");
        }
    }

    // ---- Display ---------------------------------------------------------------------------------------

    private void showPosition() {
        out.println(renderer.render(game.board()));
        out.println("   UPPERCASE = " + sideLabel(Team.WHITE) + "  |  lowercase = " + sideLabel(Team.BLACK));
        if (config.shotClockEnabled()) {
            out.println("Shot clock (moves without progress / limit): "
                    + theme.teamName(Team.WHITE) + " "
                    + ShotClockGame.stateOf(game, Team.WHITE).movesWithoutProgress() + "/" + config.shotClockLimit()
                    + ", " + theme.teamName(Team.BLACK) + " "
                    + ShotClockGame.stateOf(game, Team.BLACK).movesWithoutProgress() + "/" + config.shotClockLimit());
        }
        if (config.scoreboardEnabled()) {
            out.println("SCOREBOARD  " + theme.teamName(Team.WHITE) + " " + ScoreboardGame.scoreOf(game, Team.WHITE)
                    + " - " + ScoreboardGame.scoreOf(game, Team.BLACK) + " " + theme.teamName(Team.BLACK));
        }
        if (!game.status().isOver() && game.isInCheck()) {
            out.println(theme.teamName(game.currentTurn()) + " is in check.");
        }
    }

    private void announceScoreboardVerdict() {
        if (!config.scoreboardEnabled()) {
            return;
        }
        if (ScoreboardGame.decidedOnPoints(game)) {
            Team leader = game.winner().orElseThrow();
            out.println("*** Scoreboard decides the draw: " + theme.teamName(leader) + " wins "
                    + ScoreboardGame.scoreOf(game, leader) + "-" + ScoreboardGame.scoreOf(game, leader.opposite())
                    + " ***");
        } else if (game.status().isDraw() && game.status() != GameStatus.DRAW_BY_AGREEMENT) {
            out.println("The scoreboard is level (" + ScoreboardGame.scoreOf(game, Team.WHITE) + "-"
                    + ScoreboardGame.scoreOf(game, Team.BLACK) + "): the draw stands.");
        }
    }

    /** "White" in plain chess, "White (Los Angeles Lakers)" when the side has a franchise. */
    private String sideLabel(Team team) {
        String plain = team == Team.WHITE ? "White" : "Black";
        String themed = theme.teamName(team);
        return themed.equals(plain) ? plain : plain + " (" + themed + ")";
    }

    private void showBench() {
        for (Team team : Team.values()) {
            List<Piece> bench = game.bench(team);
            out.print(theme.teamName(team) + " bench: ");
            if (bench.isEmpty()) {
                out.println("(empty)");
            } else {
                StringBuilder names = new StringBuilder();
                for (Piece piece : bench) {
                    if (names.length() > 0) {
                        names.append(", ");
                    }
                    names.append(piece.type().letter()).append(" = ").append(theme.pieceName(piece.type(), team));
                }
                out.println(names);
            }
        }
    }

    private void showHelp() {
        out.println("Commands:");
        out.println("  e2 e4            move a piece (also 'e2e4'); for a promotion add the piece: 'e7 e8 q'");
        out.println("  undo / redo      take back / replay actions");
        out.println("  bench            show captured pieces and traded-out pieces");
        if (config.tradeEnabled()) {
            out.println("  trade e4 q       trade your piece on e4 for a queen from your bench (once per team)");
        }
        out.println("  resign           give up the game");
        out.println("  draw             offer a draw to the other player");
        out.println("  help             this text");
        out.println("  quit             leave");
        out.println("Pieces: K king, Q queen, R rook, B bishop, N knight, P pawn (uppercase = white, lowercase = black).");
        if (!(theme instanceof StandardPieceTheme)) {
            out.println("Legend:");
            for (Team team : Team.values()) {
                StringBuilder legend = new StringBuilder("  " + theme.teamName(team) + ": ");
                for (PieceType type : PieceType.values()) {
                    legend.append(type.letter()).append('=').append(NbaPieceTheme.roleName(type)).append("  ");
                }
                out.println(legend.toString().stripTrailing());
                break; // both sides use the same roles; one line is enough
            }
        }
    }

    // ---- Input helpers ---------------------------------------------------------------------------------

    /** Prints a prompt and reads a line; returns null (and remembers it) when input has ended. */
    private String prompt(String text) {
        if (inputClosed) {
            return null;
        }
        out.print(text);
        out.flush();
        try {
            String line = in.readLine();
            if (line == null) {
                inputClosed = true;
                out.println();
            }
            return line;
        } catch (IOException e) {
            inputClosed = true;
            return null;
        }
    }

    private Integer askNumber(String text, int min, int max) {
        return askNumber(text, min, max, false);
    }

    private Integer askNumber(String text, int min, int max, boolean blankMeansNull) {
        while (true) {
            String line = prompt(text);
            if (line == null) {
                return null;
            }
            if (blankMeansNull && line.isBlank()) {
                return null;
            }
            try {
                int value = Integer.parseInt(line.trim());
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the message
            }
            out.println("Enter a number between " + min + " and " + max + ".");
        }
    }

    private Boolean askYesNo(String text) {
        while (true) {
            String line = prompt(text);
            if (line == null) {
                return null;
            }
            String answer = line.trim().toLowerCase();
            if (answer.equals("y") || answer.equals("yes") || answer.equals("s") || answer.equals("si")) {
                return true;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return false;
            }
            out.println("Answer y or n.");
        }
    }
}
