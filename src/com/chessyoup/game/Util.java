package com.chessyoup.game;

import org.goochjs.glicko2.Rating;
import org.goochjs.glicko2.RatingCalculator;
import org.goochjs.glicko2.RatingPeriodResults;

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
}