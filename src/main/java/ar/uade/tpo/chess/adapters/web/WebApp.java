package ar.uade.tpo.chess.adapters.web;

import ar.uade.tpo.chess.adapters.nba.*;
import ar.uade.tpo.chess.core.game.ChessGame;
import ar.uade.tpo.chess.core.model.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/** Adaptador HTTP local. Las decisiones de ajedrez siguen a cargo del motor existente. */
public final class WebApp {
    private final HttpServer server;
    private final Map<String, Session> sessions = new LinkedHashMap<>();

    private static final class Session {
        ChessGame game = NbaGameFactory.create(GameConfig.standard());
        GameConfig config = GameConfig.standard();
        String white = "Blancas", black = "Negras", mode = "standard";
        boolean started;
    }

    public WebApp(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", this::handle);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    public void start() { server.start(); }
    public void stop() { server.stop(0); }
    public int port() { return server.getAddress().getPort(); }

    public static void main(String[] args) throws IOException {
        int port = args.length == 0 ? 8080 : Integer.parseInt(args[0]);
        WebApp app = new WebApp(port);
        app.start();
        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
        System.out.println("ChessBA · http://127.0.0.1:" + app.port());
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (!path.startsWith("/api/")) {
                if (!exchange.getRequestMethod().equals("GET")) { send(exchange, 405, "text/plain", "Método no permitido"); return; }
                String resource = switch (path) {
                    case "/", "/index.html" -> "index.html";
                    case "/app.js" -> "app.js";
                    case "/style.css" -> "style.css";
                    default -> null;
                };
                if (resource == null) { send(exchange, 404, "text/plain", "No encontrado"); return; }
                try (var in = WebApp.class.getResourceAsStream("/web/" + resource)) {
                    if (in == null) { send(exchange, 404, "text/plain", "No encontrado"); return; }
                    send(exchange, 200, resource.endsWith("css") ? "text/css" : resource.endsWith("js") ? "text/javascript" : "text/html", new String(in.readAllBytes(), StandardCharsets.UTF_8));
                }
                return;
            }
            boolean mutation = !path.equals("/api/state") && !path.equals("/api/franchises") && !path.equals("/api/result");
            if (!exchange.getRequestMethod().equals(mutation ? "POST" : "GET")) { send(exchange, 405, "application/json", json(Map.of("error", "Método no permitido"))); return; }
            String origin = exchange.getRequestHeaders().getFirst("Origin");
            if (mutation && origin != null && !origin.equals("http://" + exchange.getRequestHeaders().getFirst("Host"))) {
                send(exchange, 403, "application/json", json(Map.of("error", "Origen no permitido"))); return;
            }
            if (path.equals("/api/franchises")) {
                send(exchange, 200, "application/json", json(Arrays.stream(Franchise.values()).map(f -> Map.of("code", f.abbreviation(), "name", f.displayName())).toList())); return;
            }
            Session session = session(exchange);
            synchronized (session) {
                Map<String, String> params = new HashMap<>();
                if (mutation) {
                    byte[] body = exchange.getRequestBody().readNBytes(8193);
                    if (body.length > 8192) { send(exchange, 413, "application/json", json(Map.of("error", "Solicitud demasiado grande"))); return; }
                    for (String pair : new String(body, StandardCharsets.UTF_8).split("&")) {
                        String[] parts = pair.split("=", 2);
                        if (parts.length == 2) params.put(decode(parts[0]), decode(parts[1]));
                    }
                }
                switch (path) {
                    case "/api/state" -> { }
                    case "/api/result" -> {
                        if (!session.game.status().isOver()) {
                            send(exchange, 409, "application/json", json(Map.of("error", "La partida todavía no terminó."))); return;
                        }
                        send(exchange, 200, "application/json", json(gameResult(session.game, session.config.scoreboardEnabled(), session.white, session.black))); return;
                    }
                    case "/api/new" -> newGame(session, params);
                    case "/api/move" -> session.game.play(Position.of(params.get("from")), Position.of(params.get("to")), params.containsKey("promotion") ? PieceType.valueOf(params.get("promotion")) : null);
                    case "/api/undo" -> session.game.undo();
                    case "/api/redo" -> session.game.redo();
                    case "/api/resign" -> session.game.resign(session.game.currentTurn());
                    case "/api/draw" -> session.game.agreeDraw();
                    case "/api/trade" -> {
                        if (!session.config.tradeEnabled()) throw new IllegalArgumentException("Los intercambios no están habilitados.");
                        session.game.execute(new TradeAction(Position.of(params.get("square")), PieceType.valueOf(params.get("piece"))));
                    }
                    default -> { send(exchange, 404, "application/json", json(Map.of("error", "Ruta no encontrada"))); return; }
                }
                if (mutation && !session.game.history().isEmpty()) session.started = true;
                send(exchange, 200, "application/json", json(state(session)));
            }
        } catch (IllegalArgumentException | IllegalStateException | ar.uade.tpo.chess.core.game.IllegalMoveException ex) {
            send(exchange, 400, "application/json", json(Map.of("error", Objects.toString(ex.getMessage(), "Acción inválida"))));
        } catch (Exception ex) {
            ex.printStackTrace();
            send(exchange, 500, "application/json", json(Map.of("error", "No se pudo completar la acción.")));
        } finally { exchange.close(); }
    }

