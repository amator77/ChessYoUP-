package com.chessyoup.ui.ctrl;

import java.util.List;

import android.content.Intent;
import android.util.Log;

import com.chessyoup.game.GameTransport;
import com.chessyoup.game.chess.ChessGamePlayer;
import com.chessyoup.game.chess.ChessGameState;
import com.chessyoup.game.chess.ChessGameTransport;
import com.chessyoup.ui.ChessOnlinePlayGameUI;
import com.chessyoup.ui.ChessYoUpActivity;
import com.chessyoup.ui.util.UIUtil;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;

public class RoomEventsAdapter implements RoomUpdateListener,
		RoomStatusUpdateListener {

	private final static String TAG = "RoomEventsAdapter";
	
	private ChessGameTransport gameTransport;
	
	private ChessYoUpActivity mainActivity;
	
	public RoomEventsAdapter(ChessYoUpActivity mainActivity){
		this.mainActivity = mainActivity;
	}
	
	public void setGameTransport(ChessGameTransport gameTransport) {
		this.gameTransport = gameTransport;
	}

	public GameTransport getGameTransport() {
		return gameTransport;
	}

	@Override
	public void onJoinedRoom(int statusCode, Room room) {
		Log.d(TAG, "onJoinedRoom :: statusCode=" + statusCode + ",");
		printRoom(room);
	}

	@Override
	public void onLeftRoom(int statusCode, String roomId) {
		Log.d(TAG, "onLeftRoom :: statusCode=" + statusCode + ", roomdId="
				+ roomId);
		
		if( mainActivity != null){
			mainActivity.getRoomsAdapter().removeRoom(roomId);
		}
	}

	@Override
	public void onRoomConnected(int statusCode, Room room) {
		Log.d(TAG, "onRoomConnected :: statusCode=" + statusCode + ",");		
		ChessGamePlayer remotePlayer = getRemotePlayer(room);
		mainActivity.getRoomsAdapter().removeRoom(room.getRoomId());
		gameTransport.configure(room.getRoomId(), remotePlayer.getParticipant().getParticipantId());		
		ChessGameState model = ChessGameController.getController().newGame(room, gameTransport, remotePlayer);
		Log.d(TAG, model.toString());		
		Intent chessRoomUIIntent = new Intent(mainActivity, ChessOnlinePlayGameUI.class);
        chessRoomUIIntent.putExtra(ChessYoUpActivity.ROOM_ID_EXTRA, room.getRoomId());
        mainActivity.startActivity(chessRoomUIIntent);		
	}

	@Override
	public void onRoomCreated(int statusCode, Room room) {
		Log.d(TAG, "onRoomCreated :: statusCode=" + statusCode + ",");
		printRoom(room);
		

		if( room != null ){
			mainActivity.setSelectedTab(1);
			mainActivity.getRoomsAdapter().addRoom(room);		
		}
		else{
			Log.e(TAG, "onRoomCreated :: statusCode=" + statusCode + ", Room is null!");
		}
	}

	@Override
	public void onConnectedToRoom(Room room) {
		Log.d(TAG, "onConnectedToRoom");
		printRoom(room);
	}

	@Override
	public void onDisconnectedFromRoom(Room room) {
		Log.d(TAG, "onDisconnectedFromRoom");
		printRoom(room);
	}

	@Override
	public void onP2PConnected(String participantId) {
		Log.d(TAG, "onP2PConnected :: participantId=" + participantId);
	}

	@Override
	public void onP2PDisconnected(String participantId) {
		Log.d(TAG, "onP2PDisconnected :: participantId=" + participantId);
	}

	@Override
	public void onPeerDeclined(Room room, List<String> arg1) {
		Log.d(TAG, "onPeerDeclined :: " + arg1);
		printRoom(room);
		
		mainActivity.getRoomsAdapter().removeRoom(room.getRoomId());
		
		for(String pid : arg1){
		    for(Participant p : room.getParticipants()){
		        if(p.getParticipantId().equals(pid)){
		            UIUtil.displayShortMessage(mainActivity, p.getDisplayName()+" rejected your invitation!");
		            return;
		        }
		    }
		}				
	}

	@Override
	public void onPeerInvitedToRoom(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeerInvitedToRoom :: " + arg1);
		printRoom(arg0);
	}

	@Override
	public void onPeerJoined(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeerJoined :: " + arg1);
		printRoom(arg0);
	}

	@Override
	public void onPeerLeft(Room room, List<String> arg1) {
		Log.d(TAG, "onPeerLeft :: " + arg1);
		printRoom(room);
		
		this.gameTransport.remoteLeft();			
	}

	@Override
	public void onPeersConnected(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeersConnected :: " + arg1);
		printRoom(arg0);
	}

	@Override
	public void onPeersDisconnected(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeersDisconnected :: " + arg1);
		printRoom(arg0);
	}

	@Override
	public void onRoomAutoMatching(Room room) {
		Log.d(TAG, "onRoomAutoMatching");
		printRoom(room);
	}

	@Override
	public void onRoomConnecting(Room room) {
		Log.d(TAG, "onRoomConnecting");
		printRoom(room);
	}

	private void printRoom(Room room) {
		if (room != null) {
			Log.d(TAG,
					" Room : id=" + room.getRoomId() + ",creator="
							+ room.getCreatorId() + ",status="
							+ room.getStatus() + ",variant="
							+ room.getVariant() + ",participants="
							+ room.getParticipantIds());
		} else {
			Log.d(TAG, "Room :: " + null);
		}
	}
	
	private ChessGamePlayer getRemotePlayer(Room room) {
		ChessGamePlayer remotePlayer = null;
		
        for(Participant p : room.getParticipants()){
            if( !p.getPlayer().getPlayerId().equals(ChessGameController.getController().getLocalPlayer().getPlayer().getPlayerId())){
                remotePlayer = new ChessGamePlayer();
                remotePlayer.setParticipant(p);                                              
            }
            else{
            	ChessGameController.getController().getLocalPlayer().setParticipant(p);
            }
        }
        
        return remotePlayer;
    }
}
