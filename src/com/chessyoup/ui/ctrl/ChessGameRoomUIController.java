package com.chessyoup.ui.ctrl;

import java.util.ArrayList;

import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.chessyoup.chessboard.ChessboardStatus;
import com.chessyoup.chessboard.ChessboardUIInterface;
import com.chessyoup.game.GameController;
import com.chessyoup.model.Move;
import com.chessyoup.model.Position;
import com.chessyoup.model.TextIO;
import com.chessyoup.ui.ChessGameRoomUI;

public class ChessGameRoomUIController extends GestureDetector.SimpleOnGestureListener implements ChessboardUIInterface,Runnable  {
	
	private final static String TAG = "ChessGameRoomUIController";
	
	private ChessGameRoomUI chessGameRoomUI;
	
	private Handler handlerTimer;
	

	public ChessGameRoomUIController(ChessGameRoomUI chessGameRoomUI){
		this.chessGameRoomUI = chessGameRoomUI;
		this.handlerTimer  = new Handler();
	}
	
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
				GameController.getInstance().getRealTimeChessGame().move(TextIO
						.moveToUCIString(m), 0);				
			}
		}
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

    	
    	if( chessGameRoomUI.getChessboardController().localTurn() ){
    		if( wTime <= 0 && chessGameRoomUI.getChessboardController().getGame().currPos().whiteMove ){
    			chessGameRoomUI.getChessboardController().resignGame();
    			GameController.getInstance().getRealTimeChessGame().flag();    			    			
    		}
    		
    		if( bTime <= 0 && !chessGameRoomUI.getChessboardController().getGame().currPos().whiteMove ){
    			chessGameRoomUI.getChessboardController().resignGame();    		
    			GameController.getInstance().getRealTimeChessGame().flag();
    		}
    	}
    	
        if (chessGameRoomUI.getChessboardController().getGameMode().clocksActive()) {
        	chessGameRoomUI.updateClocks(timeToString(wTime),timeToString(bTime));        	        	     	        	           
        } 
        
        handlerTimer.removeCallbacks(this);
        if (nextUpdate > 0)
            handlerTimer.postDelayed(this, nextUpdate);
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
	
	private final String timeToString(int time) {
        int secs = (int)Math.floor((time + 999) / 1000.0);
        boolean neg = false;
        if (secs < 0) {
            neg = true;
            secs = -secs;
        }
        int mins = secs / 60;
        secs -= mins * 60;
        StringBuilder ret = new StringBuilder();
        if (neg) ret.append('-');
        ret.append(mins);
        ret.append(':');
        if (secs < 10) ret.append('0');
        ret.append(secs);
        return ret.toString();
    }

	@Override
	public void run() {		
		this.chessGameRoomUI.getChessboardController().updateRemainingTime();
	}
}
