package com.chessyoup.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.chessyoup.R;
import com.chessyoup.game.GameHelper.GameHelperListener;
import com.chessyoup.game.Util;
import com.chessyoup.game.chess.ChessGamePlayer;
import com.chessyoup.ui.ctrl.ChessGameController;
import com.chessyoup.ui.ctrl.GoogleAPIController;
import com.chessyoup.ui.ctrl.RoomController;
import com.chessyoup.ui.dialogs.NewGameDialog;
import com.chessyoup.ui.dialogs.NewGameDialog.NewGameDialogListener;
import com.chessyoup.ui.fragment.FragmentAdapter;
import com.chessyoup.ui.fragment.IncomingInvitationsFragment;
import com.chessyoup.ui.fragment.InvitationsAdapter;
import com.chessyoup.ui.fragment.OutgoingInvitationFragment;
import com.chessyoup.ui.fragment.RoomsAdapter;
import com.google.android.gms.appstate.OnStateLoadedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.Leaderboards;
import com.google.android.gms.games.leaderboard.Leaderboards.LoadPlayerScoreResult;
import com.google.android.gms.games.multiplayer.Invitation;

public class ChessYoUpActivity extends FragmentActivity implements NewGameDialogListener, OnStateLoadedListener, GameHelperListener {

    private final static String TAG = "ChessYoUpActivity";

    private GoogleAPIController apiController;

    private final static int RC_SELECT_PLAYERS = 10000;

    public static final String ROOM_ID_EXTRA = "roomId";

    private FragmentAdapter adapter;

    private ViewPager viewPager;

    private ChessGameController chessGameController;

    private InvitationsAdapter invitationsAdapter;

    private RoomsAdapter roomsAdapter;

    private RoomController roomController;

    private OutgoingInvitationFragment outgoingFragment;

    private IncomingInvitationsFragment incomingFragment;
    
    private TextView playerName;
    
    private TextView playerRating;
    
    private ImageView playerAvatar;
    
    // *********************************************************************
    // *********************************************************************
    // Activity methods
    // *********************************************************************
    // *********************************************************************

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.playerName =  ((TextView) findViewById(R.id.playerName));
        this.playerRating =  ((TextView) findViewById(R.id.playerRating));
        this.playerAvatar = (ImageView) findViewById(R.id.playerAvatar);
        roomController = new RoomController(this);
        apiController = GoogleAPIController.getInstance(this);
        chessGameController = ChessGameController.getController();
        chessGameController.setRoomController(roomController);
        adapter = new FragmentAdapter(this.getSupportFragmentManager());
        invitationsAdapter = new InvitationsAdapter(getApplicationContext());
        this.roomsAdapter = new RoomsAdapter(getApplicationContext());
        incomingFragment = new IncomingInvitationsFragment();
        incomingFragment.setInvitationsAdapter(invitationsAdapter);
        outgoingFragment = new OutgoingInvitationFragment();
        outgoingFragment.setRoomsAdapter(roomsAdapter);
        adapter.addFragment(incomingFragment);
        adapter.addFragment(outgoingFragment);
        viewPager = (ViewPager) findViewById(R.id.main_pager);
        viewPager.setAdapter(adapter);
        Util.TOP_RATING_BASE = getResources().getInteger(R.integer.leaderboard_top_rating_base_start);
        Util.LOW_RATING_BASE = getResources().getInteger(R.integer.leaderboard_low_rating_base_start);
        Util.DEFAULT_RATING_DEVIATION = getResources().getInteger(R.integer.default_rating_deviation);
        Util.DEFAULT_RATING_VOLATILITY = getResources().getInteger(R.integer.default_rating_volatility);

        installListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        apiController.connect();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "**** got onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "**** got onDestroy");
        super.onStop();
        apiController.diconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected :: " + item.getItemId() + "");