    private synchronized Session session(HttpExchange exchange) {
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie != null) for (String part : cookie.split(";")) {
            String[] entry = part.trim().split("=", 2);
            if (entry.length == 2 && entry[0].equals("chessba") && sessions.containsKey(entry[1])) return sessions.get(entry[1]);
        }
        if (sessions.size() >= 128) sessions.remove(sessions.keySet().iterator().next());
        String id = UUID.randomUUID().toString();
        Session session = new Session();
        sessions.put(id, session);
        exchange.getResponseHeaders().add("Set-Cookie", "chessba=" + id + "; Path=/; HttpOnly; SameSite=Strict");
        return session;
    }

    private void newGame(Session s, Map<String, String> p) {
        String mode = p.getOrDefault("mode", "standard");
        if (!List.of("standard", "nba", "custom", "theme").contains(mode)) throw new IllegalArgumentException("Modo inválido");
        int limit = mode.equals("standard") || mode.equals("theme") ? GameConfig.DEFAULT_SHOT_CLOCK_LIMIT : Integer.parseInt(p.getOrDefault("limit", "10"));
        if (limit < 1 || limit > 24) throw new IllegalArgumentException("El límite del shot clock debe estar entre 1 y 24.");
        GameConfig config = mode.equals("nba") ? GameConfig.builder().shotClock(limit).trade().hotHand().scoreboard().build()
                : mode.equals("custom") ? new GameConfig(Boolean.parseBoolean(p.get("shotClock")), limit, Boolean.parseBoolean(p.get("trade")), Boolean.parseBoolean(p.get("hotHand")), Boolean.parseBoolean(p.get("scoreboard"))) : GameConfig.standard();
        String white = "Blancas", black = "Negras";
        if (!mode.equals("standard")) {
            Franchise w = Franchise.find(p.getOrDefault("white", "LAL")).orElseThrow(() -> new IllegalArgumentException("Franquicia inválida"));
            Franchise b = Franchise.find(p.getOrDefault("black", "BOS")).orElseThrow(() -> new IllegalArgumentException("Franquicia inválida"));
            if (w == b) throw new IllegalArgumentException("Elegí dos franquicias diferentes.");
            white = w.displayName(); black = b.displayName();
        }
        s.game = NbaGameFactory.create(config); s.config = config; s.mode = mode; s.white = white; s.black = black;
        s.started = true;
    }

    private Map<String, Object> state(Session s) {
        ChessGame g = s.game;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("board", g.board().pieces().stream().map(p -> Map.of("square", p.position().toString(), "type", p.piece().type().name(), "team", p.piece().team().name())).toList());
        result.put("legalMoves", g.legalMoves().stream().map(m -> Map.of("from", m.from().toString(), "to", m.to().toString(), "promotion", m.promotion() == null ? "" : m.promotion().name())).toList());
        result.put("turn", g.currentTurn().name()); result.put("status", g.status().name());
        result.put("winner", g.winner().map(Enum::name).orElse("")); result.put("check", !g.status().isOver() && g.isInCheck());
        result.put("history", g.history()); result.put("canUndo", g.canUndo()); result.put("canRedo", g.canRedo());
        result.put("halfmoveClock", g.halfmoveClock());
        result.put("mode", s.mode); result.put("white", s.white); result.put("black", s.black);
        result.put("started", s.started);
        result.put("scoreboard", s.config.scoreboardEnabled());
        result.put("decidedOnPoints", ScoreboardGame.decidedOnPoints(g));
        result.put("result", gameResult(g, s.config.scoreboardEnabled(), s.white, s.black));
        result.put("shotClock", s.config.shotClockEnabled()); result.put("limit", s.config.shotClockLimit());
        result.put("trade", s.config.tradeEnabled()); result.put("hotHand", s.config.hotHandEnabled());
        for (Team team : Team.values()) {
            String key = team == Team.WHITE ? "white" : "black";
            result.put(key + "Score", ScoreboardGame.scoreOf(g, team));
            result.put(key + "Bench", g.bench(team).stream().map(p -> p.type().name()).toList());
            result.put(key + "Clock", ShotClockGame.stateOf(g, team).movesWithoutProgress());
            result.put(key + "ClockExpired", ShotClockGame.stateOf(g, team).isExpired());
            result.put(key + "Streak", HotHandGame.streakOf(g, team));
            result.put(key + "Traded", TradeAction.hasTraded(g, team));
        }
        return result;
    }

    static Map<String, Object> gameResult(ChessGame g, boolean scoreboard, String white, String black) {
        if (!g.status().isOver()) return null;
        boolean onPoints = ScoreboardGame.decidedOnPoints(g);
        String reason = onPoints ? "Victoria por puntos tras " + drawReason(g.status()) : switch (g.status()) {
            case CHECKMATE -> "Jaque mate";
            case RESIGNATION -> "Rendición";
            default -> drawReason(g.status());
        };
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", g.status().name());
        summary.put("winner", g.winner().map(Enum::name).orElse(""));
        summary.put("winnerName", g.winner().map(t -> t == Team.WHITE ? white : black).orElse(""));
        summary.put("reason", reason);
        summary.put("decidedOnPoints", onPoints);
        summary.put("scoreboard", scoreboard);
        summary.put("white", white); summary.put("black", black);
        summary.put("whiteScore", ScoreboardGame.scoreOf(g, Team.WHITE));
        summary.put("blackScore", ScoreboardGame.scoreOf(g, Team.BLACK));
        summary.put("actions", g.history().size());
        summary.put("whitePieces", g.board().pieces(Team.WHITE).size());
        summary.put("blackPieces", g.board().pieces(Team.BLACK).size());
        summary.put("whiteBench", g.bench(Team.WHITE).size());
        summary.put("blackBench", g.bench(Team.BLACK).size());
        summary.put("outcome", g.winner().map(t -> t == Team.WHITE ? "1–0" : "0–1").orElse("½–½"));
        return summary;
    }

    private static String drawReason(ar.uade.tpo.chess.core.model.GameStatus status) {
        return switch (status) {
            case STALEMATE -> "ahogado";
            case DRAW_THREEFOLD_REPETITION -> "triple repetición";
            case DRAW_FIFTY_MOVE_RULE -> "regla de los 50 movimientos";
            case DRAW_INSUFFICIENT_MATERIAL -> "material insuficiente";
            case DRAW_BY_AGREEMENT -> "tablas de común acuerdo";
            default -> status.name();
        };
    }

    private static String decode(String value) { return URLDecoder.decode(value, StandardCharsets.UTF_8); }

    private static void send(HttpExchange e, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        e.getResponseHeaders().set("Cache-Control", "no-store");
        e.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        e.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; style-src 'self'; script-src 'self'; img-src 'self' data: https://cdn.nba.com; frame-ancestors 'none'");
        e.sendResponseHeaders(status, bytes.length); e.getResponseBody().write(bytes);
    }

    /** Convierte a JSON cadenas, valores simples, mapas y listas usados por el adaptador. */
    static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof Map<?, ?> map) return map.entrySet().stream().map(e -> json(e.getKey().toString()) + ":" + json(e.getValue())).collect(Collectors.joining(",", "{", "}"));
        if (value instanceof Collection<?> list) return list.stream().map(WebApp::json).collect(Collectors.joining(",", "[", "]"));
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toString().toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\""); case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r"); case '\t' -> out.append("\\t");
                default -> { if (c < 32) out.append(String.format("\\u%04x", (int)c)); else out.append(c); }
            }
        }
        return out.append('"').toString();
    }
}
