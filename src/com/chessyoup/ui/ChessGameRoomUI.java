package com.chessyoup.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;

import com.chessyoup.R;
import com.chessyoup.chessboard.ChessboardController;
import com.chessyoup.chessboard.ChessboardMode;
import com.chessyoup.game.GameController;
import com.chessyoup.game.GameModel;
import com.chessyoup.game.Util;
import com.chessyoup.game.view.ChessBoardPlayView;
import com.chessyoup.game.view.PgnScreenTextView;
import com.chessyoup.model.pgn.PGNOptions;
import com.chessyoup.ui.ctrl.ChessGameRoomController;
import com.chessyoup.ui.ctrl.ChessGameRoomUIController;
import com.chessyoup.ui.ctrl.RealTimeChessGameController;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;

public class ChessGameRoomUI extends FragmentActivity{
	
	private final static String TAG = "ChessGameRoomUI";
	
	private GameModel chessGameModel;
	
	private RealTimeChessGameController realTimechessGameController;
	
	private ChessGameRoomUIController  gameRoomUIController;
	
	private ChessGameRoomController roomController;
	
	private ChessBoardPlayView boardPlayView;
	
	private ChessboardController chessboardController; 
	
	private PgnScreenTextView pgnScreenTextView;
	
	private boolean boardIsEnabled;
	
	private ProgressDialog pg;

	// *********************************************************************
	// *********************************************************************
	// Activity life cycle interface
	// *********************************************************************
	// *********************************************************************
	
