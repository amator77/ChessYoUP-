package com.chessyoup.ui.ctrl;

import android.util.Log;

import com.chessyoup.game.GameController;
import com.chessyoup.game.GameVariant;
import com.chessyoup.game.RealTimeChessGame.RealTimeChessGameListener;
import com.chessyoup.ui.ChessGameRoomUI;

public class RealTimeChessGameController implements RealTimeChessGameListener {

	private final static String TAG = "RealTimeChessGameController";
	
	private ChessGameRoomUI chessGameRoomUI;
			
	public RealTimeChessGameController(ChessGameRoomUI chessGameRoomUI){
		this.chessGameRoomUI = chessGameRoomUI;
		GameController.getInstance().getRealTimeChessGame().setListener(this);		
	}
	
	@Override
	public void onChallangeRecevied(GameVariant gameVariant) {
		Log.d(TAG, "onChallangeRecevied :: gameVariant="+gameVariant);		
	}

	@Override
	public void onStartRecevied() {
		Log.d(TAG, "onStartRecevied :: ");		
	}

	@Override
	public void onReadyRecevied(double remoteRating, double remoteRD,
			double volatility) {
		Log.d(TAG, "onReadyRecevied :: remoteRating="+remoteRating+",remoteRD="+remoteRD+",volatility"+volatility);		
		
		chessGameRoomUI.getGameModel().getRemotePlayer().setRating(remoteRating);
		chessGameRoomUI.getGameModel().getRemotePlayer().setRatingDeviation(remoteRD);
		chessGameRoomUI.getGameModel().getRemotePlayer().setVolatility(volatility);
		chessGameRoomUI.roomReady();				
	}

	@Override
	public void onMoveRecevied(String move, int thinkingTime) {
		Log.d(TAG, "onMoveRecevied :: move="+move+",thinkingTime="+thinkingTime);
		chessGameRoomUI.getChessboardController().makeRemoteMove(move);
	}

	@Override
	public void onResignRecevied() {
		Log.d(TAG, "onResignRecevied ::");		
	}

	@Override
	public void onDrawRecevied() {
		Log.d(TAG, "onDrawRecevied ::");		
	}

	@Override
	public void onFlagRecevied() {
		Log.d(TAG, "onFlagRecevied ::");		
	}

	@Override
	public void onRematchRecevied() {
		Log.d(TAG, "onRematchRecevied ::");
	}

	@Override
	public void onAbortRecevied() {
		Log.d(TAG, "onAbortRecevied ::");		
	}

	@Override
	public void onException(String message) {
		Log.d(TAG, "onException :: message"+message);		
	}

	@Override
	public void onChatReceived(String message) {
		Log.d(TAG, "onChatReceived :: message"+message);		
	}

	public ChessGameRoomUI getChessGameRoomUI() {
		return chessGameRoomUI;
	}

	public void setChessGameRoomUI(ChessGameRoomUI chessGameRoomUI) {
		this.chessGameRoomUI = chessGameRoomUI;
	}
}
