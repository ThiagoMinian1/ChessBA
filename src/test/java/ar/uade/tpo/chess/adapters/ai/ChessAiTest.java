package ar.uade.tpo.chess.adapters.ai;

import ar.uade.tpo.chess.adapters.nba.*;
import ar.uade.tpo.chess.core.game.*;
import ar.uade.tpo.chess.core.model.*;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static ar.uade.tpo.chess.TestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class ChessAiTest {
    @Test void everyDifficultyChoosesALegalMoveWithoutTouchingHistoryOrRedo(){
        for(var level:ChessAi.Difficulty.values()){
            ChessGame game=new Game();play(game,"e2e4","e7e5");game.undo();
            var before=game.history();var legal=game.legalMoves();
            var choice=new ChessAi(new Random(1)).choose(game,GameConfig.standard(),level);
            assertTrue(legal.contains(choice.move()));assertEquals(before,game.history());assertTrue(game.canRedo());
            assertTrue(game.redo());assertEquals(Team.WHITE,game.currentTurn());
        }
    }
    @Test void findsMateInOne(){
        ChessGame game=gameOf(Team.WHITE,"Kf6","Qg6","kh8");
        var choice=new ChessAi(new Random(1)).choose(game,GameConfig.standard(),ChessAi.Difficulty.MEDIUM);
        choice.play(game);assertEquals(GameStatus.CHECKMATE,game.status());assertEquals(Team.WHITE,game.winner().orElseThrow());
    }
    @Test void analysisCopyRetainsRepetitionAndNbaCounters(){
        GameConfig config=GameConfig.builder().shotClock(3).hotHand().scoreboard().trade().build();
        ChessGame game=NbaGameFactory.create(config);play(game,"e2e4","d7d5","e4d5");
        var before=game.history();int score=ScoreboardGame.scoreOf(game,Team.WHITE);
        var choice=new ChessAi(new Random(2)).choose(game,config,ChessAi.Difficulty.MEDIUM);
        assertEquals(before,game.history());assertEquals(score,ScoreboardGame.scoreOf(game,Team.WHITE));
        choice.play(game);assertEquals(before.size()+1,game.history().size());game.undo();assertEquals(before,game.history());
        ChessGame standard=new Game();play(standard,"g1f3","g8f6","f3g1","f6g8","g1f3","g8f6","f3g1");
        ChessGame copy=standard.copyForAnalysis();play(copy,"f6g8");assertEquals(GameStatus.DRAW_THREEFOLD_REPETITION,copy.status());
        assertEquals(GameStatus.ONGOING,standard.status());
    }
    @Test void rejectsFinishedGame(){
        ChessGame game=new Game();game.agreeDraw();
        assertThrows(IllegalStateException.class,()->new ChessAi().choose(game,GameConfig.standard(),ChessAi.Difficulty.EASY));
    }
}
