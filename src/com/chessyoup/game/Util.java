package com.chessyoup.game;

import org.goochjs.glicko2.Rating;
import org.goochjs.glicko2.RatingCalculator;
import org.goochjs.glicko2.RatingPeriodResults;

import com.chessyoup.game.chess.ChessGameVariant;

public class Util {
	
	public static RatingCalculator ratingSystem = new RatingCalculator(0.06, 0.5);
	
	public static final void computeRatingOnResult(Rating winner, Rating loser ){				
		RatingPeriodResults results = new RatingPeriodResults();
		results.addResult(winner, loser);
		ratingSystem.updateRatings(results);
	}
	
	public static final void computeRatingOnDraw(Rating player1, Rating player2 ){				
		RatingPeriodResults results = new RatingPeriodResults();
		results.addDraw(player1, player2);
		ratingSystem.updateRatings(results);
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

	public static void showGameError() {
		// TODO Auto-generated method stub
		
	}
}