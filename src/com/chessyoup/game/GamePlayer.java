package com.chessyoup.game;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.games.multiplayer.Participant;

public class GamePlayer {
	
	private Participant participant; 
		
	public GamePlayer() {		
	}
	
	public Participant getParticipant() {
		return participant;
	}

	public void setParticipant(Participant participant) {
		this.participant = participant;
	}
}
