package com.chessyoup.game;

import org.goochjs.glicko2.Rating;
import org.goochjs.glicko2.RatingCalculator;
import org.goochjs.glicko2.RatingPeriodResults;

import com.chessyoup.game.chess.ChessGamePlayer;
import com.chessyoup.game.chess.ChessGameVariant;

public class Util {
	
    public static long TOP_RATING_BASE = 1500;
    public static long LOW_RATING_BASE = 2000000000;
    public static long DEFAULT_RATING_DEVIATION = 150;
    public static long DEFAULT_RATING_VOLATILITY = 0;
    
	public static RatingCalculator ratingSystem = new RatingCalculator(0.06, 0.5);
	
	public static final void updateRatingsOnResult(ChessGamePlayer winner, ChessGamePlayer louser ){                      
        Rating winnerRating = getRating(winner);
        Rating louserRating = getRating(louser);
        RatingPeriodResults results = new RatingPeriodResults();
        results.addResult(winnerRating, louserRating);
        ratingSystem.updateRatings(results);        
        int winnerGain =  (int)winnerRating.getRating() - winner.getRating();
        int louserLoss = louser.getRating() - (int)louser.getRating();
        winner.setTopScore(winner.getTopScore()+winnerGain);                
        winner.setRatingDeviation(winnerRating.getRatingDeviation());
        winner.setVolatility(winnerRating.getVolatility());
        winner.setRatingChange(winnerGain);
        
        louser.setLowScore(louser.getLowScore()-louserLoss);
        louser.setRatingDeviation(louserRating.getRatingDeviation());
        louser.setVolatility(louserRating.getVolatility());      
        louser.setRatingChange(louserLoss);
    }
	
	public static final void updateRatingsOnDraw(ChessGamePlayer whitePlayer, ChessGamePlayer blackPlayer ){
	    Rating whiteRating = getRating(whitePlayer);
        Rating blackRating = getRating(blackPlayer);
        RatingPeriodResults results = new RatingPeriodResults();
        results.addDraw(whiteRating, blackRating);
        ratingSystem.updateRatings(results);        
        int whiteDiff =  (int)whiteRating.getRating() - whitePlayer.getRating();
        int blackDiff = blackPlayer.getRating() - (int)blackRating.getRating();
        
        if( whiteDiff > 0 ){
            whitePlayer.setTopScore(whitePlayer.getTopScore()+whiteDiff);
        }
        else{
            whitePlayer.setLowScore(whitePlayer.getLowScore()-whiteDiff);
        }
        
        whitePlayer.setRatingChange(whiteDiff);
        whitePlayer.setRatingDeviation(whiteRating.getRatingDeviation());
        whitePlayer.setVolatility(whiteRating.getVolatility());
        
        if( blackDiff > 0 ){
            blackPlayer.setTopScore(blackPlayer.getTopScore()+blackDiff);
        }
        else{
            blackPlayer.setLowScore(blackPlayer.getLowScore()-blackDiff);
        }
        
        blackPlayer.setRatingChange(blackDiff);
        blackPlayer.setRatingDeviation(blackRating.getRatingDeviation());
        blackPlayer.setVolatility(blackRating.getVolatility());
	}	
	
	public static final int computeRating(long topScore,long lowScore){	    
	    long gainedPoints = topScore - TOP_RATING_BASE;
	    long lostPoints = LOW_RATING_BASE - lowScore;
	    long netPoints = gainedPoints - lostPoints;	    
	    return (int)(TOP_RATING_BASE + netPoints);
	}
	
	/**
	 * Calculate game variant code.This is using on invitation or game mathicng criteria.
	 * @param gameType - values : 1 for Normal Game , 2 for chess960 .. etc
	 * @param time - in seconds
	 * @param increment - in seconds
	 * @param movesNumber - total numbers of moves in time
	 * @param isRated - if is an rated game
	 * @param isWhite - if owner is white player
	 * @return
	 */
	public static int getGameVariant(int gameType,int time,int increment,int movesNumber,boolean isRated,boolean isWhite){
		StringBuffer sb = new StringBuffer(String.valueOf(time/1000));
		sb.append(gameType);
		sb.append(fill(String.valueOf(time),3));
		sb.append(fill(String.valueOf(increment),2));
		sb.append(fill(String.valueOf(movesNumber),2));
		sb.append(isRated ? "1" : "0");
		sb.append(isWhite ? "1" : "0");
		return Integer.parseInt(sb.toString());
	}
	
	public static int gameVariantToInt(ChessGameVariant gv){		
		return getGameVariant( gv.getType() , gv.getTime() , gv.getIncrement() , gv.getMoves() , gv.isRated() ,gv.isWhite());
	}
	
	public static int switchSide(int gameVariant){
		ChessGameVariant gv = Util.getGameVariant(gameVariant);
		gv.setWhite(gv.isWhite() ? false : true );
		return gameVariantToInt(gv);
	}
	
	public static int switchSide(ChessGameVariant gameVariant){
		ChessGameVariant gv = getGameVariant(Util.gameVariantToInt(gameVariant));
		gv.setWhite(gv.isWhite() ? false : true );
		return gameVariantToInt(gv);
	}
	
	public static ChessGameVariant getGameVariant(int variant){
		String s = String.valueOf(variant);
		int gameType = Integer.parseInt(s.substring(0, 1));
		int time = Integer.parseInt(s.substring(1, 4));
		int increment = Integer.parseInt(s.substring(4, 6));
		int moves = Integer.parseInt(s.substring(6, 8));
		boolean isRated = Integer.parseInt(s.substring(8, 9)) == 1;
		boolean isWhite = Integer.parseInt(s.substring(9, 10)) == 1;
		
		ChessGameVariant gv = new ChessGameVariant();		
		gv.setType(gameType);
		gv.setTime(time);
		gv.setIncrement(increment);
		gv.setMoves(moves);
		gv.setRated(isRated);
		gv.setWhite(isWhite);
				
		return gv;
	}
	
	private static String fill(String value,int size){
		StringBuffer sb = new StringBuffer();
		
		for( int i = 0; i < size ; i++){
			if( (sb.length() + value.length())  < size ){
				sb.append("0");				
			}
		}
		
		return sb.append(value).toString();
	}
	
	private static Rating getRating(ChessGamePlayer player){
	    Rating rating = new Rating(player.getPlayer().getPlayerId(), ratingSystem);
        rating.setRating(player.getRating());
        rating.setRatingDeviation(player.getRatingDeviation());
        rating.setVolatility(player.getVolatility());        
        
        return rating;
	}
}