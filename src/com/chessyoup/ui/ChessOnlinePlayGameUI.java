package com.chessyoup.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
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
import com.chessyoup.game.Util;
import com.chessyoup.game.chess.ChessGameController;
import com.chessyoup.game.chess.ChessGameModel;
import com.chessyoup.game.chess.ChessGameVariant;
import com.chessyoup.game.view.ChessBoardPlayView;
import com.chessyoup.game.view.PgnScreenTextView;
import com.chessyoup.model.Game.GameState;
import com.chessyoup.model.pgn.PGNOptions;
import com.chessyoup.ui.ctrl.ChessboardUIController;
import com.chessyoup.ui.ctrl.RealTimeChessGameController;
import com.chessyoup.ui.fragment.FragmentAdapter;
import com.chessyoup.ui.fragment.FragmentChat;
import com.chessyoup.ui.fragment.FragmentGame;
import com.chessyoup.ui.fragment.FragmentMoves;
import com.chessyoup.ui.util.UIUtil;
import com.google.android.gms.common.images.ImageManager;

public class ChessOnlinePlayGameUI extends FragmentActivity {

    private final static String TAG = "ChessGameRoomUI";

    private ChessGameModel chessGameModel;

    private ChessGameController chessGameController;

    private ChessboardUIController gameUIController;

    private RealTimeChessGameController roomGameController;
    
    private ChessBoardPlayView chessBoardPlayView;

    private ChessboardController chessboardController;

    private PgnScreenTextView pgnScreenTextView;

    private boolean boardIsEnabled;

    private ProgressDialog pg;

    private TextView localClockView, remoteClockView, localPlayerView, remotePlayerView;

    private ViewPager viewPager;

    private FragmentAdapter adapter;

    FragmentGame fGame;

