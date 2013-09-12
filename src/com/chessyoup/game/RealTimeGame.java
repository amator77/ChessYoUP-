package com.chessyoup.game;

import android.util.Log;

import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeReliableMessageSentListener;


public abstract class RealTimeGame implements RealTimeMessageReceivedListener, RealTimeReliableMessageSentListener {
	
	private static final String TAG = "RealTimeGame";
	
	protected GamesClient client;
	
	protected GameState gameState;
				
	public RealTimeGame(GamesClient client,GameState gameState){
		this.client = client;
		this.gameState = gameState;		
	}
	
	public void sendMessage(byte[] messageData){
		Log.d(TAG, "sendMessage :: "+parseMessage(messageData));
		this.client.sendReliableRealTimeMessage(this, messageData, gameState.getRoom().getRoomId(), gameState.getRemoteId());
	}

	public GameState getGameState() {
		return gameState;
	}

	public void setGameState(GameState gameState) {
		this.gameState = gameState;
	}

	@Override
	public void onRealTimeMessageSent(int statusCode, int tokenId,
			String recipientParticipantId) {
		
		Log.d(TAG, "onRealTimeMessageSent :: statusCode:"+statusCode+" ,tokenId:"+tokenId+" ,recipientParticipantId:"+recipientParticipantId);
		
		switch (statusCode) {
			
		case GamesClient.STATUS_OK :			
			break;
		case GamesClient.STATUS_REAL_TIME_MESSAGE_SEND_FAILED :			
			break;
		case GamesClient.STATUS_REAL_TIME_ROOM_NOT_JOINED :			
			break;
		default:
			break;
		}		
	}

	@Override
	public void onRealTimeMessageReceived(RealTimeMessage message) {		
		this.handleMessageReceived(message.getSenderParticipantId() ,  message.getMessageData());		
	}	
	
	protected abstract void handleMessageReceived( String senderId ,  byte[] messageData);
	
	protected abstract String parseMessage( byte[] messageData);
}
