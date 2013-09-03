package com.chessyoup.game;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.chessyoup.R;
import com.chessyoup.model.Move;
import com.chessyoup.ui.ChessTableActivity;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeReliableMessageSentListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;

public class ChessGameClient implements RealTimeMessageReceivedListener,
		RoomUpdateListener, RoomStatusUpdateListener,
		RealTimeReliableMessageSentListener {

	private final static String TAG = "ChessGameClient";

	private final static int RC_WAITING_ROOM = 10002;

	private static ChessGameClient instance;

	private List<ChessGameClientListener> listeners;
	
	private List<Invitation> invitations;
	
	private GamesClient gameClient;

	private Context context;

	private Room room;

	private ChessGameClientListener chessGameClientListener;
	
	private InviteCallback inviteCallback;
	
    public interface InviteCallback {        
        void onCreateRoomFailed();
        
        void onCreateRoomSucceeded(Room room);
    }
	
	public ChessGameClient(GamesClient gameClient, Context context) {
		this.context = context;
		this.gameClient = gameClient;
		this.listeners = new ArrayList<ChessGameClientListener>();
		this.invitations = new ArrayList<Invitation>();
	}

	public synchronized static void init(GamesClient gameClient, Context context) {
		if (ChessGameClient.instance == null) {
			ChessGameClient.instance = new ChessGameClient(gameClient, context);
		}
	}

	public static ChessGameClient getChessClient() {
		if (ChessGameClient.instance == null) {
			throw new RuntimeException("Client not initialized!");
		}

		return ChessGameClient.instance;
	}
	
	public void acceptInvitation(Invitation inv) {		
		RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this);
		roomConfigBuilder.setInvitationIdToAccept(inv.getInvitationId())
				.setMessageReceivedListener(this)
				.setRoomStatusUpdateListener(this);

		gameClient.joinRoom(roomConfigBuilder.build());
	}
	
	public void sendMove(Move move, int thinkingTime) {
		Log.d(TAG, "sendMove ::" + move.toString() + " , thinkingTime"
				+ thinkingTime);

		String remoteId = getRemoteParticipant();

		if (remoteId != null) {

			JSONObject object = new JSONObject();

			try {
				object.put("move", move.toString());
				object.put("time", thinkingTime);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			this.gameClient.sendReliableRealTimeMessage(this, object.toString()
					.getBytes(), this.room.getRoomId(), remoteId);
		}
	}

	private String getRemoteParticipant() {

		if (this.room != null) {

			for (Participant participant : this.room.getParticipants()) {
				String pId = this.room.getParticipantId(participant
						.getParticipantId());

				if (pId != null) {

					if (pId.equals(this.room.getCreatorId())) {
						continue;
					} else {
						return pId;
					}
				} else {
					continue;
				}
			}
		}

		return null;
	}

	public void registerListener(ChessGameClientListener listener) {
		if (!this.listeners.contains(listener)) {
			this.listeners.add(listener);
		}
	}

	public void removeListener(ChessGameClientListener listener) {
		if (this.listeners.contains(listener)) {
			this.listeners.remove(listener);
		}
	}

	public Room getRoom(String roomId) {
		return this.room;
	}
	
	// *********************************************************************
	// RealTimeReliableMessageSentListener methods
	// *********************************************************************
	@Override
	public void onRealTimeMessageSent(int statusCode, int tokenId,
			String recipientParticipantId) {

		Log.d(TAG, "onRealTimeMessageSent :: statusCode : " + statusCode
				+ " , tokenId : " + tokenId + " , recipientParticipantId : "
				+ recipientParticipantId);

	}
	
	// *********************************************************************
	// RealTimeMessageReceivedListener methods
	// *********************************************************************
	@Override
	public void onRealTimeMessageReceived(RealTimeMessage message) {
		Log.d(TAG,"onRealTimeMessageReceived :"+message.toString()+" , "+message.getSenderParticipantId()+" , "+new String(message.getMessageData()));
		
		byte[] buf = message.getMessageData();
		String sender = message.getSenderParticipantId();
		String move = null;
								
		try {
			JSONObject json = new JSONObject(new String(message.getMessageData()));
			move = json.getString("move");								
		} catch (JSONException e) {			
			e.printStackTrace();
			return;
		}
		
		Log.d(TAG, "Move received: " + move);
		
		for(ChessGameClientListener listener : listeners){
			listener.onMoveReceived(move, 0);
		}				
	}

	// *********************************************************************
	// RoomUpdateListener methods
	// *********************************************************************
	@Override
	public void onRoomCreated(int statusCode, Room room) {
		Log.d(TAG, "onRoomCreated :: status code :" + statusCode + " , " + room);

		if (statusCode != GamesClient.STATUS_OK) {
			Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);
			if( this.inviteCallback != null ){
				this.inviteCallback.onCreateRoomFailed();
			}
			return;
		}

		this.room = room;

//		Intent chessTableIntent = new Intent(this.context,
//				ChessTableActivity.class);
//		chessTableIntent.putExtra("owner", true);
//		chessTableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		this.context.startActivity(chessTableIntent);
		
		if( this.inviteCallback != null ){
			this.inviteCallback.onCreateRoomSucceeded(room);
		}
	}

	@Override
	public void onJoinedRoom(int statusCode, Room room) {
		Log.d(TAG, "onJoinedRoom :: status code :" + statusCode + " , " + room);
		this.room = room;
		Intent chessTableIntent = new Intent(this.context,
				ChessTableActivity.class);
		chessTableIntent.putExtra("owner", false);
		chessTableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		this.context.startActivity(chessTableIntent);
	}

	@Override
	public void onLeftRoom(int statusCode, String roomId) {
		Log.d(TAG, "onLeftRoom :: status code :" + statusCode + " , roomId :"
				+ roomId);
	}

	@Override
	public void onRoomConnected(int statusCode, Room room) {
		Log.d(TAG, "onRoomConnected :: status code :" + statusCode + " ,"
				+ room);
	}

	// *********************************************************************
	// RoomStatusUpdateListener methods
	// *********************************************************************

	@Override
	public void onConnectedToRoom(Room room) {
		Log.d(TAG, "onConnectedToRoom :: " + room);

	}

	@Override
	public void onDisconnectedFromRoom(Room room) {
		Log.d(TAG, "onDisconnectedFromRoom :: " + room);
	}

	@Override
	public void onP2PConnected(String participantId) {
		Log.d(TAG, "onP2PConnected :: " + participantId);
	}

	@Override
	public void onP2PDisconnected(String participantId) {
		Log.d(TAG, "onP2PDisconnected :: " + participantId);
	}

	@Override
	public void onPeerDeclined(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeerDeclined :: " + arg0 + " , " + arg1);

	}

	@Override
	public void onPeerInvitedToRoom(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeerInvitedToRoom :: " + arg0 + " , " + arg1);
	}

	@Override
	public void onPeerJoined(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeerJoined :: " + arg0 + " , " + arg1);
	}

	@Override
	public void onPeerLeft(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeerLeft :: " + arg0 + " , " + arg1);
	}

	@Override
	public void onPeersConnected(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeersConnected :: " + arg0 + " , " + arg1);
	}

	@Override
	public void onPeersDisconnected(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeersDisconnected :: " + arg0 + " , " + arg1);
	}

	@Override
	public void onRoomAutoMatching(Room room) {
		Log.d(TAG, "onRoomAutoMatching :: " + room);
	}

	@Override
	public void onRoomConnecting(Room room) {
		Log.d(TAG, "onRoomConnecting :: " + room);
	}

	public void invitePlayer(ArrayList<String> invitees , InviteCallback inviteCallback) {
		Log.d(TAG, "Creating room...for :"+invitees.toString());		
		RoomConfig.Builder rtmConfigBuilder = RoomConfig
				.builder(ChessGameClient.getChessClient());
		rtmConfigBuilder.addPlayersToInvite(invitees);
		rtmConfigBuilder.setMessageReceivedListener(ChessGameClient
				.getChessClient());
		rtmConfigBuilder.setRoomStatusUpdateListener(ChessGameClient
				.getChessClient());
		this.inviteCallback = inviteCallback;
		gameClient.createRoom(rtmConfigBuilder.build());		
		Log.d(TAG, "Room created, waiting for it to be ready...");
	}
	 
	public void leaveRoom(String roomId) {
		Log.d(TAG, "Leaving room.");			
		gameClient.leaveRoom(this, roomId);				
	}
	
	public ChessGameClientListener getChessGameClientListener() {
		return chessGameClientListener;
	}

	public void setChessGameClientListener(
			ChessGameClientListener chessGameClientListener) {
		this.chessGameClientListener = chessGameClientListener;
	}
}
