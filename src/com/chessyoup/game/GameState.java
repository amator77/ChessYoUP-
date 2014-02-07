package com.chessyoup.game;

import org.goochjs.glicko2.Rating;

import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.Room;

public class GameState {
	
	private PlayerState owner;
		
	private Room room = null;

	private String myId = null;

	private String remoteId = null;

	private String lastWhitePlayerId = null;

	private String incomingInvitationId = null;
	
	private GameVariant gameVariant;
	
	private boolean waitRoomDismissedFromCode = false;
	
	private Rating remoteRating;
	
	private String whitePlayerId;
	
	private String blackPlayerId;
	
	private boolean ready;
	
	private boolean started; 
	
	public GameState(PlayerState ownerState){
		this.owner = ownerState;			
	}
	
	public void reset(){
		this.room = null;
		this.myId = null;
		this.remoteId = null;
		this.lastWhitePlayerId = null;
		this.incomingInvitationId = null;
		this.waitRoomDismissedFromCode = false;		
		this.gameVariant = null;
		this.remoteRating = null;
		this.whitePlayerId = null;
		this.blackPlayerId = null;
		this.ready = false;
		this.started = false;
	}
			
	public boolean isLocalPlayerRoomOwner(){
		if( this.myId != null && this.room != null ){
			return myId.equals(this.room.getCreatorId());
		}
		else{
			return false;
		}		
	}

	public PlayerState getOwner() {
		return owner;
	}

	public void setOwner(PlayerState owner) {
		this.owner = owner;
	}

	public Room getRoom() {
		return room;
	}

	public void setRoom(Room room) {
		this.room = room;
	}

	public String getMyId() {
		return myId;
	}

	public void setMyId(String myId) {
		this.myId = myId;		
	}

	public String getRemoteId() {
		return remoteId;
	}

	public void setRemoteId(String remoteId) {
		this.remoteId = remoteId;
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

	public boolean isWaitRoomDismissedFromCode() {
		return waitRoomDismissedFromCode;
	}

	public void setWaitRoomDismissedFromCode(boolean waitRoomDismissedFromCode) {
		this.waitRoomDismissedFromCode = waitRoomDismissedFromCode;
	}
	
	public String getRemoteDisplayName() {
		if (this.room != null) {
			if (this.remoteId != null) {
				for (Participant p : this.room.getParticipants()) {
					if (p.getParticipantId().equals(this.remoteId)) {
						return p.getPlayer().getDisplayName();
					}
				}
			}
		}

		return null;
	}	
	
	public boolean isStarted() {
		return started;
	}

	public void setStarted(boolean started) {
		this.started = started;
	}

	public GameVariant getGameVariant() {
		return gameVariant;
	}

	public void setGameVariant(GameVariant gameVariant) {
		if( gameVariant != null ){
			this.setGameVariant(gameVariant, true);
		}
		else{
			this.gameVariant = null;
		}
	}

	public void setGameVariant(GameVariant gameVariant, boolean isOwner) {
		this.gameVariant = gameVariant;
		
		if( gameVariant.isWhite() ){
			if( isOwner ){
				this.whitePlayerId = this.myId;
				this.blackPlayerId = this.remoteId;
			}
			else{
				this.whitePlayerId = this.remoteId;
				this.blackPlayerId = this.myId;
			}
		}
		else{
			if( isOwner ){
				this.blackPlayerId = this.myId;
				this.whitePlayerId = this.remoteId;				
			}
			else{
				this.blackPlayerId = this.remoteId;
				this.whitePlayerId = this.myId;
			}
		}
	}	
	
	public String getDisplayName(String participantId) {
		if (this.room != null) {

			for (Participant p : this.room.getParticipants()) {
				if (p.getParticipantId().equals(participantId)) {
					return p.getPlayer().getDisplayName();
				}
			}
		}

		return null;
	}
	
	public String getNextWhitePlayer() {
		if (this.lastWhitePlayerId == null) {
			return myId;
		} else {
			if (this.lastWhitePlayerId.equals(myId)) {
				this.lastWhitePlayerId = remoteId;
				return remoteId;
			} else {
				this.lastWhitePlayerId = myId;
				return myId;
			}
		}
	}

	public void setRemoteRating(double remoteRating, double remoteRD,double volatility) {
		this.remoteRating = new Rating(remoteId, Util.ratingSystem);
		this.remoteRating.setRating(remoteRating);
		this.remoteRating.setRatingDeviation(remoteRD);
		this.remoteRating.setVolatility(volatility);
	}
	
	public Rating getOwnerRating() {
		Rating rating = new Rating(myId, Util.ratingSystem);
		rating.setRating(owner.getRating());
		rating.setRatingDeviation(owner.getRatingDeviation());
		rating.setVolatility(owner.getVolatility());
		
		return rating;
	}
	
	public Rating getRemoteRating() {
		return remoteRating;
	}

	public String getWhitePlayerId() {
		return whitePlayerId;
	}

	public void setWhitePlayerId(String whitePlayerId) {
		this.whitePlayerId = whitePlayerId;
	}

	public String getBlackPlayerId() {
		return blackPlayerId;
	}

	public void setBlackPlayerId(String blackPlayerId) {
		this.blackPlayerId = blackPlayerId;
	}

	public boolean isReady() {
		return ready;
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

	public void setRemoteRating(Rating remoteRating) {
		this.remoteRating = remoteRating;
	}	
}
