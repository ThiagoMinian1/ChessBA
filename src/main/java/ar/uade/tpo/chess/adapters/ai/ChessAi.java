package ar.uade.tpo.chess.adapters.ai;

import ar.uade.tpo.chess.adapters.nba.*;
import ar.uade.tpo.chess.core.game.*;
import ar.uade.tpo.chess.core.model.*;
import java.util.*;

/** Rival local: búsqueda minimax con poda alfa-beta sobre una copia aislada de la partida. */
public final class ChessAi {
    public enum Difficulty {
        EASY(1, 400, 500), MEDIUM(2, 1800, 1000), HARD(3, 5000, 1800);
        final int depth, nodes, millis;
        Difficulty(int depth, int nodes, int millis) { this.depth=depth; this.nodes=nodes; this.millis=millis; }
    }
    public record Choice(Move move, Position square, PieceType benchPiece) {
        public void play(ChessGame game) {
            if (move != null) game.play(move); else game.execute(new TradeAction(square, benchPiece));
        }
    }
    private static final class Limit extends RuntimeException { }
    private final Random random;
    public ChessAi() { this(new Random()); }
    public ChessAi(Random random) { this.random=Objects.requireNonNull(random); }

    public Choice choose(ChessGame original, GameConfig config, Difficulty difficulty) {
        ChessGame game=NbaGameFactory.decorate(original.copyForAnalysis(), config);
        if (game.status().isOver()) throw new IllegalStateException("La partida terminó.");
        Team side=game.currentTurn();
        List<Choice> roots=choices(game, config);
        if (roots.isEmpty()) throw new IllegalStateException("No hay jugadas legales.");
        Collections.shuffle(roots, random);
        roots.sort(Comparator.comparingInt((Choice c)->priority(game,c)).reversed());
        Search search=new Search(side,config,difficulty);
        Choice best=roots.getFirst();
        for(int depth=1;depth<=difficulty.depth;depth++) {
            Choice iterationBest=best; int score=Integer.MIN_VALUE; boolean completed=true;
            List<Map.Entry<Choice,Integer>> easy=new ArrayList<>();
            for(Choice choice:roots) {
                boolean applied=false;
                try {
                    search.check(); choice.play(game); applied=true;
                    int value=search.visit(game,depth-1,-2_000_000,2_000_000,1);
                    easy.add(Map.entry(choice,value));
                    if(value>score){score=value;iterationBest=choice;}
                } catch(Limit limit) { completed=false; break; }
                finally { if(applied)game.undo(); }
            }
            if(!completed)break;
            best=iterationBest;
            if(difficulty==Difficulty.EASY && score<900_000) {
                final int threshold=score-100;
                List<Choice> nearby=easy.stream().filter(e->e.getValue()>=threshold).map(Map.Entry::getKey).toList();
                best=nearby.get(random.nextInt(nearby.size()));
            }
            roots.remove(best);roots.addFirst(best);
        }
        return best;
    }
    private static final class Search {
        final Team side; final GameConfig config; final Difficulty level; final long deadline; int nodes;
        Search(Team side,GameConfig config,Difficulty level){this.side=side;this.config=config;this.level=level;deadline=System.nanoTime()+level.millis*1_000_000L;}
        void check(){if(++nodes>level.nodes||System.nanoTime()>deadline||Thread.currentThread().isInterrupted())throw new Limit();}
        int visit(ChessGame game,int depth,int alpha,int beta,int ply){
            check();
            if(game.status().isOver())return game.winner().map(t->t==side?1_000_000-ply:-1_000_000+ply).orElse(0);
            if(depth==0)return evaluate(game,side,config);
            boolean maximizing=game.currentTurn()==side;
            int best=maximizing?-2_000_000:2_000_000;
            List<Choice> options=choices(game,config);
            options.sort(Comparator.comparingInt((Choice c)->priority(game,c)).reversed());
            for(Choice option:options){
                boolean applied=false; int value;
                try{option.play(game);applied=true;value=visit(game,depth-1,alpha,beta,ply+1);}
                finally{if(applied)game.undo();}
                best=maximizing?Math.max(best,value):Math.min(best,value);
                if(maximizing)alpha=Math.max(alpha,best);else beta=Math.min(beta,best);
                if(beta<=alpha)break;
            }
            return best;
        }
    }
    private static List<Choice> choices(ChessGame game,GameConfig config){
        List<Choice> result=new ArrayList<>();
        for(Move move:game.legalMoves())result.add(new Choice(move,null,null));
        Team side=game.currentTurn();
        if(config.tradeEnabled()&&!game.status().isOver()&&!game.isInCheck()&&!TradeAction.hasTraded(game,side)) {
            Set<PieceType> bench=new HashSet<>();game.bench(side).forEach(p->bench.add(p.type()));
            for(PlacedPiece placed:game.board().pieces(side))if(placed.piece().type()!=PieceType.KING)
                for(PieceType type:bench)if(type!=PieceType.KING&&!(type==PieceType.PAWN&&(placed.position().rank()==0||placed.position().rank()==7)))
                    result.add(new Choice(null,placed.position(),type));
        }
        return result;
    }
    private static int priority(ChessGame game,Choice choice){
        if(choice.move==null)return value(choice.benchPiece)-game.board().pieceAt(choice.square).map(p->value(p.type())).orElse(0);
        return game.board().pieceAt(choice.move.to()).map(p->value(p.type())).orElse(choice.move.type()==MoveType.EN_PASSANT?100:0)
                +(choice.move.promotion()==null?0:value(choice.move.promotion()));
    }
    private static int evaluate(ChessGame game,Team side,GameConfig config){
        int total=0;
        for(PlacedPiece placed:game.board().pieces()){
            Piece p=placed.piece();Position pos=placed.position();
            int central=14-(Math.abs(2*pos.file()-7)+Math.abs(2*pos.rank()-7));
            int bonus=switch(p.type()){
                case PAWN -> (p.team()==Team.WHITE?pos.rank():7-pos.rank())*7;
                case KNIGHT,BISHOP -> central*4;
                case KING -> 0;
                default -> central;
            };
            total+=(p.team()==side?1:-1)*(value(p.type())+bonus);
        }
        if(config.scoreboardEnabled())total+=40*(ScoreboardGame.scoreOf(game,side)-ScoreboardGame.scoreOf(game,side.opposite()));
        if(config.hotHandEnabled())total+=20*(HotHandGame.streakOf(game,side)-HotHandGame.streakOf(game,side.opposite()));
        if(config.shotClockEnabled())total+=8*(ShotClockGame.stateOf(game,side.opposite()).movesWithoutProgress()-ShotClockGame.stateOf(game,side).movesWithoutProgress());
        return total;
    }
    private static int value(PieceType type){return switch(type){case PAWN->100;case KNIGHT->320;case BISHOP->330;case ROOK->500;case QUEEN->900;case KING->20_000;};}
}
