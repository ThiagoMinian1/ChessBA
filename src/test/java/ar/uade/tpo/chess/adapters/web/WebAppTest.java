package ar.uade.tpo.chess.adapters.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class WebAppTest {
    private WebApp app;
    private HttpClient client;
    private String base;

    @BeforeEach void start() throws Exception {
        app = new WebApp(0); app.start(); base = "http://127.0.0.1:" + app.port();
        client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    }
    @AfterEach void stop() { client.close(); app.stop(); }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> post(String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + "/api/" + path)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void servesPackagedInterfaceAndRejectsUnknownResources() throws Exception {
        var page = get("/"); assertEquals(200, page.statusCode()); assertTrue(page.body().contains("Tu próxima gran jugada"));
        assertTrue(get("/app.js").body().contains("/api/"));
        assertTrue(get("/style.css").headers().firstValue("Content-Type").orElseThrow().startsWith("text/css"));
        assertEquals(404, get("/pom.xml").statusCode());
    }

    @Test void playsRealGameAndRestoresMateThroughUndoRedo() throws Exception {
        assertTrue(get("/api/state").body().contains("\"turn\":\"WHITE\""));
        for (String move : new String[]{"f2f3", "e7e5", "g2g4", "d8h4"}) {
            var response = post("move", "from=" + move.substring(0, 2) + "&to=" + move.substring(2));
            assertEquals(200, response.statusCode(), response.body());
        }
        String mate = get("/api/state").body();
        assertTrue(mate.contains("\"status\":\"CHECKMATE\"")); assertTrue(mate.contains("\"winner\":\"BLACK\""));
        assertTrue(post("undo", "").body().contains("\"status\":\"ONGOING\""));
        assertTrue(post("redo", "").body().contains("\"status\":\"CHECKMATE\""));
    }

    @Test void illegalMovesAreRejectedWithoutChangingState() throws Exception {
        String before = get("/api/state").body();
        assertEquals(400, post("move", "from=e2&to=e5").statusCode());
        assertEquals(before, get("/api/state").body());
        assertEquals(400, post("move", "from=invalid&to=e4").statusCode());
        assertEquals(400, post("trade", "square=a1&piece=QUEEN").statusCode());
        assertEquals(before, get("/api/state").body());
    }

    @Test void browserSessionsAreIndependent() throws Exception {
        assertEquals(200, post("move", "from=e2&to=e4").statusCode());
        try (var other = HttpClient.newHttpClient()) {
            var fresh = other.send(HttpRequest.newBuilder(URI.create(base + "/api/state")).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertTrue(fresh.body().contains("\"turn\":\"WHITE\""));
            assertTrue(get("/api/state").body().contains("\"turn\":\"BLACK\""));
        }
    }

    @Test void configuresNbaAndRejectsDuplicateFranchisesAtomically() throws Exception {
        String nba = post("new", "mode=nba&white=LAL&black=BOS").body();
        assertTrue(nba.contains("Los Angeles Lakers")); assertTrue(nba.contains("Boston Celtics"));
        assertTrue(nba.contains("\"shotClock\":true")); assertTrue(nba.contains("\"trade\":true"));
        assertEquals(400, post("new", "mode=nba&white=LAL&black=LAL").statusCode());
        assertEquals(nba, get("/api/state").body());
        assertTrue(get("/api/franchises").body().contains("San Antonio Spurs"));
    }

    @Test void appliesCustomMechanicsAndAgreementAndResignation() throws Exception {
        String custom = post("new", "mode=custom&white=LAL&black=BOS&shotClock=true&limit=3&hotHand=false&trade=false").body();
        assertTrue(custom.contains("\"limit\":3")); assertTrue(custom.contains("\"hotHand\":false"));
        assertTrue(post("draw", "").body().contains("DRAW_BY_AGREEMENT"));
        post("undo", "");
        String resignation = post("resign", "").body();
        assertTrue(resignation.contains("RESIGNATION")); assertTrue(resignation.contains("\"winner\":\"BLACK\""));
    }

    @Test void rejectsCrossOriginMutationsAndWrongMethods() throws Exception {
        var response = client.send(HttpRequest.newBuilder(URI.create(base + "/api/new")).header("Origin", "https://example.org")
                .POST(HttpRequest.BodyPublishers.ofString("mode=standard")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode()); assertEquals(405, get("/api/new").statusCode());
        assertEquals(413, post("new", "x=" + "a".repeat(8193)).statusCode());
    }

    @Test void escapesJsonStrings() {
        assertEquals("\"a\\\"b\\\\c\\n\\t\\u0001\"", WebApp.json("a\"b\\c\n\t\u0001"));
    }
}
