package com.chessyoup.game;

import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;

public class RealTimeChessGame extends RealTimeGame implements RealTimeMessageReceivedListener {
	
	private static final byte READY = 0;
	private static final byte START = 1;
	private static final byte MOVE = 2;
	private static final byte DRAW = 3;
	private static final byte RESIGN = 4;
	private static final byte FLAG = 5;
	private static final byte REMATCH = 6;
	
	private RealTimeChessGameListener listener;
	
	public interface RealTimeChessGameListener{
		
		public void onReadyRecevied();
		
		public void onStartRecevied();
		
		public void onMoveRecevied();
		
		public void onDrawRecevied();
		
		public void onFlagRecevied();
		
		public void onRematchRecevied();
	}
	
	public RealTimeChessGame(GameConnector connector){
		
	}
	
	
	public RealTimeChessGameListener getListener() {
		return listener;
	}

	public void setListener(RealTimeChessGameListener listener) {
		this.listener = listener;
	}

	@Override
	public void onRealTimeMessageReceived(RealTimeMessage message) {
		// TODO Auto-generated method stub
		
	}			
}