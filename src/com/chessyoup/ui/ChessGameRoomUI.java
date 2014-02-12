package com.chessyoup.ui;

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
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.chessyoup.R;
import com.chessyoup.chessboard.ChessboardController;
import com.chessyoup.chessboard.ChessboardMode;
import com.chessyoup.game.GameController;
import com.chessyoup.game.GameModel;
import com.chessyoup.game.GameVariant;
import com.chessyoup.game.Util;
import com.chessyoup.game.view.ChessBoardPlayView;
import com.chessyoup.game.view.PgnScreenTextView;
import com.chessyoup.model.Game.GameState;
import com.chessyoup.model.pgn.PGNOptions;
import com.chessyoup.ui.ctrl.GameUIController;
import com.chessyoup.ui.ctrl.RoomController;
import com.chessyoup.ui.ctrl.RoomGameController;
import com.chessyoup.ui.util.UIUtil;
import com.google.android.gms.common.images.ImageManager;

public class ChessGameRoomUI extends FragmentActivity {

    private final static String TAG = "ChessGameRoomUI";
    
    private GameModel chessGameModel;

    private GameUIController gameUIController;

    private RoomGameController roomGameController;

    private RoomController roomController;

    private ChessBoardPlayView chessBoardPlayView;

    private ChessboardController chessboardController;

    private PgnScreenTextView pgnScreenTextView;

    private boolean boardIsEnabled;

    private ProgressDialog pg;

    private TextView localClockView, remoteClockView, localPlayerView, remotePlayerView;
    
    private GameStartData gameStartData;
    