    // *********************************************************************
    // *********************************************************************
    // Activity life cycle interface
    // *********************************************************************
    // *********************************************************************

    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate :: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_room);
        fGame = new FragmentGame();
        final FragmentMoves fMoves = new FragmentMoves();
        final FragmentChat fChat = new FragmentChat();
        adapter = new FragmentAdapter(this.getSupportFragmentManager());
        adapter.addFragment(fGame);
        adapter.addFragment(fMoves);
        adapter.addFragment(fChat);
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(adapter);
        this.chessGameController = ChessGameController.getController();
        this.chessGameModel = this.chessGameController.findChessModelByRoomId(getIntent().getStringExtra(ChessYoUpActivity.ROOM_ID_EXTRA));
        this.roomGameController = new RealTimeChessGameController(this);
        this.pgnScreenTextView = new PgnScreenTextView(new PGNOptions());        
        this.gameUIController = new ChessboardUIController(this);
        this.chessboardController = new ChessboardController(this.gameUIController, this.pgnScreenTextView, new PGNOptions());
        this.boardIsEnabled = true;        

        fGame.runInstallListeners = new Runnable() {

            @Override
            public void run() {
                chessBoardPlayView = fGame.chessBoardPlayView;
                localClockView = fGame.localClockView;
                remoteClockView = fGame.remoteClockView;
                localPlayerView = fGame.localPlayerView;
                remotePlayerView = fGame.remotePlayerView;
                installListeners();
                showGameNaviation(false);

                if (chessGameModel != null) {
                    Log.d(TAG, "onStart :: send ready message to remote : " + chessGameModel.getRemotePlayer());
                    chessGameController.getRealTimeGameClient().ready(chessGameController.getLocalPlayer());
                    updateChessboard();
                    updateRemotePlayerView(true);
                    updateLocalPlayerView(true);
                } else {
                    chessboardController.newGame(new ChessboardMode(ChessboardMode.ANALYSIS),false);
                    displayShortMessage("Empty board!");
                }
            }
        };
    }

    protected void onStart() {
        Log.d(TAG, "onStart :: ");
        super.onStart();
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
                chessGameController.leaveRoom(this.chessGameModel.getRoom().getRoomId());
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
        Log.d(TAG, "onOptionsItemSelected :: " + item.getItemId() + "");

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
            case R.id.menu_game:
                this.viewPager.setCurrentItem(0);
                return true;
            case R.id.menu_moves:
                this.viewPager.setCurrentItem(1);
                return true;
            case R.id.menu_chat:
                this.viewPager.setCurrentItem(2);
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

        if (chessGameModel != null && chessGameModel.getRemotePlayer() != null) {
            displayShortMessage(chessGameModel.getRemotePlayer().getPlayer().getDisplayName() + " left. You win!!!");
        }

        if (chessboardController.getGame().getGameState() == GameState.ALIVE) {
            if (chessGameModel.getBlackPlayer().getPlayer().getPlayerId().equals(chessGameModel.getRemotePlayer().getPlayer().getPlayerId())) {
                this.getChessboardController().resignGameForBlack();
            } else {
                this.getChessboardController().resignGameForWhite();
            }
        }

        chessboardController.setGameMode(new ChessboardMode(ChessboardMode.ANALYSIS));
    }

    public void rematchConfig() {

        if (this.chessGameModel != null) {
            ChessGameVariant gameVariant = this.chessGameModel.getGameVariant();
            gameVariant.setWhite(gameVariant.isWhite() ? false : true);
            chessGameController.getRealTimeGameClient().sendChallange(Util.gameVariantToInt(gameVariant), true);
        }
    }

    public void roomReady() {
        if (pg != null && pg.isShowing()) {
            pg.dismiss();
        }

        this.updateChessboard();
        this.updateRemotePlayerView(false);

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
        showGameNaviation(true);
    }

    public void updateClocks(String whiteTime, String blackTime) {
        this.remoteClockView.setText(chessBoardPlayView.isFlipped() ? whiteTime : blackTime);
        this.localClockView.setText(chessBoardPlayView.isFlipped() ? blackTime : whiteTime);
    }

    public void updateRemotePlayerView(boolean loadAvatar) {

        if (loadAvatar && chessGameModel.getRemotePlayer().getPlayer().getIconImageUri() != null) {
            ImageManager.create(this.getApplicationContext()).loadImage((ImageView) findViewById(R.id.remotePlayerAvatarView), chessGameModel.getRemotePlayer().getPlayer().getIconImageUri());
        }

        StringBuffer sb = new StringBuffer(chessGameModel.getRemotePlayer().getPlayer().getDisplayName());
        sb.append(" (").append(Math.round(chessGameModel.getRemotePlayer().getRating())).append(")");
        this.remotePlayerView.setText(sb.toString());
    }

    public void updateLocalPlayerView(boolean loadAvatar) {
        if (loadAvatar) {

            ImageManager.create(this.getApplicationContext()).loadImage((ImageView) findViewById(R.id.localPlayerAvatarView), chessGameController.getLocalPlayer().getPlayer().getIconImageUri());

        }

        StringBuffer sb = new StringBuffer(chessGameController.getLocalPlayer().getPlayer().getDisplayName());
        sb.append(" (").append(Math.round(chessGameController.getLocalPlayer().getRating())).append(")");
        this.localPlayerView.setText(sb.toString());
    }

    public void updateChessboard() {
        chessBoardPlayView.setFlipped(chessGameModel.getBlackPlayer().getPlayer().getPlayerId().equals(chessGameController.getLocalPlayer().getPlayer().getPlayerId()));
        ChessGameVariant gv = chessGameModel.getGameVariant();
        chessboardController.setTimeLimit(gv.getTime() * 1000, gv.getMoves(), gv.getIncrement() * 1000);
        this.updateClocks(UIUtil.timeToString(gv.getTime() * 1000), UIUtil.timeToString(gv.getTime() * 1000));
    }

    // *********************************************************************
    // *********************************************************************
    // Get and set methods
    // *********************************************************************
    // *********************************************************************

    public ChessGameModel getGameModel() {
        return this.chessGameModel;
    }

    public String getWitePlayerName() {
        return "whitePlayer";
    }

    public String getBlackPlayerName() {
        return "blackPlayer";
    }

    public ChessboardUIController getGameRoomUIController() {
        return gameUIController;
    }

    public void setGameRoomUIController(ChessboardUIController gameRoomUIController) {
        this.gameUIController = gameRoomUIController;
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
        if (chessboardController.getGameMode().getModeNr() == ChessboardMode.ANALYSIS) {
            Log.d(TAG, "game goToStart call :: ");
            chessboardController.gotoMove(10000);
        }
    }

    private void handleGameGoToStartAction() {
        if (chessboardController.getGameMode().getModeNr() == ChessboardMode.ANALYSIS) {
            Log.d(TAG, "game goToStart call :: ");
            chessboardController.gotoStartOfVariation();
        }
    }

    private void handleGameGoForwardAction() {
        if (chessboardController.getGameMode().getModeNr() == ChessboardMode.ANALYSIS) {
            Log.d(TAG, "game goForward call :: ");
            chessboardController.gotoMove(chessboardController.getGame().currPos().fullMoveCounter + 1);
        }
    }

    private void handleGameGoBackAction() {
        if (chessboardController.getGameMode().getModeNr() == ChessboardMode.ANALYSIS) {
            Log.d(TAG, "game goBack call :: ");
            int nr = chessboardController.getGame().currPos().fullMoveCounter - 1;
            chessboardController.gotoMove(nr >= 0 ? nr : 0);
        }
    }

    private void handleAbortAction() {

        if (chessboardController.isAbortRequested()) {
            chessGameController.getRealTimeGameClient().abort();
            chessboardController.abortGame();
        } else {

            UIUtil.buildConfirmAlertDialog(this, getString(R.string.option_abort_game), new Runnable() {

                @Override
                public void run() {
                    chessGameController.getRealTimeGameClient().abort();
                    chessboardController.setAbortRequested(true);
                    displayShortMessage(getString(R.string.abort_request_message));
                }
            }).show();
        }
    }

    private void handleDrawAction() {
        if (chessboardController.isDrawRequested()) {
            chessGameController.getRealTimeGameClient().draw();
            chessboardController.drawGame();
        } else {

            chessGameController.getRealTimeGameClient().draw();
            chessboardController.setDrawRequested(true);

            displayShortMessage(getString(R.string.draw_request_message));
        }
    }

    private void handleRematchAction() {
        if (this.chessboardController.isRemtachRequested()) {
            this.rematchConfig();
        } else {
            chessGameController.getRealTimeGameClient().rematch();            
            displayShortMessage(getString(R.string.remtach_request_message));
        }
    }

    private void handleResignAction() {
        UIUtil.buildConfirmAlertDialog(this, getString(R.string.option_resign_game), new Runnable() {

            @Override
            public void run() {
                chessGameController.getRealTimeGameClient().resign();                
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

//    class GameStartData {
//        String remotePlayer;
//        boolean isChallanger;
//        String invitationId;
//        int gameVariant;
//    }

//    public GameStartData getRoomStartState(Bundle savedInstanceState) {
//        GameStartData data = new GameStartData();
//
//        if (savedInstanceState != null) {
//            // TODO restore from saved state
//            return null;
//        } else {
//            Intent intent = getIntent();
//            data.remotePlayer = intent.getStringExtra(ChessYoUpActivity.REMOTE_PLAYER_EXTRA);
//
//            if (data.remotePlayer != null) {
//                data.isChallanger = intent.getBooleanExtra(ChessYoUpActivity.IS_CHALANGER_EXTRA, false);
//                data.gameVariant = intent.getIntExtra(ChessYoUpActivity.GAME_VARIANT_EXTRA, 0);
//                data.invitationId = intent.getStringExtra(ChessYoUpActivity.INVITATION_ID_EXTRA);
//                return data;
//            } else {
//                return null;
//            }
//        }
//    }

    public RealTimeChessGameController getRealTimeChessGameController() {
        return roomGameController;
    }

    public void setRealTimeChessGameController(RealTimeChessGameController realTimeChessGameController) {
        this.roomGameController = realTimeChessGameController;
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

    private void showGameNaviation(boolean show) {
        fGame.gameNavStart.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        fGame.gameNavPrev.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        fGame.gameNavNext.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        fGame.gameNavEnd.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    private void installListeners() {


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

        fGame.gameNavStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                handleGameGoToStartAction();
            }
        });

        fGame.gameNavPrev.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                handleGameGoBackAction();
            }
        });

        fGame.gameNavNext.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                handleGameGoForwardAction();
            }
        });

        fGame.gameNavEnd.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                handleGameGoToEndAction();;
            }
        });
    }

    public void displayShortMessage(String string) {
        Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT).show();
    }

    public void acceptChallange(ChessGameVariant gameVariant) {
        // TODO accept new challange

    }
}
