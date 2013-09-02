package com.chessyoup.game;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.chessyoup.model.Move;
import com.chessyoup.ui.ChessTableActivity;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;

public class ChessGameClient implements RealTimeMessageReceivedListener , RoomUpdateListener , RoomStatusUpdateListener  {
	
	private final static String TAG = "ChessGameClient";
	
	private final static int RC_WAITING_ROOM = 10002;
	
	private static ChessGameClient instance;
	
	private GamesClient gameClient;
	
	private String remotePlayerId;
	
	private Context context;
	
	private Room room;
	
	private ChessGameClientListener chessGameClientListener;
	
	public ChessGameClient(GamesClient gameClient,Context context){
		this.context = context;
		this.gameClient = gameClient;		
	}
		
	public synchronized static void init(GamesClient gameClient,Context context){
		if( ChessGameClient.instance == null ){
			ChessGameClient.instance = new ChessGameClient(gameClient,context);
		}
	}
	
	public static ChessGameClient getChessClient(){
		if( ChessGameClient.instance  == null ){
			throw new RuntimeException("Client not initialized!");
		}
		
		return ChessGameClient.instance ;
	}
		
	public void sendMove( Move move , int thinkingTime ,String roomId , String participantId ){
		
	}
		
	// *********************************************************************
	// RealTimeMessageReceivedListener methods
	// *********************************************************************
	@Override
	public void onRealTimeMessageReceived(RealTimeMessage message) {
		Log.d(TAG, "onRealTimeMessageReceived :: "+message);
	}
		
	// *********************************************************************
	// RoomUpdateListener methods
	// *********************************************************************
	@Override
	public void onRoomCreated(int statusCode, Room room) {
		Log.d(TAG, "onRoomCreated :: status code :"+statusCode+" , "+room);
		
		if (statusCode != GamesClient.STATUS_OK) {
			Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);			
			return;
		}
		
		this.room = room;
						
		Intent chessTableIntent = new Intent(this.context, ChessTableActivity.class);
		chessTableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);				
		this.context.startActivity(chessTableIntent);					
	}		
	
	@Override
	public void onJoinedRoom(int statusCode, Room room) {
		Log.d(TAG, "onJoinedRoom :: status code :"+statusCode+" , "+room);		
	}

	@Override
	public void onLeftRoom(int statusCode, String roomId) {
		Log.d(TAG, "onLeftRoom :: status code :"+statusCode+" , roomId :"+roomId);			
	}

	@Override
	public void onRoomConnected(int statusCode, Room room) {
		Log.d(TAG, "onRoomConnected :: status code :"+statusCode+" ,"+room);		
	}
			
	
	// *********************************************************************
	// RoomStatusUpdateListener methods
	// *********************************************************************
	
	@Override
	public void onConnectedToRoom(Room room) {
		Log.d(TAG, "onConnectedToRoom :: "+room);	
		
	}

	@Override
	public void onDisconnectedFromRoom(Room room) {
		Log.d(TAG, "onDisconnectedFromRoom :: "+room);		
	}

	@Override
	public void onP2PConnected(String participantId) {
		Log.d(TAG, "onP2PConnected :: "+participantId);		
	}

	@Override
	public void onP2PDisconnected(String participantId) {
		Log.d(TAG, "onP2PDisconnected :: "+participantId);
	}

	@Override
	public void onPeerDeclined(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeerDeclined :: "+arg0+" , "+arg1);
		
	}

	@Override
	public void onPeerInvitedToRoom(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeerInvitedToRoom :: "+arg0+" , "+arg1);		
	}

	@Override
	public void onPeerJoined(Room arg0, List<String> arg1) {		
		Log.d(TAG, "onPeerJoined :: "+arg0+" , "+arg1);
	}

	@Override
	public void onPeerLeft(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeerLeft :: "+arg0+" , "+arg1);		
	}

	@Override
	public void onPeersConnected(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeersConnected :: "+arg0+" , "+arg1);		
	}

	@Override
	public void onPeersDisconnected(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeersDisconnected :: "+arg0+" , "+arg1);		
	}

	@Override
	public void onRoomAutoMatching(Room room) {
		Log.d(TAG, "onRoomAutoMatching :: "+room);		
	}

	@Override
	public void onRoomConnecting(Room room) {
		Log.d(TAG, "onRoomConnecting :: "+room);	
	}

	public void waitingForRemotePlayer(Activity context) {						
		Intent i =  gameClient.getRealTimeWaitingRoomIntent(room,2);
		context.startActivityForResult(i, RC_WAITING_ROOM);
	}

	public void invitePlayer(ArrayList<String> invitees) {
		 Log.d(TAG, "Creating room...");		 		 		 
		 this.remotePlayerId = invitees.get(0);
		 RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(ChessGameClient.getChessClient());
		 rtmConfigBuilder.addPlayersToInvite(invitees);
		 rtmConfigBuilder.setMessageReceivedListener(ChessGameClient.getChessClient());
		 rtmConfigBuilder.setRoomStatusUpdateListener(ChessGameClient.getChessClient());		 		 
		 gameClient.createRoom(rtmConfigBuilder.build());
		 Log.d(TAG, "Room created, waiting for it to be ready...");
	}

	public ChessGameClientListener getChessGameClientListener() {
		return chessGameClientListener;
	}

	public void setChessGameClientListener(
			ChessGameClientListener chessGameClientListener) {
		this.chessGameClientListener = chessGameClientListener;
	}		
}
