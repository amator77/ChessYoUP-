package com.chessyoup.game;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.chessyoup.chessboard.ChessboardController;
import com.chessyoup.chessboard.ChessboardStatus;
import com.chessyoup.chessboard.ChessboardUIInterface;
import com.chessyoup.game.view.ChessBoardPlay;
import com.chessyoup.game.view.PgnScreenTextView;
import com.chessyoup.model.Move;
import com.chessyoup.model.Position;
import com.chessyoup.model.pgn.PGNOptions;

public class ChessTable implements ChessboardUIInterface {
	
	private static final String TAG = "ChessTable";
	
	private String ownerId;
	
	private String whitePlayerId;
	
	private String blackPlayerId;
			
	private ChessBoardPlay chessboardView;
	
	private ChessboardController ctrl;
	
	private PgnScreenTextView gameTextListener;
		
	public ChessTable(Context context, ChessBoardPlay  chessboardView ,String owner){			
		this.chessboardView = chessboardView;
		this.ownerId = owner;
		this.ctrl = new ChessboardController(this, gameTextListener, new PGNOptions());
		this.installListeners();
	}
	
	public void startGame(){
		
	}
	
	public String getOwner() {
		return ownerId;
	}

	public void setOwner(String owner) {
		this.ownerId = owner;
	}

	public String getWhitePlayer() {
		return whitePlayerId;
	}

	public void setWhitePlayer(String whitePlayer) {
		this.whitePlayerId = whitePlayer;
	}

	public String getBlackPlayer() {
		return blackPlayerId;
	}

	public void setBlackPlayer(String blackPlayer) {
		this.blackPlayerId = blackPlayer;
	}

	@Override
	public void setPosition(Position pos, String variantInfo,
			ArrayList<Move> variantMoves) {
		// TODO Auto-generated method stub
		
		Log.d(TAG,"setPosition "+pos.toString());
		this.chessboardView.setPosition(pos);			
	}

	@Override
	public void setSelection(int sq) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setStatus(ChessboardStatus status) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void moveListUpdated() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void requestPromotePiece() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void runOnUIThread(Runnable runnable) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reportInvalidMove(Move m) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRemainingTime(long wTime, long bTime, long nextUpdate) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAnimMove(Position sourcePos, Move move, boolean forward) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String whitePlayerName() {		
		return this.whitePlayerId;
	}

	@Override
	public String blackPlayerName() {
		return this.blackPlayerId;
	}

	@Override
	public boolean discardVariations() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void localMoveMade(Move m) {
		// TODO Auto-generated method stub
		
	}		
	
	private void installListeners() {
		
		final GestureDetector gd = new GestureDetector(chessboardView.getContext(),
				new GestureDetector.SimpleOnGestureListener() {
					private float scrollX = 0;
					private float scrollY = 0;

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
						chessboardView.cancelLongPress();
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
							
							int sq = chessboardView.eventToSquare(e);
							Move m = chessboardView.mousePressed(sq);
							Log.d(TAG,"handleClick"+sq);
							
							if (m != null) {
								Log.d(TAG,"Move :"+m);
								if (true) {
									Log.d(TAG,"Local turn  :"+ctrl.getGame().getGameState() +" , "+ctrl.getGame().currPos().whiteMove);
									ctrl.makeLocalMove(m);									
								}
							}
						}
					}
				});
		
		this.chessboardView.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				Log.d(TAG,"onTouch");
				return gd.onTouchEvent(event);
			}
		});
	}
}
