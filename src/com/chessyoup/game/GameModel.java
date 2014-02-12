package com.chessyoup.game;

import com.google.android.gms.games.multiplayer.realtime.Room;

public class GameModel {
	
	private GamePlayer remotePlayer;
			
	private Room room = null;

	private String lastWhitePlayerId = null;

	private String incomingInvitationId = null;
	
	private GameVariant gameVariant;
			
	private GamePlayer whitePlayer;
	
	private GamePlayer blackPlayer;
	
	private boolean ready;
	
	public GameModel(){				
	}
	
	public void reset(){			
		this.lastWhitePlayerId = whitePlayer != null ? whitePlayer.getParticipant().getParticipantId() : null;		
		this.incomingInvitationId = null;
		this.whitePlayer = null;
		this.blackPlayer = null;
		this.ready = false;		
	}

	public GamePlayer getRemotePlayer() {
		return remotePlayer;
	}

	public void setRemotePlayer(GamePlayer remotePlayer) {
		this.remotePlayer = remotePlayer;
	}

	public Room getRoom() {
		return room;
	}

	public void setRoom(Room room) {
		this.room = room;
	}

	public String getLastWhitePlayerId() {
		return lastWhitePlayerId;
	}

	public void setLastWhitePlayerId(String lastWhitePlayerId) {
		this.lastWhitePlayerId = lastWhitePlayerId;
	}

	public String getIncomingInvitationId() {
		return incomingInvitationId;
	}

	public void setIncomingInvitationId(String incomingInvitationId) {
		this.incomingInvitationId = incomingInvitationId;
	}

	public GameVariant getGameVariant() {
		return gameVariant;
	}

	public void setGameVariant(GameVariant gameVariant) {		
		this.gameVariant = gameVariant;
	}

	public GamePlayer getWhitePlayer() {
		return whitePlayer;
	}

	public void setWhitePlayer(GamePlayer whitePlayer) {
		this.whitePlayer = whitePlayer;
	}

	public GamePlayer getBlackPlayer() {
		return blackPlayer;
	}

	public void setBlackPlayer(GamePlayer blackPlayer) {
		this.blackPlayer = blackPlayer;
	}

	public boolean isReady() {
		return ready;
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

	public void setRemoteRating(double remoteElo, double remoteRd,
			double volatility) {
		
		this.remotePlayer.setRating(remoteElo);
		this.remotePlayer.setRatingDeviation(remoteRd);
		this.remotePlayer.setVolatility(volatility);
	}
}
