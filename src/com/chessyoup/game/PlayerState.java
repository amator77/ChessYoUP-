package com.chessyoup.game;

import org.goochjs.glicko2.Rating;
import org.json.JSONException;
import org.json.JSONObject;

public class PlayerState {
			
	private Rating rating;

	private int wins;

	private int draws;

	private int loses;

	public PlayerState(String playerId) {
		this.rating = new Rating(playerId, Util.ratingSystem);
		this.wins = 0;
		this.draws = 0;
		this.loses = 0;
	}

	public Rating getRating() {
		return rating;
	}

	public void setRating(Rating rating) {
		this.rating = rating;
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
	
	public String getId(){
		return this.rating.getUid();
	}
	
	public void updateFromJSON(String jsonString) {

		try {
			JSONObject json = new JSONObject(jsonString);
			this.rating.setRating(json.getDouble("elo"));
			this.rating.setRatingDeviation(json.getDouble("rd"));
			this.rating.setVolatility(json.getDouble("vol"));
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
			json.put("elo", this.rating.getRating());
			json.put("rd", this.rating.getRatingDeviation());
			json.put("vol", this.rating.getVolatility());
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
