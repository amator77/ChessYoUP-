package com.chessyoup.game;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.games.multiplayer.Participant;

public class GamePlayer {
	
	private Participant participant; 
	
	private double rating;
	
	private double ratingDeviation;
	
	private double volatility;
	
	private int wins;

	private int draws;

	private int loses;

	public GamePlayer() {
		this.rating = 1500;
		this.ratingDeviation = 150;
		this.volatility = 0;
		this.wins = 0;
		this.draws = 0;
		this.loses = 0;
		
	}
	
	public Participant getParticipant() {
		return participant;
	}

	public void setParticipant(Participant participant) {
		this.participant = participant;
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

	@Override
	public String toString() {
		return "PlayerState [rating=" + rating + ", ratingDeviation="
				+ ratingDeviation + ", volatility=" + volatility + ", wins="
				+ wins + ", draws=" + draws + ", loses=" + loses + "]";
	}
}
