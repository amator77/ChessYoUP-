package com.chessyoup.game.chess;

import org.json.JSONException;
import org.json.JSONObject;

import com.chessyoup.game.GamePlayer;
import com.chessyoup.game.Util;

public class ChessGamePlayer extends GamePlayer {
    
    private long topScore;
    
    private long lowScore;
    
    private double liveRating;
    
    private double ratingDeviation;
    
    private double volatility;
    
    private int wins;

    private int draws;

    private int loses;
    
    private int ratingChange;
        
    public ChessGamePlayer(){  
        this.topScore = Util.TOP_RATING_BASE;
        this.lowScore = Util.LOW_RATING_BASE;
        this.ratingDeviation = Util.DEFAULT_RATING_DEVIATION;        
        this.volatility = Util.DEFAULT_RATING_DEVIATION;
        this.wins = 0;
        this.draws = 0;
        this.loses = 0;
    }
    
    public int getRating() {
        return Util.computeRating(topScore, lowScore);
    }

    public double getRatingDeviation() {
        return ratingDeviation;
    }

    public void setRatingDeviation(double ratingDeviation) {
        this.ratingDeviation = ratingDeviation;
    }

    public double getVolatility() {
        return volatility;
    }

    public void setVolatility(double volatility) {
        this.volatility = volatility;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getDraws() {
        return draws;
    }

    public void setDraws(int draws) {
        this.draws = draws;
    }

    public int getLoses() {
        return loses;
    }

    public void setLoses(int loses) {
        this.loses = loses;
    }

    public void updateFromJSON(String jsonString) {

        try {
            JSONObject json = new JSONObject(jsonString);            
            this.ratingDeviation = json.getDouble("rd");
            this.volatility = json.getDouble("vol");
            this.wins = json.getInt("wins");
            this.draws = json.getInt("draws");
            this.loses = json.getInt("loses");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    public long getTopScore() {
        return topScore;
    }

    public void setTopScore(long topScore) {
        this.topScore = topScore;
    }

    public long getLowScore() {
        return lowScore;
    }

    public void setLowScore(long lowScore) {
        this.lowScore = lowScore;
    }

    public String toJSON() {
        JSONObject json = new JSONObject();

        try {
            json.put("elo", this.getRating());
            json.put("rd", this.ratingDeviation);
            json.put("vol", this.volatility);
            json.put("wins", this.wins);
            json.put("draws", this.draws);
            json.put("loses", this.loses);
            return json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return toString();
        }
    }

    public void setLiveRating(double liveRating) {
        this.liveRating = liveRating;        
    }

    public double getLiveRating() {
        return liveRating;
    }

    
    
    public int getRatingChange() {
        return ratingChange;
    }

    public void setRatingChange(int ratingChange) {
        this.ratingChange = ratingChange;
    }

    @Override
    public String toString() {
        return "ChessGamePlayer [topScore=" + topScore + ", lowScore=" + lowScore + ", liveRating=" + liveRating + ", ratingDeviation=" + ratingDeviation + ", volatility=" + volatility + ", wins="
                        + wins + ", draws=" + draws + ", loses=" + loses + ", ratingChange=" + ratingChange + "]";
    }  
}
