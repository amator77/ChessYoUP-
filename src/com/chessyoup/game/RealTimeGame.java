package com.chessyoup.game;

import java.util.ArrayList;
import java.util.List;


public class RealTimeGame {
	
	private String localPlayerId;
	
	private String remotePlayerId;
	
	private List<GameMessage> history;
	
	private GameConnector connector;
		
	
	public RealTimeGame(){
		this.history = new ArrayList<GameMessage>();
	}
	
	public void sendMessage(GameMessage message){
		
	}

	public String getLocalPlayerId() {
		return localPlayerId;
	}

	public void setLocalPlayerId(String localPlayerId) {
		this.localPlayerId = localPlayerId;
	}

	public String getRemotePlayerId() {
		return remotePlayerId;
	}

	public void setRemotePlayerId(String remotePlayerId) {
		this.remotePlayerId = remotePlayerId;
	}

	public GameConnector getConnector() {
		return connector;
	}

	public void setConnector(GameConnector connector) {
		this.connector = connector;
	}	
}
