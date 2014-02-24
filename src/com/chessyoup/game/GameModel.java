package com.chessyoup.game;

import com.google.android.gms.games.multiplayer.realtime.Room;

public class GameModel {
	
	private GamePlayer remotePlayer;
			
	private Room room = null;
			
	private GameVariant gameVariant;			
	
	public boolean localPlayerOwner;
	
	private boolean ready;
	
	public GameModel(){				
	}
	
	public void reset(){								
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

	public GameVariant getGameVariant() {
		return gameVariant;
	}

	public void setGameVariant(GameVariant gameVariant) {		
		this.gameVariant = gameVariant;
	}

	public boolean isReady() {
		return ready;
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

    public boolean isLocalPlayerOwner() {
        return localPlayerOwner;
    }

    public void setLocalPlayerOwner(boolean localPlayerOwner) {
        this.localPlayerOwner = localPlayerOwner;
    }
}
