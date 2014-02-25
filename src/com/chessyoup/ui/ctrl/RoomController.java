package com.chessyoup.ui.ctrl;

import java.util.List;

import android.util.Log;

import com.chessyoup.R;
import com.chessyoup.game.chess.ChessGameController;
import com.chessyoup.ui.ChessYoUpActivity;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;

public class RoomController implements RoomUpdateListener,
		RoomStatusUpdateListener , OnInvitationReceivedListener {

	private final static String TAG = "RoomController";

	private ChessYoUpActivity chessYoUpActivity;

	public RoomController(ChessYoUpActivity chessYoUpActivity) {
		this.chessYoUpActivity = chessYoUpActivity;		
	}

	@Override
	public void onJoinedRoom(int statusCode, Room room) {
		Log.d(TAG, "onJoinedRoom :: statusCode="+statusCode+",");
		printRoom(room);
	}

	@Override
	public void onLeftRoom(int statusCode, String roomId) {
		Log.d(TAG, "onLeftRoom :: statusCode="+statusCode+", roomdId="+roomId);
		chessYoUpActivity.getRoomsAdapter().removeRoom(roomId);
	}

	@Override
	public void onRoomConnected(int statusCode, Room room) {		
		Log.d(TAG, "onRoomConnected :: statusCode="+statusCode+",");
		printRoom(room);
				
		if (statusCode != GamesClient.STATUS_OK) {
			Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
			ChessGameController.getController().showGameError(chessYoUpActivity.getString(R.string.error), chessYoUpActivity.getString(R.string.game_problem));			
			return;
		}
		
		//TODO start game activity		
	}

	@Override
	public void onRoomCreated(int statusCode, Room room) {
		Log.d(TAG, "onRoomCreated :: statusCode="+statusCode+",");
		printRoom(room);
						
		if (statusCode != GamesClient.STATUS_OK) {
			Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);
			ChessGameController.getController().showGameError(chessYoUpActivity.getString(R.string.error), chessYoUpActivity.getString(R.string.game_problem));			
			return;
		}		
		
		chessYoUpActivity.setSelectedTab(1);
		chessYoUpActivity.getRoomsAdapter().addRoom(room);		
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
		Log.d(TAG, "onP2PConnected :: participantId="+participantId);
	}

	@Override
	public void onP2PDisconnected(String participantId) {
		Log.d(TAG, "onP2PDisconnected :: participantId="+participantId);
	}

	@Override
	public void onPeerDeclined(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeerDeclined :: "+arg1);
		printRoom(arg0);
	}

	@Override
	public void onPeerInvitedToRoom(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeerInvitedToRoom :: "+arg1);
		printRoom(arg0);
	}

	@Override
	public void onPeerJoined(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeerJoined :: "+arg1);
		printRoom(arg0);
	}

	@Override
	public void onPeerLeft(Room room, List<String> arg1) {
		Log.d(TAG, "onPeerLeft :: "+arg1);
		printRoom(room);
				 
		ChessGameController.getController().getRealTimeGameClient().remoteLeft();						
	}

	@Override
	public void onPeersConnected(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeersConnected :: "+arg1);
		printRoom(arg0);
	}

	@Override
	public void onPeersDisconnected(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeersDisconnected :: "+arg1);
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
	
    @Override
    public void onInvitationReceived(Invitation invitation) {
        chessYoUpActivity.setSelectedTab(0);
        chessYoUpActivity.getInvitationsAdapter().addInvitation(invitation);      
    }

    @Override
    public void onInvitationRemoved(String invitationId) {
        chessYoUpActivity.getInvitationsAdapter().removeChallenge(invitationId);
    }
    
    private void printRoom(Room room) {
        if( room != null){
        Log.d(TAG,
                " Room : id=" + room.getRoomId() + ",creator="
                        + room.getCreatorId() + ",status=" + room.getStatus()
                        + ",variant=" + room.getVariant() + ",participants="+room.getParticipantIds());
        }
        else{
            Log.d(TAG,"Room :: "+null);
        }
    }
}