    // *********************************************************************
    // *********************************************************************
    // Activity life cycle interface
    // *********************************************************************
    // *********************************************************************
    
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate :: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_room);
        this.localClockView = (TextView) findViewById(R.id.localPlayerClockView);
        this.remoteClockView = (TextView) findViewById(R.id.remotePlayerClockView);
        this.localPlayerView = (TextView) findViewById(R.id.localPlayerDisplayNameView);
        this.remotePlayerView = (TextView) findViewById(R.id.remotePlayerDisplayNameView);
        this.chessGameModel = new GameModel();
        this.roomGameController = new RoomGameController(this);
        this.chessBoardPlayView = (ChessBoardPlayView) findViewById(R.id.chessboard);
        this.pgnScreenTextView = new PgnScreenTextView(new PGNOptions());
        this.roomController = new RoomController(this);
        this.gameUIController = new GameUIController(this);
        this.chessboardController = new ChessboardController(this.gameUIController, this.pgnScreenTextView, new PGNOptions());
        this.chessboardController.newGame(new ChessboardMode(ChessboardMode.ANALYSIS), false);
        this.installChessBoardTouchListener();
        this.boardIsEnabled = true;        
        this.gameStartData = getRoomStartState(savedInstanceState);
    }

    protected void onStart() {
        super.onStart();
        
        if( this.gameStartData != null){            
            Log.d(TAG, "onStart :: " + gameStartData.toString());

            if (this.gameStartData.isChallanger) {
                this.createGameRoom(this.gameStartData.remotePlayer,this.gameStartData.gameVariant);
            } else {
                this.joinGameRoom(this.gameStartData.invitationId);
            }

            this.updateLocalPlayerView(this.gameStartData.gameVariant, this.gameStartData.isChallanger, true);
        }
        else{
            displayShortMessage("Empty board!");
        }                
    }

    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart :: ");
    }

    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume :: ");
    }

    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause :: ");
    }

    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop :: ");
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy :: ");

        this.gameUIController.stopClock();

        if (this.chessGameModel.getRoom() != null) {
            if (this.chessGameModel != null && this.chessGameModel.getRoom() != null) {
                GameController.getInstance().leaveRoom(this.roomController, this.chessGameModel.getRoom().getRoomId());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.game_menu, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        this.handleExitAction(getString(R.string.option_resign_game));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected :: "+item.getItemId()+"");
        
        switch (item.getItemId()) {
            case R.id.menu_resign:
                this.handleResignAction();
                return true;
            case R.id.menu_exit:                
                this.handleExitAction(getString(R.string.close_room_message));
                return true;
            case R.id.menu_rematch:
                this.handleRematchAction();                
                return true;
            case R.id.menu_draw:
                this.handleDrawAction();
                return true;
            case R.id.menu_abort:
                this.handleAbortAction();
                return true;
            case R.id.menu_previous:
                this.handleGameGoBackAction();                
                return true;
            case R.id.menu_next:
                this.handleGameGoForwardAction();                         
                return true;
            case R.id.menu_start:
                this.handleGameGoToStartAction();                         
                return true;
            case R.id.menu_end:
                this.handleGameGoToEndAction();                         
                return true;
        }

        return true;
    }

    // *********************************************************************
    // *********************************************************************
    // Business interface
    // *********************************************************************
    // *********************************************************************
    
    public void remotePlayerLeft() {
        
        ((ImageView) findViewById(R.id.remotePlayerAvatarView)).setImageResource(R.drawable.general_avatar_unknown);
        this.remotePlayerView.setText("");
        
        if( chessGameModel != null && chessGameModel.getRemotePlayer() != null ){
            displayShortMessage(chessGameModel.getRemotePlayer().getParticipant().getDisplayName() +" left. You win!!!");
        }
        
        if (chessboardController.getGame().getGameState() == GameState.ALIVE) {
            if (chessGameModel.getBlackPlayer().getParticipant().getParticipantId().equals(chessGameModel.getRemotePlayer().getParticipant().getParticipantId())) {
                this.getChessboardController().resignGameForBlack();            
            } else {
                this.getChessboardController().resignGameForWhite();
            }            
        }
        
        chessboardController.setGameMode(new ChessboardMode(ChessboardMode.ANALYSIS));
    }
    
    public void rematchConfig() {
        
        if(GameController.getInstance().isInitilized() && this.chessGameModel != null ){
            GameVariant gameVariant = this.chessGameModel.getGameVariant(); 
            gameVariant.setWhite(gameVariant.isWhite() ? false : true);// switch side
            GameController.getInstance().getRealTimeChessGame().sendChallange(Util.gameVariantToInt(gameVariant), true);
        }        
    }
    
    public void roomReady() {
        if (pg != null && pg.isShowing()) {
            pg.dismiss();
        }
        
        this.updateChessboard();
        this.updateRemotePlayerView();

        Toast.makeText(getApplicationContext(), "Room is ready", Toast.LENGTH_LONG).show();

        if (chessBoardPlayView.isFlipped()) {
            this.chessboardController.newGame(new ChessboardMode(ChessboardMode.TWO_PLAYERS_WHITE_REMOTE), false);
        } else {
            this.chessboardController.newGame(new ChessboardMode(ChessboardMode.TWO_PLAYERS_BLACK_REMOTE), false);
        }

        this.chessboardController.startGame();
    }

    public void gameFinished() {
        this.chessboardController.setGameMode(new ChessboardMode(ChessboardMode.ANALYSIS));        
    }

    public void updateClocks(String whiteTime, String blackTime) {
        this.remoteClockView.setText(chessBoardPlayView.isFlipped() ? whiteTime : blackTime);
        this.localClockView.setText(chessBoardPlayView.isFlipped() ? blackTime : whiteTime);
    }

    public void updateRemotePlayerView() {

        if (chessGameModel.getRemotePlayer().getParticipant().getIconImageUri() != null) {
            ImageManager.create(this.getApplicationContext()).loadImage((ImageView) findViewById(R.id.remotePlayerAvatarView), chessGameModel.getRemotePlayer().getParticipant().getIconImageUri());
        }

        StringBuffer sb = new StringBuffer(chessGameModel.getRemotePlayer().getParticipant().getDisplayName());
        sb.append(" (").append(Math.round(chessGameModel.getRemotePlayer().getRating())).append(")");
        this.remotePlayerView.setText(sb.toString());
    }

    public void updateLocalPlayerView(int gameVariant, boolean isChallanger, boolean loadAvatar) {
        
        if (loadAvatar) {
            ImageManager.create(this.getApplicationContext()).loadImage((ImageView) findViewById(R.id.localPlayerAvatarView),
                            GameController.getInstance().getGamesClient().getCurrentPlayer().getIconImageUri());
        }

        StringBuffer sb = new StringBuffer(GameController.getInstance().getGamesClient().getCurrentPlayer().getDisplayName());
        sb.append(" (").append(Math.round(GameController.getInstance().getLocalPlayer().getRating())).append(")");
        this.localPlayerView.setText(sb.toString());        
    }

    public void updateChessboard(){    	  
        GameVariant gv = chessGameModel.getGameVariant();
        chessBoardPlayView.setFlipped(false);        
                
        if (gv.isWhite()) {
            chessGameModel.setWhitePlayer(GameController.getInstance().getLocalPlayer());
            chessGameModel.setBlackPlayer(chessGameModel.getRemotePlayer());              
        }
        else{
            chessGameModel.setWhitePlayer(chessGameModel.getRemotePlayer());
            chessGameModel.setBlackPlayer(GameController.getInstance().getLocalPlayer());
            chessBoardPlayView.setFlipped(true);           
        }
                

        chessboardController.setTimeLimit(gv.getTime() * 1000, gv.getMoves(), gv.getIncrement() * 1000);
        this.updateClocks(UIUtil.timeToString(gv.getTime() * 1000), UIUtil.timeToString(gv.getTime() * 1000));
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

    public GameUIController getGameRoomUIController() {
        return gameUIController;
    }

    public void setGameRoomUIController(GameUIController gameRoomUIController) {
        this.gameUIController = gameRoomUIController;
    }

    public RoomController getRoomController() {
        return roomController;
    }

    public void setRoomController(RoomController roomController) {
        this.roomController = roomController;
    }

    public ChessBoardPlayView getBoardPlayView() {
        return chessBoardPlayView;
    }

    public void setBoardPlayView(ChessBoardPlayView boardPlayView) {
        this.chessBoardPlayView = boardPlayView;
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
    
    private void handleGameGoToEndAction() {        
        if( chessboardController.getGameMode().getModeNr() == ChessboardMode.ANALYSIS){
            Log.d(TAG, "game goToStart call :: ");                                        
            chessboardController.gotoMove(10000);
        }
    }

    private void handleGameGoToStartAction() {
        if( chessboardController.getGameMode().getModeNr() == ChessboardMode.ANALYSIS){
            Log.d(TAG, "game goToStart call :: ");                                        
            chessboardController.gotoStartOfVariation();
        }
    }

    private void handleGameGoForwardAction() { 
        if( chessboardController.getGameMode().getModeNr() == ChessboardMode.ANALYSIS){
            Log.d(TAG, "game goForward call :: ");                                        
            chessboardController.gotoMove( chessboardController.getGame().currPos().fullMoveCounter+1 );
        }
    }

    private void handleGameGoBackAction() { 
        if( chessboardController.getGameMode().getModeNr() == ChessboardMode.ANALYSIS){
            Log.d(TAG, "game goBack call :: ");                    
            int nr = chessboardController.getGame().currPos().fullMoveCounter-1;                    
            chessboardController.gotoMove( nr >= 0 ? nr : 0);
        }
    }

    private void handleAbortAction() {
    	
    	if( chessboardController.isAbortRequested() ){
    		if(GameController.getInstance().isInitilized()){
                GameController.getInstance().getRealTimeChessGame().abort();
            }
        	
            chessboardController.abortGame();
    	}
    	else{
    	
	        UIUtil.buildConfirmAlertDialog(this, getString(R.string.option_abort_game), new Runnable() {
	
	            @Override
	            public void run() {
	            	
	                GameController.getInstance().getRealTimeChessGame().abort();
	                chessboardController.setAbortRequested(true);
	                displayShortMessage(getString(R.string.abort_request_message));
	            }
	        }).show();          
    	}
    }

    private void handleDrawAction() {
        if( chessboardController.isDrawRequested() ){
        	
        	if(GameController.getInstance().isInitilized()){
                GameController.getInstance().getRealTimeChessGame().draw();
            }
        	
            chessboardController.drawGame();
        }
        else{
            
        	if(GameController.getInstance().isInitilized()){
                GameController.getInstance().getRealTimeChessGame().draw();
                chessboardController.setDrawRequested(true);
            }
        	
            displayShortMessage(getString(R.string.draw_request_message));    
        }
    }

    private void handleRematchAction() {        
        if( this.chessboardController.isRemtachRequested() ){
        	this.rematchConfig();
        }
        else{
            
            if(GameController.getInstance().isInitilized()){
                GameController.getInstance().getRealTimeChessGame().rematch();
            }
            
            displayShortMessage(getString(R.string.remtach_request_message));
        }
    }

    private void handleResignAction() {        
        UIUtil.buildConfirmAlertDialog(this, getString(R.string.option_resign_game), new Runnable() {

            @Override
            public void run() {
                
                if(GameController.getInstance().isInitilized()){
                    GameController.getInstance().getRealTimeChessGame().resign();
                }
                                        
                chessboardController.resignGame();                                                
            }
        }).show();        
    }

    private void handleExitAction(String message) {
                
        if (chessboardController.getGame().getGameState() == GameState.ALIVE) {
            UIUtil.buildConfirmAlertDialog(this, message, new Runnable() {

                @Override
                public void run() {
                    finish();
                }
            }).show();
        } else {
            finish();
        }
    }
    
    class GameStartData{
        String remotePlayer;
        boolean isChallanger;
        String invitationId;
        int gameVariant;
    }
    
    public GameStartData getRoomStartState(Bundle savedInstanceState){
        GameStartData data = new GameStartData(); 
        
        if( savedInstanceState != null ){            
            //TODO restore from saved state
            return null;            
        }
        else{                        
            Intent intent = getIntent();
            data.remotePlayer = intent.getStringExtra(ChessYoUpActivity.REMOTE_PLAYER_EXTRA);
            
            if( data.remotePlayer != null ){                 
                data.isChallanger = intent.getBooleanExtra(ChessYoUpActivity.IS_CHALANGER_EXTRA, false);
                data.gameVariant =  intent.getIntExtra(ChessYoUpActivity.GAME_VARIANT_EXTRA, 0);
                data.invitationId = intent.getStringExtra(ChessYoUpActivity.INVITATION_ID_EXTRA);                                              
                return data;
            }
            else{
                return null;
            }                       
        }
    }
    
    public RoomGameController getRealTimeChessGameController() {
        return roomGameController;
    }

    public void setRealTimeChessGameController(RoomGameController realTimeChessGameController) {
        this.roomGameController = realTimeChessGameController;
    }

    private void createGameRoom(String remotePlayer, int gameVariant) {
        Log.d(TAG, "createGameRoom :: remotePlayer=" + remotePlayer + ",gameVariant:" + Util.getGameVariant(gameVariant).toString());
        GameVariant gv = Util.getGameVariant(gameVariant);
        this.chessGameModel.setGameVariant(gv);                     
        GameController.getInstance().createRoom(this.roomController, this.roomController, remotePlayer, Util.switchSide(gv));
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.d(TAG, "Room created, waiting for it to be ready...");
        this.showWaitingDialog("Waiting ...", new Runnable() {

            @Override
            public void run() {
                ChessGameRoomUI.this.finish();
            }
        });
    }

    private void joinGameRoom(String invitationId) {       
        chessGameModel.setIncomingInvitationId(invitationId);
        GameController.getInstance().joinRoom(this.roomController, this.roomController, invitationId);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.showWaitingDialog("Waiting for game to start!", new Runnable() {

            @Override
            public void run() {
                ChessGameRoomUI.this.finish();
            }
        });
    }

    private void showWaitingDialog(String string, final Runnable runnable) {
        pg = ProgressDialog.show(this, "Waiting...", string, true, true, new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                Log.d(TAG, "Canceld");
                runnable.run();
            }
        });
    }

    private void installChessBoardTouchListener() {
        final GestureDetector gd = new GestureDetector(this, gameUIController);
        chessBoardPlayView.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                Log.d(TAG, "onTouch");

                if (boardIsEnabled) {
                    return gd.onTouchEvent(event);
                } else {
                    displayShortMessage("Table disabled!");
                    return false;
                }
            }
        });
    }

    public void displayShortMessage(String string) {
        Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT).show();
    }

    public void acceptChallange(GameVariant gameVariant) {
        // TODO accept new challange
        
    }
}
