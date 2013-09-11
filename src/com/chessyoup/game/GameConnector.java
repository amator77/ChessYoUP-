package com.chessyoup.game;

public class GameConnector {
	
	private GameConnectorListener listener;
	
	public interface GameConnectorListener{
		public void onMessageReceived(byte[] data);
	}
	
		
	public void sendMessage(byte[] data){
		
	}


	public GameConnectorListener getListener() {
		return listener;
	}


	public void setListener(GameConnectorListener listener) {
		this.listener = listener;
	}		
}
