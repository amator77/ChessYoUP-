package com.chessyoup.game;

import com.google.android.gms.games.multiplayer.realtime.Room;

public interface GameState {
	
	public GameTransport getGameTransport() ;

	public GamePlayer getLocalPlayer();

	public GamePlayer getRemotePlayer();

	public Room getRoom();

	public GameVariant getGameVariant();

	public boolean isReady();
	
    public boolean isLocalPlayerRoomCreator();
}
