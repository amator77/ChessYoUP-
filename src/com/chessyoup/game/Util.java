package com.chessyoup.game;

import org.goochjs.glicko2.Rating;
import org.goochjs.glicko2.RatingCalculator;
import org.goochjs.glicko2.RatingPeriodResults;

import com.chessyoup.model.TimeControl;

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
	
	public int getGameVariant(int time , int increment,boolean rated){
		StringBuffer sb = new StringBuffer(String.valueOf(time/1000));
		sb.append(String.valueOf(increment/1000));
		sb.append(rated ? "1" : "0");		
		return Integer.parseInt(sb.toString());
	}
	
	public TimeControl getTimeControll(int variant){			
		return null;
	}
}