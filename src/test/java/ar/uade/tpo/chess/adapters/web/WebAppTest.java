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

    private HttpResponse<String> agreeDraw() throws Exception {
        String state = post("draw-offer", "").body();
        String rival = state.contains("\"drawOffer\":\"WHITE\"") ? "BLACK" : "WHITE";
        return post("draw-response", "team=" + rival + "&response=accept");
    }
    @Test void servesPackagedInterfaceAndRejectsUnknownResources() throws Exception {
        var page = get("/"); assertEquals(200, page.statusCode()); assertTrue(page.body().contains("Tu próxima gran jugada"));
        assertTrue(get("/app.js").body().contains("/api/"));
        assertTrue(get("/style.css").headers().firstValue("Content-Type").orElseThrow().startsWith("text/css"));
        assertEquals(404, get("/pom.xml").statusCode());
        for(String name:new String[]{"bounce","swish","crowd"}) {
            var sound=client.send(HttpRequest.newBuilder(URI.create(base+"/"+name+".mp3")).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200,sound.statusCode());
            assertEquals("audio/mpeg",sound.headers().firstValue("Content-Type").orElseThrow());
            assertTrue(sound.body().length>1000);
            assertEquals(405,client.send(HttpRequest.newBuilder(URI.create(base+"/"+name+".mp3")).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.discarding()).statusCode());
        }
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

    @Test void nbaShotClockAcceptsManualLimitAndRejectsInvalidValuesAtomically() throws Exception {
        var response = post("new", "mode=nba&white=LAL&black=BOS&limit=7");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"limit\":7"));
        assertTrue(response.body().contains("\"shotClock\":true"));
        String before = get("/api/state").body();
        for (String invalid : new String[]{"0", "25", "abc", "2.5"}) {
            assertEquals(400, post("new", "mode=nba&white=LAL&black=BOS&limit=" + invalid).statusCode());
            assertEquals(before, get("/api/state").body());
        }
        for (int limit : new int[]{1, 24}) {
            var boundary = post("new", "mode=nba&white=LAL&black=BOS&limit=" + limit);
            assertEquals(200, boundary.statusCode());
            assertTrue(boundary.body().contains("\"limit\":" + limit + ","));
        }
        String html = get("/").body();
        assertTrue(html.contains("max=\"24\""));
        assertTrue(html.contains("jugadas propias, no segundos"));
    }

    @Test void themeOnlyModeUsesFranchisesWithStrictlyStandardRules() throws Exception {
        var response=post("new","mode=theme&white=LAL&black=BOS&shotClock=true&trade=true&hotHand=true&scoreboard=true&limit=24");
        assertEquals(200,response.statusCode());
        String themed=response.body();
        assertTrue(themed.contains("\"mode\":\"theme\""));
        assertTrue(themed.contains("Los Angeles Lakers"));
        for(String flag:new String[]{"shotClock","trade","hotHand","scoreboard"}) assertTrue(themed.contains("\""+flag+"\":false"));
        String played=moves("e2e4","d7d5","e4d5","c7c6","d5c6");
        assertTrue(played.contains("\"turn\":\"BLACK\""));
        assertTrue(played.contains("\"whiteScore\":0"));
        assertEquals(400,post("trade","square=g8&piece=PAWN").statusCode());
        assertEquals(played,get("/api/state").body());
        String page=get("/").body();
        assertTrue(page.contains("id=\"homeTheme\""));
        assertTrue(page.contains("name=\"mode\" value=\"theme\""));
    }

    @Test void appliesCustomMechanicsAndAgreementAndResignation() throws Exception {
        String custom = post("new", "mode=custom&white=LAL&black=BOS&shotClock=true&limit=3&hotHand=false&trade=false").body();
        assertTrue(custom.contains("\"limit\":3")); assertTrue(custom.contains("\"hotHand\":false"));
        assertTrue(agreeDraw().body().contains("DRAW_BY_AGREEMENT"));
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

    @Test void drawOfferRequiresRivalAcceptanceAndCanBeRejectedOrCancelledByMove() throws Exception {
        String offered=post("draw-offer", "").body();
        assertTrue(offered.contains("\"drawOffer\":\"WHITE\""));
        assertTrue(offered.contains("\"status\":\"ONGOING\""));
        assertEquals(400,post("draw-response","team=WHITE&response=accept").statusCode());
        assertEquals(offered,get("/api/state").body());
        assertTrue(post("draw-response","team=BLACK&response=reject").body().contains("\"drawOffer\":\"\""));
        assertTrue(get("/api/state").body().contains("\"status\":\"ONGOING\""));
        post("draw-offer", "");
        assertTrue(moves("e2e4").contains("\"drawOffer\":\"\""));
        assertEquals(400,post("draw-response","team=BLACK&response=accept").statusCode());
        post("draw-offer", "");
        String accepted=post("draw-response","team=WHITE&response=accept").body();
        assertTrue(accepted.contains("DRAW_BY_AGREEMENT"));
        assertTrue(accepted.contains("\"drawOffer\":\"\""));
        assertTrue(post("undo", "").body().contains("\"status\":\"ONGOING\""));
        assertTrue(post("redo", "").body().contains("DRAW_BY_AGREEMENT"));
    }

    @Test void escapesJsonStrings() {
        assertEquals("\"a\\\"b\\\\c\\n\\t\\u0001\"", WebApp.json("a\"b\\c\n\t\u0001"));
    }

    private String moves(String... moves) throws Exception {
        String result = "";
        for (String move : moves) {
            var response = post("move", "from=" + move.substring(0,2) + "&to=" + move.substring(2,4)
                    + (move.length()>4 ? "&promotion=" + move.substring(4) : ""));
            assertEquals(200, response.statusCode(), response.body());
            result = response.body();
        }
        return result;
    }

    @Test void webSupportsCastlingAndRestoresBothPieces() throws Exception {
        moves("e2e4","e7e5","g1f3","b8c6","f1c4","g8f6");
        String before = get("/api/state").body();
        assertTrue(before.contains("\"halfmoveClock\":4"));
        String castled = moves("e1g1");
        assertTrue(castled.matches("(?s).*\\{[^}]*\"square\":\"g1\"[^}]*\\}.*"));
        assertFalse(castled.matches("(?s).*\\{[^}]*\"square\":\"h1\"[^}]*\\}.*"));
        post("undo", "");
        String undone=get("/api/state").body();
        assertTrue(undone.contains("\"square\":\"e1\""));
        assertTrue(undone.contains("\"square\":\"h1\""));
        assertEquals(castled, post("redo", "").body());
    }

    @Test void webSupportsEnPassantWithPointsAndUndo() throws Exception {
        post("new", "mode=custom&white=LAL&black=BOS&scoreboard=true");
        moves("e2e4","a7a6","e4e5","d7d5");
        String captured=moves("e5d6");
        assertTrue(captured.contains("\"whiteScore\":1"));
        assertFalse(captured.contains("\"square\":\"d5\""));
        assertTrue(post("undo", "").body().contains("\"whiteScore\":0"));
        assertEquals(captured,post("redo", "").body());
    }

    @Test void webOffersAllFourPromotionsAndAppliesUnderpromotion() throws Exception {
        moves("a2a4","h7h5","a4a5","h5h4","a5a6","h4h3","a6b7","h3g2");
        String ready=get("/api/state").body();
        for(String type:new String[]{"QUEEN","ROOK","BISHOP","KNIGHT"}) assertTrue(ready.contains("\"promotion\":\""+type+"\""));
        assertEquals(400,post("move","from=b7&to=a8").statusCode());
        assertEquals(ready,get("/api/state").body());
        String promoted=moves("b7a8KNIGHT");
        assertTrue(promoted.contains("\"square\":\"a8\""));
        assertTrue(post("undo", "").body().contains("\"square\":\"b7\""));
        assertEquals(promoted,post("redo", "").body());
    }

    @Test void webShotClockAndHotHandExposeRealExtraTurns() throws Exception {
        post("new","mode=custom&white=LAL&black=BOS&shotClock=true&limit=1");
        assertTrue(moves("g1f3").contains("\"whiteClockExpired\":true"));
        String skipped=moves("g8f6");
        assertTrue(skipped.contains("\"turn\":\"BLACK\""));
        assertTrue(skipped.contains("shot clock: opponent's turn skipped"));
        assertTrue(post("undo", "").body().contains("\"whiteClockExpired\":true"));
        assertEquals(skipped,post("redo", "").body());
        post("new","mode=custom&white=LAL&black=BOS&hotHand=true");
        String extra=moves("e2e4","d7d5","e4d5","c7c6","d5c6");
        assertTrue(extra.contains("\"turn\":\"WHITE\""));
        assertTrue(extra.contains("hot hand: extra move"));
        assertTrue(post("undo", "").body().contains("\"whiteStreak\":1"));
        assertEquals(extra,post("redo", "").body());
    }

    @Test void webTradeConsumesTurnWithoutScoringAndRejectsPawnOnBackRank() throws Exception {
        post("new","mode=custom&white=LAL&black=BOS&trade=true&scoreboard=true");
        moves("e2e4","d7d5","e4d5","d8d5");
        String before=get("/api/state").body();
        assertEquals(400,post("trade","square=g1&piece=PAWN").statusCode());
        assertEquals(before,get("/api/state").body());
        var trade=post("trade","square=f2&piece=PAWN");
        assertEquals(200,trade.statusCode(),trade.body());
        assertTrue(trade.body().contains("\"whiteTraded\":true"));
        assertTrue(trade.body().contains("\"turn\":\"BLACK\""));
        assertTrue(trade.body().contains("\"whiteScore\":1"));
        assertTrue(post("undo", "").body().contains("\"whiteTraded\":false"));
        assertEquals(trade.body(),post("redo", "").body());
    }

    @Test void scoreboardPointsComeFromTheBackendAndFollowUndoRedo() throws Exception {
        var configured = post("new", "mode=custom&white=LAL&black=BOS&scoreboard=true");
        assertEquals(200, configured.statusCode());
        assertTrue(configured.body().contains("\"scoreboard\":true"));
        for (String move : new String[]{"e2e4","d7d5","e4d5","d8d5","b1c3","d5g2","f1g2"}) {
            var response = post("move", "from=" + move.substring(0,2) + "&to=" + move.substring(2));
            assertEquals(200, response.statusCode(), response.body());
        }
        String captured = get("/api/state").body();
        assertTrue(captured.contains("\"whiteScore\":10"));
        assertTrue(captured.contains("\"blackScore\":2"));
        assertTrue(captured.contains("\"result\":null"));
        assertTrue(post("undo", "").body().contains("\"whiteScore\":1"));
        assertTrue(post("redo", "").body().contains("\"whiteScore\":10"));
        assertEquals(400, post("move", "from=a1&to=a8").statusCode());
        assertEquals(captured, get("/api/state").body());
    }

    @Test void resultEndpointDistinguishesAgreedDrawsFromPointsVictories() throws Exception {
        assertEquals(409, get("/api/result").statusCode());
        assertEquals(405, post("result", "").statusCode());
        post("new", "mode=custom&white=LAL&black=BOS&scoreboard=true");
        for(String move : new String[]{"e2e4","d7d5","e4d5"}) post("move", "from="+move.substring(0,2)+"&to="+move.substring(2));
        agreeDraw();
        var result=get("/api/result");
        assertEquals(200,result.statusCode());
        assertTrue(result.body().contains("\"winner\":\"\""));
        assertTrue(result.body().contains("\"decidedOnPoints\":false"));
        assertTrue(result.body().contains("\"whiteScore\":1"));
        assertTrue(result.body().contains("\"actions\":4"));
        assertTrue(result.body().contains("\"blackPieces\":15"));
        post("undo", "");assertEquals(409,get("/api/result").statusCode());
        post("redo", "");assertEquals(result.body(),get("/api/result").body());
    }

    @Test void pointDecidedDrawProducesTheCorrectResultSummary() {
        var game=ar.uade.tpo.chess.adapters.nba.NbaGameFactory.decorate(
                ar.uade.tpo.chess.TestSupport.gameOf(ar.uade.tpo.chess.core.model.Team.WHITE,"Ka1","Bc1","kh8","nf4"),
                ar.uade.tpo.chess.adapters.nba.GameConfig.builder().scoreboard().build());
        ar.uade.tpo.chess.TestSupport.play(game,"c1f4");
        var result=WebApp.gameResult(game,true,"Local","Visitante");
        assertEquals("WHITE",result.get("winner"));assertEquals("Local",result.get("winnerName"));
        assertEquals(true,result.get("decidedOnPoints"));assertEquals(3,result.get("whiteScore"));
        assertTrue(result.get("reason").toString().startsWith("Victoria por puntos"));
        game.undo();assertNull(WebApp.gameResult(game,true,"Local","Visitante"));
    }

    @Test void homePageStartsWithTheGameHiddenAndTracksExplicitStarts() throws Exception {
        assertTrue(get("/").body().contains("id=\"gameScreen\" hidden"));
        assertTrue(get("/api/state").body().contains("\"started\":false"));
        var fresh=post("new","mode=nba&white=LAL&black=BOS");
        assertTrue(fresh.body().contains("\"started\":true"));
        assertTrue(fresh.body().contains("\"scoreboard\":true"));
        assertTrue(fresh.body().contains("\"whiteScore\":0"));
        assertTrue(post("new","mode=standard").body().contains("\"scoreboard\":false"));
    }
}
