package com.chessyoup.game;


public interface ChessGameClientListener {
	
	public void onStartGame(String whitePlayerId , String blackPlayerId , String remotePlayerId);
	
	public void onMoveReceived(String move , int thinkingTime);
		
}
