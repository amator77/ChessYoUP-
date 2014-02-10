package com.chessyoup.ui.ctrl;

import java.util.List;

import android.util.Log;

import com.chessyoup.R;
import com.chessyoup.game.GameController;
import com.chessyoup.game.GameModel;
import com.chessyoup.game.GamePlayer;
import com.chessyoup.game.Util;
import com.chessyoup.ui.ChessGameRoomUI;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;

public class ChessGameRoomController implements RoomUpdateListener,
		RoomStatusUpdateListener {

	private final static String TAG = "ChessGameRoomController";

	private ChessGameRoomUI chessGameRoomUI;

	public ChessGameRoomController(ChessGameRoomUI chessGameRoomUI) {
		this.chessGameRoomUI = chessGameRoomUI;		
	}

	@Override
	public void onJoinedRoom(int statusCode, Room room) {
		Log.d(TAG, "onJoinedRoom :: statusCode="+statusCode+",");
		printRoom(room);
	}

	@Override
	public void onLeftRoom(int statusCode, String roomId) {
		Log.d(TAG, "onJoinedRoom :: statusCode="+statusCode+", roomdId="+roomId);
	}

	@Override
	public void onRoomConnected(int statusCode, Room room) {		
		Log.d(TAG, "onRoomConnected :: statusCode="+statusCode+",");
		printRoom(room);
				
		if (statusCode != GamesClient.STATUS_OK) {
			Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
			GameController.getInstance().showGameError(chessGameRoomUI.getString(R.string.error), chessGameRoomUI.getString(R.string.game_problem));			
			return;
		}
		
		GameController.getInstance().getRealTimeChessGame().setRoom(room);
		GameController.getInstance().getRealTimeChessGame().setListener(this.chessGameRoomUI.getRealTimeChessGameController());
		GameModel gameModel = chessGameRoomUI.getGameModel();
		gameModel.setRoom(room);		
		
		for (Participant p : room.getParticipants()) {
			if(p.getParticipantId().equals(room.getCreatorId())){
				GameController.getInstance().getLocalPlayer().setParticipant(p);
			}
		}
		
		for (Participant p : room.getParticipants()) {
									
			if (!p.getParticipantId().equals(room.getCreatorId())) {
				GamePlayer remotePlayer = new GamePlayer();
				remotePlayer.setParticipant(p);
				gameModel.setRemotePlayer(remotePlayer);
				break;
			}
		}
		
		if( gameModel.getRemotePlayer() != null ){
			gameModel.setGameVariant(Util.getGameVariant(room.getVariant()));
			GameController.getInstance().getRealTimeChessGame().ready();			
		}
	}

	@Override
	public void onRoomCreated(int statusCode, Room room) {
		Log.d(TAG, "onRoomCreated :: statusCode="+statusCode+",");
		printRoom(room);
						
		if (statusCode != GamesClient.STATUS_OK) {
			Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);
			GameController.getInstance().showGameError(chessGameRoomUI.getString(R.string.error), chessGameRoomUI.getString(R.string.game_problem));			
			return;
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
	public void onPeerLeft(Room arg0, List<String> arg1) {
		Log.d(TAG, "onPeerLeft :: "+arg1);
		printRoom(arg0);
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

	private void printRoom(Room room) {
		Log.d(TAG,
				" Room : id=" + room.getRoomId() + ",creator="
						+ room.getCreatorId() + ",status=" + room.getStatus()
						+ ",variant=" + room.getVariant() + ",participants="+room.getParticipantIds());
	}
}