        switch (item.getItemId()) {
            case R.id.main_menu_exit:
                this.handleExitAction();
                return true;
            case R.id.main_menu_logout:
                this.handleLogoutAction();
                return true;
            case R.id.main_menu_invite:
                this.handleInviteActiom();
                return true;
            case R.id.main_menu_quick:
                this.handleQuickAction();
                return true;
            case R.id.main_menu_incoming:
                this.viewPager.setCurrentItem(0);
                return true;
            case R.id.main_menu_outgoing:
                this.viewPager.setCurrentItem(1);
                return true;
        }

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);

        switch (requestCode) {
            case RC_SELECT_PLAYERS:
                handleSelectPlayersResult(responseCode, intent);
                break;
            case GoogleAPIController.REQUEST_RESOLVE_ERROR: {
                GoogleAPIController.mResolvingError = false;
                if (requestCode == RESULT_OK) {
                    GoogleAPIController.getInstance(this).connect();
                }
            }
        }
    }

    @Override
    public void onStateConflict(int stateKey, String resolvedVersion, byte[] localData, byte[] serverData) {

        Log.d(TAG, "onStateConflict :: stateKey:" + stateKey + " , resolvedVersion:" + resolvedVersion + " localData:" + new String(localData) + " , serverData" + new String(serverData));
    }

    @Override
    public void onStateLoaded(int statusCode, int stateKey, byte[] localData) {
        Log.d(TAG, "onStateLoaded :: statusCode:" + statusCode + " , stateKey:" + stateKey + " localData:" + (localData != null ? new String(localData) : "null data"));

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        return super.onKeyDown(keyCode, e);
    }

    public InvitationsAdapter getInvitationsAdapter() {
        return invitationsAdapter;
    }

    public RoomsAdapter getRoomsAdapter() {
        return roomsAdapter;
    }

    @Override
    public void onSignInFailed() {
        Log.d(TAG, "Sign-in failed.");
        
    }

    @Override
    public void onSignInSucceeded() {
        Log.d(TAG, "Sign-in succeeded.");        
        Games.Invitations.registerInvitationListener(apiController.getApiClient(), this.roomController);
        this.loadPlayerRating();
    }

    @Override
    public void onNewGameRejected() {
        Log.d(TAG, "New game dialog canceled!");
    }

    @Override
    public void onNewGameCreated(String color, boolean isRated, int timeControll, int increment) {
        Log.d(TAG, "onNewGameCreated :: color :" + color + " , isRated :" + isRated + " , timeControll" + timeControll);

        // chessGameController.getRealTimeGameClient().sendChallange(1,
        // getTimeControllValue(timeControll / 1000), getIncrementValue(increment / 1000), 0,
        // isRated, color.equals("white"));
    }

    // *********************************************************************
    // *********************************************************************
    // Private section
    // *********************************************************************
    // *********************************************************************

    private int getTimeControllValue(int index) {

        String[] tcv = getResources().getStringArray(R.array.time_control_values);

        return Integer.parseInt(tcv[index]);
    }

    private int getIncrementValue(int index) {

        String[] tiv = getResources().getStringArray(R.array.time_increment_values);

        return Integer.parseInt(tiv[index]);
    }

    // Handle the result of the "Select players UI" we launched when the user
    // clicked the
    // "Invite friends" button. We react by creating a room with those players.
    private void handleSelectPlayersResult(int response, final Intent data) {
        if (response != Activity.RESULT_OK) {
            Log.w(TAG, "*** select players UI cancelled, " + response);            
            return;
        }

        Log.d(TAG, "Select players UI succeeded.");

        NewGameDialog d = new NewGameDialog();
        d.setListener(new NewGameDialogListener() {

            @Override
            public void onNewGameRejected() {
                Log.d(TAG, "Invitation is canceled!");               
            }

            @Override
            public void onNewGameCreated(String color, boolean isRated, int timeControll, int increment) {

                Log.d(TAG, "Details  :" + color + "," + isRated + "," + getTimeControllValue(timeControll) + "," + getIncrementValue(increment));
                int gameVariant = Util.getGameVariant(1, getTimeControllValue(timeControll) / 1000, getIncrementValue(increment) / 1000, 0, isRated, color.equals("white"));
                Log.d(TAG, "Game Variant :" + gameVariant);
                final ArrayList<String> invitees = data.getStringArrayListExtra(GamesClient.EXTRA_PLAYERS);
                Log.d(TAG, "Invitee count: " + invitees.size());
                Log.d(TAG, "Invitee: " + invitees.toString());
                chessGameController.createRoom(invitees.get(0), gameVariant);
            }
        });

        d.show(this.getSupportFragmentManager(), TAG);
    }

    public void setSelectedTab(int i) {
        this.viewPager.setCurrentItem(i);
    }

    private void loadPlayerRating() {
        // load top score

        PendingResult<Leaderboards.LoadPlayerScoreResult> topResult =
                        Games.Leaderboards.loadCurrentPlayerLeaderboardScore(apiController.getApiClient(), getResources().getString(R.string.leaderboard_top_rating),
                                        LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC);
        topResult.setResultCallback(new ResultCallback<Leaderboards.LoadPlayerScoreResult>() {

            @Override
            public void onResult(LoadPlayerScoreResult result) {
                Log.d(TAG, "onPlayerLeaderboardScoreLoaded :: top score :: statusCode" + result.getStatus() + ", , score :" + result.getScore());

                final ChessGamePlayer localPlayer = new ChessGamePlayer();

                if (result.getScore() == null) {
                    Log.d(TAG, "onPlayerLeaderboardScoreLoaded :: save initial top score");
                    Games.Leaderboards.submitScore(apiController.getApiClient(), getResources().getString(R.string.leaderboard_top_rating), Util.TOP_RATING_BASE);
                } else {
                    localPlayer.setTopScore(result.getScore().getRawScore());
                }


                PendingResult<Leaderboards.LoadPlayerScoreResult> lowResult =
                                Games.Leaderboards.loadCurrentPlayerLeaderboardScore(apiController.getApiClient(), getResources().getString(R.string.leaderboard_low_rating),
                                                LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC);
                lowResult.setResultCallback(new ResultCallback<Leaderboards.LoadPlayerScoreResult>() {

                    @Override
                    public void onResult(LoadPlayerScoreResult result) {
                        Log.d(TAG, "onPlayerLeaderboardScoreLoaded :: low score :: statusCode" + result.getStatus() + ", , score :" + result.getScore());


                        if (result.getScore() == null) {
                            Log.d(TAG, "onPlayerLeaderboardScoreLoaded :: save initial low score");
                            Games.Leaderboards.submitScore(apiController.getApiClient(), getResources().getString(R.string.leaderboard_low_rating), Util.LOW_RATING_BASE);
                        } else {
                            localPlayer.setLowScore(result.getScore().getRawScore());
                        }

                        chessGameController.setLocalPlayer(localPlayer);
                        localPlayer.setPlayer(Games.Players.getCurrentPlayer(apiController.getApiClient()));
                        updatePlayerStateView(localPlayer);
                    }
                });
            }
        });
    }

    private void updatePlayerStateView(final ChessGamePlayer player) { 
        this.playerName.setText(player.getPlayer().getDisplayName());
        this.playerRating.setText("Rating: " + Math.round(player.getRating()));
        ImageManager.create(getApplicationContext()).loadImage(playerAvatar, Games.Players.getCurrentPlayer(apiController.getApiClient()).getIconImageUri());        
    }

    private void installListeners() {
        outgoingFragment.setOnRoomCanceled(new Runnable() {

            @Override
            public void run() {
                ChessGameController.getController().leaveRoom(outgoingFragment.getSelectedRoom().getRoomId());
            }
        });

        incomingFragment.setOnInvitationAccepted(new Runnable() {

            @Override
            public void run() {
                chessGameController.joinRoom(incomingFragment.getSelectedInvitation().getInvitationId());
                incomingFragment.getInvitationsAdapter().removeInvitation(incomingFragment.getSelectedInvitation());
            }
        });

        incomingFragment.setOnInvitationRejectd(new Runnable() {

            @Override
            public void run() {
                System.out.println("decline "+incomingFragment.getSelectedInvitation().getInvitationId());
                Games.RealTimeMultiplayer.declineInvitation(apiController.getApiClient(), incomingFragment.getSelectedInvitation().getInvitationId());
                incomingFragment.getInvitationsAdapter().removeInvitation(incomingFragment.getSelectedInvitation());
            }
        });
    }

    private void handleQuickAction() {


    }

    private void handleInviteActiom() {
        Intent intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(apiController.getApiClient(), 1, 1, false);
        startActivityForResult(intent, RC_SELECT_PLAYERS);
    }

    private void handleLogoutAction() {
        apiController.diconnect();
        finish();
    }

    private void handleExitAction() {
        finish();
    }

    public void showGameError(String string, String string2) {
        // TODO Auto-generated method stub

    }
}
