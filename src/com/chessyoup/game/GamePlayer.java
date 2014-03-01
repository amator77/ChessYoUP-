package com.chessyoup.game;

import com.google.android.gms.games.Player;
import com.google.android.gms.games.multiplayer.Participant;

public class GamePlayer {
	
	private Player player;
	
	private Participant participant;
		
	public GamePlayer() {		
	}

	public Participant getParticipant() {
		return participant;
	}

	public void setParticipant(Participant participant) {
		this.participant = participant;
		this.player = participant.getPlayer();
	}
	
	public void setPlayer(Player player) {
		this.player = player;
	}

	public Player getPlayer() {
        return this.player;
    }
}