	protected void onCreate(Bundle savedInstanceState){
		Log.d(TAG, "onCreate :: "+savedInstanceState);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game_room);
		this.chessGameModel = new GameModel();
		this.boardPlayView = (ChessBoardPlayView)findViewById(R.id.chessboard);
		this.pgnScreenTextView  = new PgnScreenTextView(new PGNOptions());
		this.roomController = new ChessGameRoomController(this);		
		this.gameRoomUIController = new ChessGameRoomUIController(this);
		this.chessboardController = new ChessboardController(this.gameRoomUIController, this.pgnScreenTextView,new PGNOptions());
		this.realTimechessGameController = new RealTimeChessGameController(this);
		this.chessboardController.newGame(new ChessboardMode(ChessboardMode.ANALYSIS), false);
		this.installChessBoardTouchListener();			
		this.boardIsEnabled = true;				
	}

	protected void onStart(){
    	super.onStart();
    	    	
    	Intent intent = getIntent();
		boolean isRoomCreator = intent.getBooleanExtra("isRoomCreator",true);
		String remotePlayer = intent.getStringExtra("remotePlayer");
		int gameVariant = intent.getIntExtra("gameVariant",0);
		
		Log.d(TAG, "onStart :: isRoomCreator="+isRoomCreator+",remotePlayer="+remotePlayer+",gameVariant="+gameVariant);
		
		if(isRoomCreator){
			this.createGameRoom(remotePlayer , gameVariant);
		}
		else{
			this.joinGameRoom(intent.getStringExtra("invitationId"),remotePlayer,gameVariant);
		}
	}

	protected void onRestart(){
    	super.onRestart();
    	Log.d(TAG, "onRestart :: ");
	}

    protected void onResume(){
    	super.onResume();
    	Log.d(TAG, "onResume :: ");
	}

    protected void onPause(){
    	super.onPause();
    	Log.d(TAG, "onPause :: ");
	}

    protected void onStop(){
    	super.onStop();
    	Log.d(TAG, "onStop :: ");
    	
    	if( this.chessGameModel.getRoom() != null ){    		
    		GameController.getInstance().leaveRoom(this.chessGameModel.getRoom(),this.roomController);
    	}
	}

    protected void onDestroy(){
    	super.onDestroy();
    	Log.d(TAG, "onDestroy :: ");
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.game_menu, menu);
        return true;
    }
    	
	// *********************************************************************
	// *********************************************************************
	// Get and set methods
	// *********************************************************************
	// *********************************************************************
	
	public GameModel getGameModel() {		
		return this.chessGameModel;
	}
	
	public String getWitePlayerName() {
		return "whitePlayer";
	}

	public String getBlackPlayerName() {
		return "blackPlayer";
	}
	
	public ChessGameRoomUIController getGameRoomUIController() {
		return gameRoomUIController;
	}

	public void setGameRoomUIController(
			ChessGameRoomUIController gameRoomUIController) {
		this.gameRoomUIController = gameRoomUIController;
	}

	public ChessGameRoomController getRoomController() {
		return roomController;
	}

	public void setRoomController(ChessGameRoomController roomController) {
		this.roomController = roomController;
	}

	public ChessBoardPlayView getBoardPlayView() {
		return boardPlayView;
	}

	public void setBoardPlayView(ChessBoardPlayView boardPlayView) {
		this.boardPlayView = boardPlayView;
	}

	public ChessboardController getChessboardController() {
		return chessboardController;
	}

	public void setChessboardController(ChessboardController chessboardController) {
		this.chessboardController = chessboardController;
	}

	public PgnScreenTextView getPgnScreenTextView() {
		return pgnScreenTextView;
	}

	public void setPgnScreenTextView(PgnScreenTextView pgnScreenTextView) {
		this.pgnScreenTextView = pgnScreenTextView;
	}
	
	// *********************************************************************
	// *********************************************************************
	// Private section
	// *********************************************************************
	// *********************************************************************

	private void createGameRoom(String remotePlayer, int gameVariant) {		
		Log.d(TAG, "createGameRoom :: remotePlayer="+remotePlayer+",gameVariant:"+Util.getGameVariant(gameVariant).toString());		
		RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this.roomController);
		rtmConfigBuilder.addPlayersToInvite(new String[] {remotePlayer});
		rtmConfigBuilder.setVariant(gameVariant);
		rtmConfigBuilder.setMessageReceivedListener(GameController.getInstance().getRealTimeChessGame());
		rtmConfigBuilder.setRoomStatusUpdateListener(this.roomController);
		this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		GameController.getInstance().getGamesClient().createRoom(rtmConfigBuilder.build());		
		Log.d(TAG, "Room created, waiting for it to be ready...");
		this.showWaitingDialog("Waiting ...",new Runnable() {
			
			@Override
			public void run() {
				ChessGameRoomUI.this.finish();				
			}
		});
	}
		
	private void joinGameRoom(String invitationId,String remotePlayer, int gameVariant) {		
		RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this.roomController);
		roomConfigBuilder.setInvitationIdToAccept(invitationId)
				.setMessageReceivedListener(GameController.getInstance().getRealTimeChessGame())
				.setRoomStatusUpdateListener(this.roomController);
		this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		GameController.getInstance().getGamesClient().joinRoom(roomConfigBuilder.build());		
		this.showWaitingDialog("Waiting for game to start!",new Runnable() {
			
			@Override
			public void run() {
				ChessGameRoomUI.this.finish();				
			}
		});
	}
		
	private void showWaitingDialog(String string,final Runnable runnable) {
		pg = ProgressDialog.show(this, "Waiting...", string, true, true, new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				Log.d(TAG, "Canceld");
				runnable.run();				
			}
		});							
	}
		
	private void installChessBoardTouchListener() {
    	final GestureDetector gd = new GestureDetector(this,gameRoomUIController);
		boardPlayView.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				Log.d(TAG, "onTouch");
								
				if( boardIsEnabled){
					return gd.onTouchEvent(event);
				}	
				else{
					displayShortMessage("Table disabled!");
					return false;
				}
			}
		});		
	}
	
	private void displayShortMessage(String string) {
		Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT)
				.show();
	}

	public void roomIsFull() {
		if( pg != null && pg.isShowing() ){
			pg.dismiss();
		}
		
		Toast.makeText(getApplicationContext(), "room is full", Toast.LENGTH_LONG).show();		
	}
}
