package com.chessyoup.game.chess;

import org.json.JSONException;
import org.json.JSONObject;

import com.chessyoup.game.GamePlayer;

public class ChessGamePlayer extends GamePlayer {
    
    private double rating;
    
    private double ratingDeviation;
    
    private double volatility;
    
    private int wins;

    private int draws;

    private int loses;
    
    private long rank;
    
    public ChessGamePlayer(){
        this.rating = 1500;
        this.ratingDeviation = 150;
        this.volatility = 0;
        this.wins = 0;
        this.draws = 0;
        this.loses = 0;
        this.rank = 0;
    }
    
    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
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
    
    public long getRank() {
        return rank;
    }

    public void setRank(long rank) {
        this.rank = rank;
    }

    public void updateFromJSON(String jsonString) {

        try {
            JSONObject json = new JSONObject(jsonString);
            this.rating = json.getDouble("elo");
            this.ratingDeviation = json.getDouble("rd");
            this.volatility = json.getDouble("vol");
            this.wins = json.getInt("wins");
            this.draws = json.getInt("draws");
            this.loses = json.getInt("loses");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String toJSON() {
        JSONObject json = new JSONObject();

        try {
            json.put("elo", this.rating);
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
}
