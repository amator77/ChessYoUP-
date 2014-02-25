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

import com.chessyoup.R;
import com.chessyoup.game.GameHelper.GameHelperListener;
import com.chessyoup.game.Util;
import com.chessyoup.game.chess.ChessGameController;
import com.chessyoup.game.chess.ChessGamePlayer;
import com.chessyoup.ui.ctrl.RoomController;
import com.chessyoup.ui.dialogs.NewGameDialog;
import com.chessyoup.ui.dialogs.NewGameDialog.NewGameDialogListener;
import com.chessyoup.ui.fragment.FragmentAdapter;
import com.chessyoup.ui.fragment.IncomingInvitationsFragment;
import com.chessyoup.ui.fragment.InvitationsAdapter;
import com.chessyoup.ui.fragment.OutgoingInvitationFragment;
import com.chessyoup.ui.fragment.RoomsAdapter;
import com.google.android.gms.appstate.AppStateClient;
import com.google.android.gms.appstate.OnStateLoadedListener;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.OnPlayerLeaderboardScoreLoadedListener;
import com.google.android.gms.games.multiplayer.Invitation;

public class ChessYoUpActivity extends FragmentActivity implements View.OnClickListener,
		NewGameDialogListener, OnStateLoadedListener , GameHelperListener, OnPlayerLeaderboardScoreLoadedListener {

	private final static String TAG = "ChessYoUpActivity";
	private final static int RC_SELECT_PLAYERS = 10000;
	private final static int RC_INVITATION_INBOX = 10001;	
	
	public static final String ROOM_ID_EXTRA = "roomId";
	public static final String REMOTE_PLAYER_EXTRA = "remotePlayer";
	public static final String INVITATION_ID_EXTRA = "invitationId";
	public static final String IS_CHALANGER_EXTRA = "isChalanger";
	public static final String GAME_VARIANT_EXTRA = "gameVariant";
		
	private final static int[] SCREENS = { R.id.screen_main,
			R.id.screen_sign_in };

	private int mCurScreen = -1;
	
	 private FragmentAdapter adapter;
	
	private ViewPager viewPager;
	
	private ChessGameController chessGameController;
	
	private Invitation incomingInvitationId;
	
	private InvitationsAdapter invitationsAdapter;
	
	private RoomsAdapter roomsAdapter;
	
	private RoomController roomController;
	
	private OutgoingInvitationFragment outgoingFragment; 
	
	private IncomingInvitationsFragment incomingFragment; 
	
	// *********************************************************************
	// *********************************************************************
	// Activity methods
	// *********************************************************************
	// *********************************************************************

	@Override
	public void onCreate(Bundle savedInstanceState) {	    	    
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		findViewById(R.id.button_sign_in).setOnClickListener(this);
		roomController = new RoomController(this);
		chessGameController = ChessGameController.getController(); 
        ChessGameController.getController().initialize(this, this);
        ChessGameController.getController().setRoomStatusUpdateListener(roomController);
        ChessGameController.getController().setRoomUpdatelistener(roomController);				
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
        installListeners();               
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
                // TODO Auto-generated method stub
                
            }
        });
	    
	    incomingFragment.setOnInvitationRejectd(new Runnable() {
            
            @Override
            public void run() {
                ChessGameController.getController().getGamesClient().declineRoomInvitation(incomingFragment.getSelectedInvitation().getInvitationId());                
            }
        });
    }
	

    @Override
	public void onStart() {		
		super.onStart();
		chessGameController.start();
		switchToScreen(R.id.screen_main);
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
		chessGameController.stop();
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
	
	private void handleQuickAction() {
        // TODO Auto-generated method stub
        
    }

    private void handleInviteActiom() {        
        Intent intent = chessGameController.getGamesClient().getSelectPlayersIntent(1, 1);        
        startActivityForResult(intent, RC_SELECT_PLAYERS);
    }

    private void handleLogoutAction() {
        chessGameController.getGamesClient().signOut();
        switchToScreen(R.id.screen_sign_in);        
    }

    private void handleExitAction() {
        // TODO Auto-generated method stub
        
    }

    @Override
	public void onClick(View v) {		

		switch (v.getId()) {
		case R.id.button_sign_in:
		    chessGameController.beginUserInitiatedSignIn();
			break;		
		}
	}

	@Override
	public void onActivityResult(int requestCode, int responseCode,
			Intent intent) {
		super.onActivityResult(requestCode, responseCode, intent);		
		chessGameController.onActivityResult(requestCode, responseCode, intent);		
		
		switch (requestCode) {
		case RC_SELECT_PLAYERS:
			handleSelectPlayersResult(responseCode, intent);
			break;
		case RC_INVITATION_INBOX:
			handleInvitationInboxResult(responseCode, intent);
			break;
		}
	}
	
	@Override
	public void onStateConflict(int stateKey, String resolvedVersion,
			byte[] localData, byte[] serverData) {

		Log.d(TAG, "onStateConflict :: stateKey:" + stateKey
				+ " , resolvedVersion:" + resolvedVersion + " localData:"
				+ new String(localData) + " , serverData"
				+ new String(serverData));
	}

	@Override
	public void onStateLoaded(int statusCode, int stateKey, byte[] localData) {

		Log.d(TAG, "onStateLoaded :: statusCode:" + statusCode + " , stateKey:"
				+ stateKey + " localData:"
				+ (localData != null ? new String(localData) : "null data"));
		ChessGamePlayer localPlayer = null;

		
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent e) {
//		if (keyCode == KeyEvent.KEYCODE_BACK && mCurScreen == R.id.screen_game) {
//			leaveRoom();
//			return true;
//		}
		return super.onKeyDown(keyCode, e);
	}
	
	
	
	// *********************************************************************
	// *********************************************************************
	// GameHelperListener methods
	// *********************************************************************
	// *********************************************************************

	public InvitationsAdapter getInvitationsAdapter() {
        return invitationsAdapter;
    }

    public RoomsAdapter getRoomsAdapter() {
        return roomsAdapter;
    }

    @Override
	public void onSignInFailed() {
		Log.d(TAG, "Sign-in failed.");
		switchToScreen(R.id.screen_sign_in);
	}

	@Override
	public void onSignInSucceeded() {
		Log.d(TAG, "Sign-in succeeded.");

		switchToMainScreen();
					
		chessGameController.getGamesClient().loadCurrentPlayerLeaderboardScore(this, getResources().getString(R.string.leaderboard_rating_id) , LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC);
		chessGameController.getGamesClient().registerInvitationListener(this.roomController);
//      chessGameController.getAppStateClient().loadState(this, 0);
		
//		Invitation invitation = chessGameController.getInvitation();
//		
//		if (invitation != null) {
//			acceptInviteToRoom(invitation);
//			return;
//		}
		
		
	}

	// *********************************************************************
	// *********************************************************************
	// RealTimeChessGameListener methods
	// *********************************************************************
	// *********************************************************************

//	@Override
//	public void onChallangeRecevied(GameVariant gameVariant, String whiteId,
//			String blackId) {
//
//		showNewGameRequestDialog(whiteId, blackId, gameVariant.isRated(),
//				gameVariant.getTime(), gameVariant.getIncrement());
//	}

//	@Override
//	public void onStartRecevied() {
//		if (gameState.getGameVariant() != null) {
//			startGame();
//		} else {
//			Log.d(TAG,
//					"Invalid game state! There is no previous start request!");
//			Log.d(TAG, gameState.toString());
//		}
//	}
//
//	@Override
//	public void onReadyRecevied(double remoteRating, double remoteRD,
//			double volatility) {
//		gameState.setRemoteRating(remoteRating, remoteRD, volatility);
//
//		dismissWaitingRoom();
//		displayShortMessage("Ready to play!");
//		Log.d(TAG, "onReadyRecevied :: " + gameState.toString());
//
//		if (!gameState.isReady()) {
//			gameState.setReady(true);
//			realTimeChessGame.ready();
//		}
//
//		this.startGame();
//	}
//
//	@Override
//	public void onMoveRecevied(String move, int thinkingTime) {
//		this.chessTableUI.getCtrl().makeRemoteMove(move);
//	}
//
//	@Override
//	public void onResignRecevied() {
//		Log.d(TAG, "Remote resigned!");
//
//		if (gameState.getWhitePlayerId().equals(gameState.getRemoteId())) {
//			this.chessTableUI.getCtrl().resignGameForWhite();
//		} else {
//			this.chessTableUI.getCtrl().resignGameForBlack();
//		}
//
//		displayShortMessage(gameState.getRemoteDisplayName() + " resigned!");
//	}
//
//	@Override
//	public void onDrawRecevied() {
//		Log.d(TAG, "Draw requested!");
//
//		if (this.chessTableUI.getCtrl().isDrawRequested()) {
//			this.chessTableUI.getCtrl().drawGame();
//			displayShortMessage("Draw by mutual agreement!");
//		} else {
//			this.chessTableUI.getCtrl().setDrawRequested(true);
//			displayShortMessage(gameState.getRemoteDisplayName()
//					+ " is requesting draw!");
//		}
//	}
//
//	@Override
//	public void onFlagRecevied() {
//		Log.d(TAG, "Remote flaged!");
//		this.chessTableUI.getCtrl().resignGame();
//		displayShortMessage(gameState.getRemoteDisplayName()
//				+ " is out of time!!!");
//	}
//
//	@Override
//	public void onRematchRecevied() {
//		if (!gameState.isLocalPlayerRoomOwner()) {
//			displayShortMessage(gameState.getRemoteDisplayName()
//					+ " is requesting rematch!");
//		}
//	}
//
//	@Override
//	public void onAbortRecevied() {
//		Log.d(TAG, "Abort requested!");
//
//		if (this.chessTableUI.getCtrl().isAbortRequested()) {
//			this.chessTableUI.getCtrl().abortGame();
//			displayShortMessage("Game aborted!");
//		} else {
//			this.chessTableUI.getCtrl().setAbortRequested(true);
//			displayShortMessage(gameState.getRemoteDisplayName()
//					+ " is requesting to abort the game!");
//		}
//	}
//
//	@Override
//	public void onException(String message) {
//		displayShortMessage(message);
//	}

	// *********************************************************************
	// *********************************************************************
	// ChessTableUIListener methods
	// *********************************************************************
	// *********************************************************************

//	@Override
//	public void onChat(String chatMessage) {
//		realTimeChessGame.sendChatMessage(chatMessage);
//	}
//
//	@Override
//	public void onMove(String move) {
//		this.realTimeChessGame.move(move, 0);
//	}
//
//	@Override
//	public void onDrawRequest() {
//		this.realTimeChessGame.draw();
//	}
//
//	@Override
//	public void onResign() {
//
//		if (gameState.getWhitePlayerId().equals(
//				gameState.getRoom().getCreatorId())) {
//			this.chessTableUI.getCtrl().resignGameForWhite();
//		} else {
//			this.chessTableUI.getCtrl().resignGameForBlack();
//		}
//
//		this.realTimeChessGame.resign();
//	}
//
//	@Override
//	public void onFlag() {
//		this.realTimeChessGame.flag();
//	}
//
//	@Override
//	public void onAbortRequest() {
//		this.realTimeChessGame.abort();
//	}
//
//	@Override
//	public void onRematchRequest() {
//
//		if (gameState.isLocalPlayerRoomOwner()) {
//			showNewGameDialog();
//		} else {
//			this.realTimeChessGame.rematch();
//		}
//	}
//
//	@Override
//	public void onChatReceived(String message) {
//		this.chessTableUI.appendChatMesssage(message);
//	}
//
//	@Override
//	public void onTableExit() {
//		leaveRoom();
//	}
//
//	@Override
//	public void onGameFinished(Game g) {
//		this.handleGameFinished(g, gameState.getWhitePlayerId(),
//				gameState.getBlackPlayerId());
//	}

	// *********************************************************************
	// *********************************************************************
	// NewGameDialogListener
	// *********************************************************************
	// *********************************************************************

	@Override
	public void onNewGameRejected() {
		Log.d(TAG, "New game dialog canceled!");
	}

	@Override
	public void onNewGameCreated(String color, boolean isRated,
			int timeControll, int increment) {
		Log.d(TAG, "onNewGameCreated :: color :" + color + " , isRated :"
				+ isRated + " , timeControll" + timeControll);
		
		chessGameController.getRealTimeGameClient().sendChallange(1,
				getTimeControllValue(timeControll / 1000),
				getIncrementValue(increment / 1000), 0, isRated,
				color.equals("white"));
	}

	// *********************************************************************
	// *********************************************************************
	// Private section
	// *********************************************************************
	// *********************************************************************

	private int getTimeControllValue(int index) {

		String[] tcv = getResources().getStringArray(
				R.array.time_control_values);

		return Integer.parseInt(tcv[index]);
	}

	private int getIncrementValue(int index) {

		String[] tiv = getResources().getStringArray(
				R.array.time_increment_values);

		return Integer.parseInt(tiv[index]);
	}

	// Handle the result of the "Select players UI" we launched when the user
	// clicked the
	// "Invite friends" button. We react by creating a room with those players.
	private void handleSelectPlayersResult(int response, final Intent data) {
		if (response != Activity.RESULT_OK) {
			Log.w(TAG, "*** select players UI cancelled, " + response);
			switchToMainScreen();
			return;
		}

		Log.d(TAG, "Select players UI succeeded.");

		NewGameDialog d = new NewGameDialog();
		d.setListener(new NewGameDialogListener() {

			@Override
			public void onNewGameRejected() {
				Log.d(TAG, "Invitation is canceled!");
				switchToMainScreen();
			}

			@Override
			public void onNewGameCreated(String color, boolean isRated,
					int timeControll, int increment) {

				Log.d(TAG, "Details  :" + color + "," + isRated + ","
						+ getTimeControllValue(timeControll) + ","
						+ getIncrementValue(increment));
				int gameVariant = Util.getGameVariant(1,
						getTimeControllValue(timeControll) / 1000,
						getIncrementValue(increment) / 1000, 0, isRated,
						color.equals("white"));
				Log.d(TAG, "Game Variant :" + gameVariant);
				final ArrayList<String> invitees = data
						.getStringArrayListExtra(GamesClient.EXTRA_PLAYERS);
				Log.d(TAG, "Invitee count: " + invitees.size());
				Log.d(TAG, "Invitee: " + invitees.toString());
				
				ChessGameController.getController().createRoom(invitees.get(0), gameVariant);
//				
//				Intent chessRoomUIIntent = new Intent(ChessYoUpActivity.this,
//						ChessOnlinePlayGameUI.class);
//				chessRoomUIIntent.putExtra(REMOTE_PLAYER_EXTRA, invitees.get(0));
//				chessRoomUIIntent.putExtra(GAME_VARIANT_EXTRA, gameVariant);
//				chessRoomUIIntent.putExtra(IS_CHALANGER_EXTRA, true);
//				startActivity(chessRoomUIIntent);				
			}
		});

		d.show(this.getSupportFragmentManager(), TAG);
	}

	// Handle the result of the invitation inbox UI, where the player can pick
	// an invitation
	// to accept. We react by accepting the selected invitation, if any.
	private void handleInvitationInboxResult(int response, Intent data) {
		if (response != Activity.RESULT_OK) {
			Log.w(TAG, "*** invitation inbox UI cancelled, " + response);
			switchToMainScreen();
			return;
		}

		Log.d(TAG, "Invitation inbox UI succeeded.");
		Invitation inv = data.getExtras().getParcelable(
				GamesClient.EXTRA_INVITATION);

		// accept invitation
		acceptInviteToRoom(inv);
	}

	private void acceptInviteToRoom(Invitation inv) {
		Log.d(TAG, "Accepting invitation: " + inv);
		
		Intent chessRoomUIIntent = new Intent(ChessYoUpActivity.this,
				ChessOnlinePlayGameUI.class);
		chessRoomUIIntent.putExtra(REMOTE_PLAYER_EXTRA, inv.getInviter().getParticipantId());
		chessRoomUIIntent.putExtra(GAME_VARIANT_EXTRA, inv.getVariant());
		chessRoomUIIntent.putExtra(INVITATION_ID_EXTRA, inv.getInvitationId());		
		chessRoomUIIntent.putExtra(IS_CHALANGER_EXTRA, false);
		startActivity(chessRoomUIIntent);
	}

	private void switchToScreen(int screenId) {
		for (int id : SCREENS) {
			findViewById(id).setVisibility(
					screenId == id ? View.VISIBLE : View.GONE);
		}
		mCurScreen = screenId;

		boolean showInvPopup;
		if ( this.incomingInvitationId == null ) {
			showInvPopup = false;
		} else {
			showInvPopup = (mCurScreen == R.id.screen_main);
		}

		
	}

	private void switchToMainScreen() {
		switchToScreen(chessGameController.isSignedIn() ? R.id.screen_main : R.id.screen_sign_in);
	}


	private void showNewGameDialog() {
		NewGameDialog d = new NewGameDialog();
		d.setListener(this);
		d.show(this.getSupportFragmentManager(), TAG);
	}

    public void setSelectedTab(int i) {
        this.viewPager.setCurrentItem(i);        
    }

    @Override
    public void onPlayerLeaderboardScoreLoaded(int statusCode, LeaderboardScore score) {
        Log.d(TAG, "onPlayerLeaderboardScoreLoaded :: statusCode"+statusCode+", , score :"+score);
        
        if( statusCode == GamesClient.STATUS_OK ){
            int rating = 1500;
            double rd = 150;
            double volatility = 0;
            long rank=1;
            
            ChessGamePlayer localPlayer = new ChessGamePlayer();
            
            if( score == null ){ 
                chessGameController.getGamesClient().submitScore(getResources().getString(R.string.leaderboard_rating_id), rating,   rd+"-"+volatility);
            }
            else{
                rating = (int)score.getRawScore();
                String tag[] = score.getScoreTag().split("-");
                rd = Double.parseDouble(tag[0]);
                volatility = Double.parseDouble(tag[1]);
                rank = score.getRank();                
            }
            
            localPlayer.setRating(rating);
            localPlayer.setRatingDeviation(rd);
            localPlayer.setVolatility(volatility);
            localPlayer.setPlayer(chessGameController.getGamesClient().getCurrentPlayer());
            localPlayer.setRank(rank);
            chessGameController.setLocalPlayer(localPlayer);                        
            this.updatePlayerStateView(localPlayer);            
        }
    }
    
    private void updatePlayerStateView(ChessGamePlayer player ) {
        System.out.println("aici"+player.getPlayer().getDisplayName());
        ((TextView)findViewById(R.id.playerName)).setText(player.getPlayer().getDisplayName());
        ((TextView) findViewById(R.id.playerRating)).setText( "Rating: " +
        Math.round(player.getRating())+" , Rank: "+player.getRank());
        ImageManager.create(this.getApplicationContext()).loadImage((ImageView)findViewById(R.id.playerAvatar),chessGameController.getGamesClient().getCurrentPlayer().getIconImageUri());
   }
}
