package com.chessyoup.ui;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.goochjs.glicko2.Rating;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.chessyoup.R;
import com.chessyoup.chessboard.ChessboardMode;
import com.chessyoup.game.GameState;
import com.chessyoup.game.GameVariant;
import com.chessyoup.game.PlayerState;
import com.chessyoup.game.RealTimeChessGame;
import com.chessyoup.game.RealTimeChessGame.RealTimeChessGameListener;
import com.chessyoup.game.Util;
import com.chessyoup.model.Game;
import com.chessyoup.ui.ChessTableUI.ChessTableUIListener;
import com.chessyoup.ui.fragment.GameRequestDialog;
import com.chessyoup.ui.fragment.GameRequestDialog.GameRequestDialogListener;
import com.chessyoup.ui.fragment.NewGameDialog;
import com.chessyoup.ui.fragment.NewGameDialog.NewGameDialogListener;
import com.chessyoup.ui.util.DownloadImageTask;
import com.google.android.gms.appstate.AppStateClient;
import com.google.android.gms.appstate.OnStateLoadedListener;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.example.games.basegameutils.BaseGameActivity;

public class ChessYoUpActivity extends BaseGameActivity implements
		View.OnClickListener, RoomStatusUpdateListener, RoomUpdateListener,
		OnInvitationReceivedListener, ChessTableUIListener,
		NewGameDialogListener, OnStateLoadedListener, RealTimeChessGameListener {

	private final static String TAG = "ChessYoUpActivity";
	private final static int RC_SELECT_PLAYERS = 10000;
	private final static int RC_INVITATION_INBOX = 10001;
	private final static int RC_WAITING_ROOM = 10002;

	private ChessTableUI chessTableUI;

	private GameState gameState;

	private RealTimeChessGame realTimeChessGame;

	private final static int[] CLICKABLES = {
			R.id.button_accept_popup_invitation, R.id.button_invite_players,
			R.id.button_see_invitations, R.id.button_sign_in,
			R.id.button_sign_out, };

	private final static int[] SCREENS = { R.id.screen_game, R.id.screen_main,
			R.id.screen_sign_in, R.id.screen_wait };

	private int mCurScreen = -1;

	public ChessYoUpActivity() {
		super(BaseGameActivity.CLIENT_APPSTATE | BaseGameActivity.CLIENT_GAMES);
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
		PlayerState playerState = null;

		switch (statusCode) {
		case AppStateClient.STATUS_OK:
			Log.d(TAG, "onStateLoaded :: statusCode:STATUS_OK");

			if (localData == null) {
				playerState = new PlayerState(getGamesClient()
						.getCurrentPlayerId());
				getAppStateClient().updateState(0,
						playerState.toJSON().getBytes());
			} else {
				playerState = new PlayerState(getGamesClient()
						.getCurrentPlayerId());
				playerState.updateFromJSON(new String(localData));
			}

			this.gameState = new GameState(playerState);
			this.realTimeChessGame = new RealTimeChessGame(getGamesClient(),
					gameState);
			this.realTimeChessGame.setListener(this);
			this.updatePlayerStateView();

			break;
		case AppStateClient.STATUS_STATE_KEY_NOT_FOUND:
			Log.d(TAG, "onStateLoaded :: statusCode:STATUS_STATE_KEY_NOT_FOUND");
			playerState = new PlayerState(getGamesClient().getCurrentPlayerId());
			getAppStateClient().updateState(0, playerState.toJSON().getBytes());
			this.gameState = new GameState(playerState);
			this.updatePlayerStateView();
			break;
		default:
			break;
		}
	}

	// *********************************************************************
	// *********************************************************************
	// Activity methods
	// *********************************************************************
	// *********************************************************************

	@Override
	public void onCreate(Bundle savedInstanceState) {
		enableDebugLog(true, TAG);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.chessTableUI = new ChessTableUI(this);
		this.chessTableUI.setChessTableUIListener(this);
		for (int id : CLICKABLES) {
			findViewById(id).setOnClickListener(this);
		}
	}

	@Override
	public void onClick(View v) {
		Intent intent;

		switch (v.getId()) {
		case R.id.button_sign_in:
			beginUserInitiatedSignIn();
			break;
		case R.id.button_sign_out:
			signOut();
			switchToScreen(R.id.screen_sign_in);
			break;
		case R.id.button_invite_players:
			intent = getGamesClient().getSelectPlayersIntent(1, 3);
			switchToScreen(R.id.screen_wait);
			startActivityForResult(intent, RC_SELECT_PLAYERS);
			break;
		case R.id.button_see_invitations:
			intent = getGamesClient().getInvitationInboxIntent();
			switchToScreen(R.id.screen_wait);
			startActivityForResult(intent, RC_INVITATION_INBOX);
			break;
		case R.id.button_accept_popup_invitation:
			acceptInviteToRoom(gameState.getIncomingInvitationId());			
			break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int responseCode,
			Intent intent) {
		super.onActivityResult(requestCode, responseCode, intent);

		switch (requestCode) {
		case RC_SELECT_PLAYERS:
			handleSelectPlayersResult(responseCode, intent);
			break;
		case RC_INVITATION_INBOX:
			handleInvitationInboxResult(responseCode, intent);
			break;
		case RC_WAITING_ROOM:
			if (gameState.isWaitRoomDismissedFromCode()) {
				switchToScreen(R.id.screen_game);
				break;
			}

			if (responseCode == Activity.RESULT_OK) {
				Log.d(TAG, "Sending ready request to remote.");
				switchToScreen(R.id.screen_game);

				if (gameState.isLocalPlayerRoomOwner()) {
					
					if( !gameState.isReady()){
						realTimeChessGame.ready();
						gameState.setReady(true);						
					}
				}				
				
			} else if (responseCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
				leaveRoom();
			} else if (responseCode == Activity.RESULT_CANCELED) {
				leaveRoom();
			}

			break;
		}
	}
	
	@Override
	public void onStop() {
		Log.d(TAG, "**** got onStop");
		leaveRoom();
		stopKeepingScreenOn();
		switchToScreen(R.id.screen_wait);
		super.onStop();
	}
	
	@Override
	public void onStart() {
		switchToScreen(R.id.screen_wait);
		super.onStart();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent e) {
		if (keyCode == KeyEvent.KEYCODE_BACK && mCurScreen == R.id.screen_game) {
			leaveRoom();
			return true;
		}
		return super.onKeyDown(keyCode, e);
	}

	// *********************************************************************
	// *********************************************************************
	// GameHelperListener methods
	// *********************************************************************
	// *********************************************************************

	@Override
	public void onSignInFailed() {
		Log.d(TAG, "Sign-in failed.");
		switchToScreen(R.id.screen_sign_in);
	}

	@Override
	public void onSignInSucceeded() {
		Log.d(TAG, "Sign-in succeeded.");

		getAppStateClient().loadState(this, 0);

		getGamesClient().registerInvitationListener(this);

		if (getInvitationId() != null) {
			acceptInviteToRoom(getInvitationId());
			return;
		}

		switchToMainScreen();
	}

	// *********************************************************************
	// OnInvitationReceivedListener methods
	// *********************************************************************

	@Override
	public void onInvitationReceived(Invitation invitation) {
		Log.d(TAG,"onInvitationReceived :: "+invitation.toString());
		gameState.setIncomingInvitationId(invitation.getInvitationId());		
		((TextView) findViewById(R.id.incoming_invitation_text))
				.setText(getInviationDisplayInfo(invitation));
		switchToScreen(mCurScreen); // This will show the invitation popup
	}
	
	private String getInviationDisplayInfo(Invitation invitation){
		StringBuffer sb = new StringBuffer();
		GameVariant gv = Util.getGameVariant(invitation.getVariant());
		
		sb.append(invitation.getInviter().getDisplayName()).append(" ").append(getString(R.string.is_inviting_you));
		sb.append(" to play an ").append(gv.getTime()).append("'").append(gv.getIncrement()).append("'' game!");
		
		return sb.toString();
	}
	
	// *********************************************************************
	// *********************************************************************
	// RoomStatusUpdateListener methods
	// *********************************************************************
	// *********************************************************************

	@Override
	public void onConnectedToRoom(Room room) {
		Log.d(TAG, "onConnectedToRoom.");
		
		System.out.println(getGamesClient().getCurrentPlayerId());
		System.out.println(room.getCreatorId());
		
		gameState.setMyId(room.getParticipantId(getGamesClient()
				.getCurrentPlayerId()));
		
		gameState.setRoom(room);
		
		for (Participant p : room.getParticipants()) {
			if (!p.getParticipantId().equals(gameState.getMyId())) {				
				gameState.setRemoteId(p.getParticipantId());			
				break;
			}
		}
		
		gameState.setGameVariant(Util.getGameVariant(room.getVariant()), gameState.getIncomingInvitationId() == null );
		
		Log.d(TAG, gameState.toString());
		Log.d(TAG, "<< CONNECTED TO ROOM>>");
	}

	@Override
	public void onDisconnectedFromRoom(Room room) {
		gameState.reset();
		showGameError();
	}

	@Override
	public void onPeerDeclined(Room room, List<String> arg1) {
		updateRoom(room);
	}

	@Override
	public void onPeerInvitedToRoom(Room room, List<String> arg1) {
		updateRoom(room);
	}

	@Override
	public void onPeerJoined(Room room, List<String> arg1) {
		updateRoom(room);
	}

	@Override
	public void onPeerLeft(Room room, List<String> peersWhoLeft) {
		updateRoom(room);
	}

	@Override
	public void onRoomAutoMatching(Room room) {
		updateRoom(room);
	}

	@Override
	public void onRoomConnecting(Room room) {
		updateRoom(room);
	}

	@Override
	public void onPeersConnected(Room room, List<String> peers) {
		updateRoom(room);
	}

	@Override
	public void onPeersDisconnected(Room room, List<String> peers) {
		updateRoom(room);
	}

	@Override
	public void onP2PConnected(String participantId) {
	}

	@Override
	public void onP2PDisconnected(String participantId) {
	}

	// *********************************************************************
	// *********************************************************************
	// RoomUpdateListener methods
	// *********************************************************************
	// *********************************************************************

	@Override
	public void onRoomCreated(int statusCode, Room room) {
		Log.d(TAG, "onRoomCreated(" + statusCode + ", " + room + ")");
		if (statusCode != GamesClient.STATUS_OK) {
			Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);
			showGameError();
			return;
		}

		// show the waiting room UI
		showWaitingRoom(room);
	}

	@Override
	public void onRoomConnected(int statusCode, Room room) {
		Log.d(TAG, "onRoomConnected(" + statusCode + ", " + room + ")");
		if (statusCode != GamesClient.STATUS_OK) {
			Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
			showGameError();
			return;
		}
		updateRoom(room);
	}

	@Override
	public void onJoinedRoom(int statusCode, Room room) {
		Log.d(TAG, "onJoinedRoom(" + statusCode + ", " + room + ")");
		if (statusCode != GamesClient.STATUS_OK) {
			Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
			showGameError();
			return;
		}

		// show the waiting room UI
		showWaitingRoom(room);
	}

	@Override
	public void onLeftRoom(int statusCode, String roomId) {
		Log.d(TAG, "onLeftRoom, code " + statusCode);
		switchToMainScreen();
	}

	// *********************************************************************
	// *********************************************************************
	// RealTimeChessGameListener methods
	// *********************************************************************
	// *********************************************************************
	
	@Override	
	public void onChallangeRecevied(GameVariant gameVariant,String whiteId,String blackId) {

		showNewGameRequestDialog(whiteId, blackId, gameVariant.isRated(), gameVariant.getTime(),
				gameVariant.getIncrement());
	}

	@Override
	public void onStartRecevied() {
		if (gameState.getGameVariant() != null) {			
			startGame();
		} else {
			Log.d(TAG,
					"Invalid game state! There is no previous start request!");
			Log.d(TAG, gameState.toString());
		}
	}

	@Override
	public void onReadyRecevied(double remoteRating, double remoteRD,double volatility) {
		gameState.setRemoteRating(remoteRating,remoteRD,volatility);
		
		dismissWaitingRoom();		
		displayShortMessage("Ready to play!");
		Log.d(TAG,"onReadyRecevied :: "+gameState.toString());
		
		if( !gameState.isReady() ){
			gameState.setReady(true);
			realTimeChessGame.ready();
		}
		
		this.startGame();			
	}

	@Override
	public void onMoveRecevied(String move, int thinkingTime) {
		this.chessTableUI.getCtrl().makeRemoteMove(move);
	}

	@Override
	public void onResignRecevied() {
		Log.d(TAG, "Remote resigned!");
		
		if( gameState.getWhitePlayerId().equals(gameState.getRemoteId())){
			this.chessTableUI.getCtrl().resignGameForWhite();
		}
		else{
			this.chessTableUI.getCtrl().resignGameForBlack();
		}
						
		displayShortMessage(gameState.getRemoteDisplayName() + " resigned!");
	}

	@Override
	public void onDrawRecevied() {
		Log.d(TAG, "Draw requested!");

		if (this.chessTableUI.getCtrl().isDrawRequested()) {
			this.chessTableUI.getCtrl().drawGame();
			displayShortMessage("Draw by mutual agreement!");
		} else {
			this.chessTableUI.getCtrl().setDrawRequested(true);
			displayShortMessage(gameState.getRemoteDisplayName()
					+ " is requesting draw!");
		}
	}

	@Override
	public void onFlagRecevied() {
		Log.d(TAG, "Remote flaged!");
		this.chessTableUI.getCtrl().resignGame();
		displayShortMessage(gameState.getRemoteDisplayName()
				+ " is out of time!!!");
	}

	@Override
	public void onRematchRecevied() {
		if (!gameState.isLocalPlayerRoomOwner()) {
			displayShortMessage(gameState.getRemoteDisplayName()
					+ " is requesting rematch!");
		}
	}

	@Override
	public void onAbortRecevied() {
		Log.d(TAG, "Abort requested!");

		if (this.chessTableUI.getCtrl().isAbortRequested()) {
			this.chessTableUI.getCtrl().abortGame();
			displayShortMessage("Game aborted!");
		} else {
			this.chessTableUI.getCtrl().setAbortRequested(true);
			displayShortMessage(gameState.getRemoteDisplayName()
					+ " is requesting to abort the game!");
		}
	}

	@Override
	public void onException(String message) {
		displayShortMessage(message);
	}

	// *********************************************************************
	// *********************************************************************
	// ChessTableUIListener methods
	// *********************************************************************
	// *********************************************************************

	@Override
	public void onChat(String chatMessage) {
		realTimeChessGame.sendChatMessage(chatMessage);
	}

	@Override
	public void onMove(String move) {
		this.realTimeChessGame.move(move, 0);
	}

	@Override
	public void onDrawRequest() {
		this.realTimeChessGame.draw();
	}

	@Override
	public void onResign() {
				
		if(gameState.getWhitePlayerId().equals(gameState.getMyId())){
			this.chessTableUI.getCtrl().resignGameForWhite();
		}
		else{
			this.chessTableUI.getCtrl().resignGameForBlack();
		}
		
		this.realTimeChessGame.resign();
	}

	@Override
	public void onFlag() {
		this.realTimeChessGame.flag();
	}

	@Override
	public void onAbortRequest() {
		this.realTimeChessGame.abort();
	}

	@Override
	public void onRematchRequest() {

		if (gameState.isLocalPlayerRoomOwner()) {
			showNewGameDialog();
		} else {
			this.realTimeChessGame.rematch();
		}
	}

	@Override
	public void onChatReceived(String message) {
		this.chessTableUI.appendChatMesssage(message);
	}

	@Override
	public void onTableExit() {
		leaveRoom();
	}
	
	@Override
	public void onGameFinished(Game g) {
		this.handleGameFinished(g, gameState.getWhitePlayerId(),gameState.getBlackPlayerId());		
	}

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
		
		realTimeChessGame.sendChallange(1, getTimeControllValue(timeControll/1000),  getIncrementValue(increment/1000), 0, isRated, color.equals("white"));		
	}

	// *********************************************************************
	// *********************************************************************
	// Private section
	// *********************************************************************
	// *********************************************************************
	
	private void handleGameFinished(Game game,String whitePlayerId, String blackPlayerId){
		Log.d(TAG, "handleGameFinished :: game state :"+game.getGameState()+" , wp :"+whitePlayerId+",bp:"+blackPlayerId);
		
		if( game.isRated()){
			switch (game.getGameState()) {
			case ABORTED:
				updateLocalPlayerLevel();
				break;
			case BLACK_MATE:
				updateRatingOnResult(blackPlayerId, whitePlayerId);
				break;
			case WHITE_MATE:				
				updateRatingOnResult(whitePlayerId,blackPlayerId);
				break;
			case DRAW_50:		
				updateRatingOnDraw(whitePlayerId,blackPlayerId);
				break;
			case DRAW_AGREE:				
				updateRatingOnDraw(whitePlayerId,blackPlayerId);
				break;
			case DRAW_NO_MATE:				
				updateRatingOnDraw(whitePlayerId,blackPlayerId);
				break;
			case DRAW_REP:				
				updateRatingOnDraw(whitePlayerId,blackPlayerId);
				break;
			case WHITE_STALEMATE:				
				updateRatingOnDraw(whitePlayerId,blackPlayerId);
				break;
			case BLACK_STALEMATE:				
				updateRatingOnDraw(whitePlayerId,blackPlayerId);
				break;
			case RESIGN_WHITE:	
				Log.d(TAG, "RESIGN_WHITE");
				updateRatingOnResult(blackPlayerId, whitePlayerId);
				break;
			case RESIGN_BLACK:
				Log.d(TAG, "RESIGN_BLACK");
				updateRatingOnResult(whitePlayerId,blackPlayerId);
				break;
			case ALIVE:				
				Log.d(TAG, "Game not finished!");
				break;
			default:
				Log.d(TAG, "Game not finished!");
				break;
			}
		}
		else{
			Log.d(TAG, "Frendly game.Increment only level");
			updateLocalPlayerLevel();			
		}
	}
	
	private void updateRatingOnResult(String winerId, String loserId) {		
		Rating winerRating = winerId.equals(gameState.getMyId()) ? gameState.getOwnerRating() : gameState.getRemoteRating();
		Rating loserRating = loserId.equals(gameState.getMyId()) ? gameState.getOwnerRating() : gameState.getRemoteRating();
		
		Log.d(TAG, "Initial ratings :winer "+winerRating.toString());
		Log.d(TAG, "Initial ratings :loserRating "+loserRating.toString());
		
		Util.computeRatingOnResult(winerRating, loserRating);
		
		Log.d(TAG, "Updated ratings :winer "+winerRating.toString());
		Log.d(TAG, "Updated ratings :loserRating "+loserRating.toString());				
		
		if(winerRating.getUid().equals(gameState.getMyId())){
			gameState.getOwner().setRating(winerRating.getRating());
			gameState.getOwner().setRatingDeviation(winerRating.getRatingDeviation());
			gameState.getOwner().setVolatility(winerRating.getVolatility());
			gameState.setRemoteRating(loserRating.getRating(), loserRating.getRatingDeviation(), loserRating.getVolatility());
			gameState.getOwner().setWins(gameState.getOwner().getWins()+1);
		}
		else{
			gameState.getOwner().setRating(loserRating.getRating());
			gameState.getOwner().setRatingDeviation(loserRating.getRatingDeviation());
			gameState.getOwner().setVolatility(loserRating.getVolatility());
			gameState.setRemoteRating(winerRating.getRating(), winerRating.getRatingDeviation(), winerRating.getVolatility());
			gameState.getOwner().setLoses(gameState.getOwner().getLoses()+1);
		}
						
		updatePlayerStateView();				
		getAppStateClient().updateState(0, gameState.getOwner().toJSON().getBytes());
	}
	
	private void updateRatingOnDraw(String whitePlayerId, String blackPlayerId) {
		
	}
	
	private void updateLocalPlayerLevel() {
		// TODO Auto-generated method stub
		
	}

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
				Log.d(TAG, "Details  :"+color+","+isRated+","+getTimeControllValue(timeControll)+","+getIncrementValue(increment));
				int gameVariant = Util.getGameVariant(1, getTimeControllValue(timeControll)/1000,getIncrementValue(increment)/1000, 0, isRated, color.equals("white"));
				Log.d(TAG, "Game Variant :"+gameVariant);				
				final ArrayList<String> invitees = data.getStringArrayListExtra(GamesClient.EXTRA_PLAYERS);
				Log.d(TAG, "Invitee count: " + invitees.size());
				Log.d(TAG, "Invitee: " + invitees.toString()); 
				Log.d(TAG, "Creating room...");				
				RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(ChessYoUpActivity.this);
				rtmConfigBuilder.addPlayersToInvite(invitees);
				rtmConfigBuilder.setVariant(gameVariant);
				rtmConfigBuilder.setMessageReceivedListener(ChessYoUpActivity.this.realTimeChessGame);
				rtmConfigBuilder.setRoomStatusUpdateListener(ChessYoUpActivity.this);
				switchToScreen(R.id.screen_wait);
				keepScreenOn();
				getGamesClient().createRoom(rtmConfigBuilder.build());					
				Log.d(TAG, "Room created, waiting for it to be ready...");				
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
		acceptInviteToRoom(inv.getInvitationId());
	}

	private void acceptInviteToRoom(String invId) {
		Log.d(TAG, "Accepting invitation: " + invId);
		RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this);
		roomConfigBuilder.setInvitationIdToAccept(invId)
				.setMessageReceivedListener(this.realTimeChessGame)
				.setRoomStatusUpdateListener(this);
		switchToScreen(R.id.screen_wait);
		keepScreenOn();
		getGamesClient().joinRoom(roomConfigBuilder.build());
	}

	private void showGameError() {
		showAlert(getString(R.string.error), getString(R.string.game_problem));
		switchToMainScreen();
	}

	private void startGame() {		
		System.out.println( "isLocalOwner :"+gameState.isLocalPlayerRoomOwner() +" ,wp :"+gameState.getWhitePlayerId()+" ,bp "+gameState.getBlackPlayerId() +" , myd "+gameState.getMyId() );
		
		this.chessTableUI.getCtrl().setTimeLimit(gameState.getGameVariant().getTime()*1000, 0, gameState.getGameVariant().getIncrement()*1000);
		this.chessTableUI.getCtrl().newGame(
				getChessboardMode(gameState.getWhitePlayerId(), gameState.getBlackPlayerId()),gameState.getGameVariant().isRated());
		this.chessTableUI.getCtrl().startGame();		
		this.chessTableUI.flipBoard(!gameState.getMyId().equals(gameState.getWhitePlayerId()));		
		displayShortMessage("Game started!");
		this.gameState.setStarted(true);
	}

	private ChessboardMode getChessboardMode(String whitePlayerId,
			String blackPlayerId) {
		if (gameState.getMyId().equals(whitePlayerId)) {
			return new ChessboardMode(ChessboardMode.TWO_PLAYERS_BLACK_REMOTE);
		} else {
			return new ChessboardMode(ChessboardMode.TWO_PLAYERS_WHITE_REMOTE);
		}
	}

	// Leave the room.
	private void leaveRoom() {
		Log.d(TAG, "Leaving room.");
		stopKeepingScreenOn();
		if (gameState.getRoom() != null) {
			getGamesClient().leaveRoom(this, gameState.getRoom().getRoomId());
			gameState.reset();
			switchToScreen(R.id.screen_wait);
		} else {
			switchToMainScreen();
		}
	}

	// Show the waiting room UI to track the progress of other players as they
	// enter the
	// room and get connected.
	private void showWaitingRoom(Room room) {
		gameState.setWaitRoomDismissedFromCode(false);

		// minimum number of players required for our game
		final int MIN_PLAYERS = 2;
		Intent i = getGamesClient().getRealTimeWaitingRoomIntent(room,
				MIN_PLAYERS);

		// show waiting room UI
		startActivityForResult(i, RC_WAITING_ROOM);
	}

	// Forcibly dismiss the waiting room UI (this is useful, for example, if we
	// realize the
	// game needs to start because someone else is starting to play).
	private void dismissWaitingRoom() {
		gameState.setWaitRoomDismissedFromCode(true);
		finishActivity(RC_WAITING_ROOM);
	}

	private void updateRoom(Room room) {
//		Log.d(TAG, "updateRoom :" + room.toString());
	}

	private void showNewGameRequestDialog(final String whitePlayerId,
			final String blackPlayerId, final boolean isRated,
			final int timeControll, final int increment) {

		GameRequestDialog grd = new GameRequestDialog();
		grd.setGameDetails(gameState.getDisplayName(whitePlayerId) + " vs "
				+ gameState.getDisplayName(blackPlayerId) + " , "
				+ (isRated ? "rated" : "friendly") + " game.");

		grd.setListener(new GameRequestDialogListener() {

			@Override
			public void onGameRequestRejected() {
				realTimeChessGame.abort();
			}

			@Override
			public void onGameRequestAccepted() {
				realTimeChessGame.start();
				startGame();
			}
		});

		grd.show(this.getSupportFragmentManager(), TAG);
	}

	private void displayShortMessage(String string) {
		Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT)
				.show();
	}

	private void switchToScreen(int screenId) { 
		for (int id : SCREENS) {
			findViewById(id).setVisibility(
					screenId == id ? View.VISIBLE : View.GONE);
		}
		mCurScreen = screenId;
 
		boolean showInvPopup;
		if (gameState == null || gameState.getIncomingInvitationId() == null) {
			showInvPopup = false;
		} else {
			showInvPopup = (mCurScreen == R.id.screen_main);
		}

		findViewById(R.id.invitation_popup).setVisibility(
				showInvPopup ? View.VISIBLE : View.GONE);
	}

	private void switchToMainScreen() {
		switchToScreen(isSignedIn() ? R.id.screen_main : R.id.screen_sign_in);
	}

	private void keepScreenOn() {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	private void stopKeepingScreenOn() {
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	private void showNewGameDialog() {
		NewGameDialog d = new NewGameDialog();
		d.setListener(this);
		d.show(this.getSupportFragmentManager(), TAG);
	}

	private void updatePlayerStateView() {
		((TextView) findViewById(R.id.playerName)).setText(getGamesClient().getCurrentPlayer().getDisplayName());
		((TextView) findViewById(R.id.playerRating)).setText( "Rating:" + Math.round(gameState.getOwner().getRating()));
		System.out.println(getGamesClient().getCurrentPlayer().getHiResImageUri().toString());
		ImageManager.create(this.getApplicationContext()).loadImage((ImageView) findViewById(R.id.playerAvatar), getGamesClient().getCurrentPlayer().getIconImageUri());
//		((ImageView) findViewById(R.id.playerAvatar)).setImageURI(getGamesClient().getCurrentPlayer().hasHiResImage() ? getGamesClient().getCurrentPlayer().getHiResImageUri() : getGamesClient().getCurrentPlayer().getIconImageUri());
//		new DownloadImageTask((ImageView)  findViewById(R.id.playerAvatar)).execute(getGamesClient().getCurrentPlayer().getHiResImageUri().toString());
	}

	@Override
	public void onInvitationRemoved(String invitationId) {
		// TODO Auto-generated method stub
		
	}
}
