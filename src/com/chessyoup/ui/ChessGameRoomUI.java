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
import com.chessyoup.ui.ctrl.ChessGameRoomController;
import com.chessyoup.ui.ctrl.ChessGameUIController;
import com.chessyoup.ui.ctrl.RealTimeChessGameController;
import com.chessyoup.ui.util.UIUtil;
import com.google.android.gms.common.images.ImageManager;

public class ChessGameRoomUI extends FragmentActivity {

    private final static String TAG = "ChessGameRoomUI";

    private GameModel chessGameModel;

    private ChessGameUIController gameRoomUIController;

    private RealTimeChessGameController realTimeChessGameController;

    private ChessGameRoomController roomController;

    private ChessBoardPlayView chessBoardPlayView;

    private ChessboardController chessBoardController;

    private PgnScreenTextView pgnScreenTextView;

    private boolean boardIsEnabled;

    private ProgressDialog pg;

    private TextView localClockView, remoteClockView, localPlayerView, remotePlayerView;

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
        this.chessBoardPlayView = (ChessBoardPlayView) findViewById(R.id.chessboard);
        this.realTimeChessGameController = new RealTimeChessGameController(this);
        this.pgnScreenTextView = new PgnScreenTextView(new PGNOptions());
        this.roomController = new ChessGameRoomController(this);
        this.gameRoomUIController = new ChessGameUIController(this);
        this.chessBoardController = new ChessboardController(this.gameRoomUIController, this.pgnScreenTextView, new PGNOptions());
        this.chessBoardController.newGame(new ChessboardMode(ChessboardMode.ANALYSIS), false);
        this.installChessBoardTouchListener();
        this.boardIsEnabled = true;
    }

    protected void onStart() {
        super.onStart();

        Intent intent = getIntent();
        boolean isChallanger = intent.getBooleanExtra("isChallanger", true);
        String remotePlayer = intent.getStringExtra("remotePlayer");
        int gameVariant = intent.getIntExtra("gameVariant", 0);

        Log.d(TAG, "onStart :: isChallanger=" + isChallanger + ",remotePlayer=" + remotePlayer + ",gameVariant=" + gameVariant);

        if (isChallanger) {
            this.createGameRoom(remotePlayer, gameVariant);
        } else {
            this.joinGameRoom(intent.getStringExtra("invitationId"));
        }

        this.updateLocalPlayerView(gameVariant, isChallanger, true);
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

        this.gameRoomUIController.stopClock();

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
        Log.d(TAG, "onOptionsItemSelected :: "+item.toString());
        
        switch (item.getItemId()) {
            case R.id.resignGameButton:
                UIUtil.buildConfirmAlertDialog(this, getString(R.string.option_resign_game), new Runnable() {

                    @Override
                    public void run() {
                        GameController.getInstance().getRealTimeChessGame().resign();
                        gameFinished();
                    }
                });

                return true;
            case R.id.exitGameButton:
                handleExitAction(getString(R.string.close_room_message));
                return true;
            case R.id.rematchGameButton:
                GameController.getInstance().getRealTimeChessGame().rematch();
                displayShortMessage(getString(R.string.remtach_request_message));
                return true;
            case R.id.drawGameButton:
                GameController.getInstance().getRealTimeChessGame().draw();
                displayShortMessage(getString(R.string.draw_request_message));
                return true;
            case R.id.abortGameButton:
                UIUtil.buildConfirmAlertDialog(this, getString(R.string.option_resign_game), new Runnable() {

                    @Override
                    public void run() {
                        GameController.getInstance().getRealTimeChessGame().abort();
                        displayShortMessage(getString(R.string.abort_request_message));
                    }
                });                
                                
                return true;
            case R.id.previous:
                if( chessBoardController.getGameMode().getModeNr() == ChessboardMode.ANALYSIS){
                    chessBoardController.getGame().tree.goBack();                    
                }
                return true;
            case R.id.next:
                if( chessBoardController.getGameMode().getModeNr() == ChessboardMode.ANALYSIS){
                    chessBoardController.getGame().tree.goForward(0);
                }
                return true;
        }

        return true;
    }

    private void handleExitAction(String message) {

        if (chessBoardController.getGame().getGameState() == GameState.ALIVE) {
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

    // *********************************************************************
    // *********************************************************************
    // Business interface
    // *********************************************************************
    // *********************************************************************

    public void roomReady() {
        if (pg != null && pg.isShowing()) {
            pg.dismiss();
        }

        this.updateRemotePlayerView(true);

        Toast.makeText(getApplicationContext(), "Room is ready", Toast.LENGTH_LONG).show();

        if (chessBoardPlayView.isFlipped()) {
            this.chessBoardController.newGame(new ChessboardMode(ChessboardMode.TWO_PLAYERS_WHITE_REMOTE), false);
        } else {
            this.chessBoardController.newGame(new ChessboardMode(ChessboardMode.TWO_PLAYERS_BLACK_REMOTE), false);
        }

        this.chessBoardController.startGame();
    }

    public void gameFinished() {
        this.chessBoardController.setGameMode(new ChessboardMode(ChessboardMode.ANALYSIS));

    }

    public void updateClocks(String whiteTime, String blackTime) {
        this.remoteClockView.setText(chessBoardPlayView.isFlipped() ? whiteTime : blackTime);
        this.localClockView.setText(chessBoardPlayView.isFlipped() ? blackTime : whiteTime);
    }

    public void updateRemotePlayerView(boolean loadAvatar) {

        if (loadAvatar && chessGameModel.getRemotePlayer().getParticipant().getIconImageUri() != null) {
            ImageManager.create(this.getApplicationContext()).loadImage((ImageView) findViewById(R.id.remotePlayerAvatarView), chessGameModel.getRemotePlayer().getParticipant().getIconImageUri());
        }

        StringBuffer sb = new StringBuffer(chessGameModel.getRemotePlayer().getParticipant().getDisplayName());
        sb.append(" (").append(Math.round(chessGameModel.getRemotePlayer().getRating())).append(")");
        this.remotePlayerView.setText(sb.toString());
    }

    public void updateLocalPlayerView(int gameVariant, boolean isChallanger, boolean loadAvatar) {

        GameVariant gv = Util.getGameVariant(gameVariant);

        if (loadAvatar) {
            ImageManager.create(this.getApplicationContext()).loadImage((ImageView) findViewById(R.id.localPlayerAvatarView),
                            GameController.getInstance().getGamesClient().getCurrentPlayer().getIconImageUri());
        }

        StringBuffer sb = new StringBuffer(GameController.getInstance().getGamesClient().getCurrentPlayer().getDisplayName());
        sb.append(" (").append(Math.round(GameController.getInstance().getLocalPlayer().getRating())).append(")");
        this.localPlayerView.setText(sb.toString());

        chessBoardPlayView.setFlipped(false);

        if (isChallanger) {
            if (!gv.isWhite()) {
                chessBoardPlayView.setFlipped(true);
            }
        } else {
            if (gv.isWhite()) {
                chessBoardPlayView.setFlipped(true);
            }
        }

        chessBoardController.setTimeLimit(gv.getTime() * 1000, gv.getMoves(), gv.getIncrement() * 1000);
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

    public ChessGameUIController getGameRoomUIController() {
        return gameRoomUIController;
    }

    public void setGameRoomUIController(ChessGameUIController gameRoomUIController) {
        this.gameRoomUIController = gameRoomUIController;
    }

    public ChessGameRoomController getRoomController() {
        return roomController;
    }

    public void setRoomController(ChessGameRoomController roomController) {
        this.roomController = roomController;
    }

    public ChessBoardPlayView getBoardPlayView() {
        return chessBoardPlayView;
    }

    public void setBoardPlayView(ChessBoardPlayView boardPlayView) {
        this.chessBoardPlayView = boardPlayView;
    }

    public ChessboardController getChessboardController() {
        return chessBoardController;
    }

    public void setChessboardController(ChessboardController chessboardController) {
        this.chessBoardController = chessboardController;
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

    public RealTimeChessGameController getRealTimeChessGameController() {
        return realTimeChessGameController;
    }

    public void setRealTimeChessGameController(RealTimeChessGameController realTimeChessGameController) {
        this.realTimeChessGameController = realTimeChessGameController;
    }

    private void createGameRoom(String remotePlayer, int gameVariant) {
        Log.d(TAG, "createGameRoom :: remotePlayer=" + remotePlayer + ",gameVariant:" + Util.getGameVariant(gameVariant).toString());
        GameController.getInstance().createRoom(this.roomController, this.roomController, remotePlayer, gameVariant);
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
        final GestureDetector gd = new GestureDetector(this, gameRoomUIController);
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
}
