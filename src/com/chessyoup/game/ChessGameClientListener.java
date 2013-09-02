package com.chessyoup.game;

import com.chessyoup.model.Move;

public interface ChessGameClientListener {
	
	public void onMoveReceived(Move m , int thinkingTime);
		
}
