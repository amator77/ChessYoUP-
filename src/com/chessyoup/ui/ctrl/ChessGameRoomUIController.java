package com.chessyoup.ui.ctrl;

import java.util.ArrayList;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.chessyoup.chessboard.ChessboardStatus;
import com.chessyoup.chessboard.ChessboardUIInterface;
import com.chessyoup.model.Move;
import com.chessyoup.model.Position;
import com.chessyoup.ui.ChessGameRoomUI;

public class ChessGameRoomUIController extends GestureDetector.SimpleOnGestureListener implements ChessboardUIInterface  {
	
	private final static String TAG = "ChessGameRoomUIController";
	
	private ChessGameRoomUI chessGameRoomUI;
	
	
	@Override
	public boolean onDown(MotionEvent e) {
		handleClick(e);
		return true;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2,
			float distanceX, float distanceY) {

		return true;
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		chessGameRoomUI.getBoardPlayView().cancelLongPress();		
		handleClick(e);
		return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		if (e.getAction() == MotionEvent.ACTION_UP)
			handleClick(e);
		return true;
	}

	private final void handleClick(MotionEvent e) {
		if (true) {

			int sq = chessGameRoomUI.getBoardPlayView().eventToSquare(e);
			Move m = chessGameRoomUI.getBoardPlayView().mousePressed(sq);
			Log.d(TAG, "handleClick :: " + sq);

			if (m != null) {
				Log.d(TAG, "Move :" + m);
				chessGameRoomUI.getChessboardController().makeLocalMove(m);					
				// TODO send move to remote player
				
			}
		}
	}
	
	public ChessGameRoomUIController(ChessGameRoomUI chessGameRoomUI){
		this.chessGameRoomUI = chessGameRoomUI;
		
		
	}
	
	@Override
	public void setPosition(Position pos, String variantInfo,
			ArrayList<Move> variantMoves) {
		Log.d(TAG, "setPosition :: pos="+pos+",variantInfo="+variantInfo+",variantMoves"+variantMoves);
		chessGameRoomUI.getBoardPlayView().setPosition(pos);
	}

	@Override
	public void setSelection(int sq) {
		Log.d(TAG, "setSelection :: square="+sq);
		
	}

	@Override
	public void setStatus(ChessboardStatus status) {
		Log.d(TAG, "setStatus :: status="+status);
		
	}

	@Override
	public void moveListUpdated() {
		Log.d(TAG, "moveListUpdated :: ");
		
	}

	@Override
	public void requestPromotePiece() {
		Log.d(TAG, "requestPromotePiece :: ");		
	}

	@Override
	public void runOnUIThread(Runnable runnable) {
		Log.d(TAG, "runOnUIThread :: ");		
	}

	@Override
	public void reportInvalidMove(Move m) {
		Log.d(TAG, "reportInvalidMove :: move="+m.toString());	
		
	}

	@Override
	public void setRemainingTime(int wTime, int bTime, int nextUpdate) {
		Log.d(TAG, "setRemainingTime :: wTime="+wTime+",bTime"+bTime+",nextUpdate="+nextUpdate);

		
	}

	@Override
	public void setAnimMove(Position sourcePos, Move move, boolean forward) {
		Log.d(TAG, "setAnimMove :: sourcePos="+sourcePos+",move="+move+",forward="+forward);		
	}

	@Override
	public String whitePlayerName() {
		return chessGameRoomUI.getWitePlayerName();
	}

	@Override
	public String blackPlayerName() {
		return chessGameRoomUI.getBlackPlayerName();
	}

	@Override
	public boolean discardVariations() {
		Log.d(TAG, "discardVariations :: ");
		return false;
	}

	@Override
	public void localMoveMade(Move m) {
		Log.d(TAG, "localMoveMade :: m="+m);			
	}
}
