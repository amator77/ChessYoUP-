package com.chessyoup.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.chessyoup.R;
import com.chessyoup.chessboard.ChessboardMode;
import com.chessyoup.game.GameState;
import com.chessyoup.game.PlayerState;
import com.chessyoup.game.RealTimeChessGame;
import com.chessyoup.game.RealTimeChessGame.RealTimeChessGameListener;
import com.chessyoup.ui.ChessTableUI.ChessTableUIListener;
import com.chessyoup.ui.fragment.GameRequestDialog;
import com.chessyoup.ui.fragment.GameRequestDialog.GameRequestDialogListener;
import com.chessyoup.ui.fragment.NewGameDialog;
import com.chessyoup.ui.fragment.NewGameDialog.NewGameDialogListener;
import com.google.android.gms.appstate.AppStateClient;
import com.google.android.gms.appstate.OnStateLoadedListener;
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
			this.realTimeChessGame = new RealTimeChessGame(getGamesClient(), gameState);
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
			gameState.setIncomingInvitationId(null);
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
				broadcastReady();

				if (gameState.isLocalPlayerRoomOwner()) {
					showNewGameDialog(new NewGameDialogListener() {

						@Override
						public void onNewGameRejected() {
							Log.d(TAG, "New game dialog canceled!");
						}

						@Override
						public void onNewGameCreated(String color,
								boolean isRated, int timeControll, int increment) {
							Log.d(TAG, "onNewGameCreated :: color :" + color
									+ " , isRated :" + isRated
									+ " , timeControll" + timeControll);

							realTimeChessGame.newGame(
									color.equals("white") ? gameState.getMyId()
											: gameState.getRemoteId(),
									color.equals("white") ? gameState
											.getRemoteId() : gameState
											.getMyId(), timeControll,
									increment, isRated);
						}
					});
				}
			} else if (responseCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
				leaveRoom();
			} else if (responseCode == Activity.RESULT_CANCELED) {
				leaveRoom();
			}

			break;
		}
	}

	private void showNewGameDialog(NewGameDialogListener listener) {
		NewGameDialog d = new NewGameDialog();
		d.setListener(listener);
		d.show(this.getSupportFragmentManager(), TAG);
	}

	// Activity is going to the background. We have to leave the current room.
	@Override
	public void onStop() {
		Log.d(TAG, "**** got onStop");
		leaveRoom();
		stopKeepingScreenOn();
		switchToScreen(R.id.screen_wait);
		super.onStop();
	}

	// Activity just got to the foreground. We switch to the wait screen because
	// we will now
	// go through the sign-in flow (remember that, yes, every time the Activity
	// comes back to the
	// foreground we go through the sign-in flow -- but if the user is already
	// authenticated,
	// this flow simply succeeds and is imperceptible).
	@Override
	public void onStart() {
		switchToScreen(R.id.screen_wait);
		super.onStart();
	}

	// Handle back key to make sure we cleanly leave a game if we are in the
	// middle of one
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
		gameState.setIncomingInvitationId(invitation.getInvitationId());
		((TextView) findViewById(R.id.incoming_invitation_text))
				.setText(invitation.getInviter().getDisplayName() + " "
						+ getString(R.string.is_inviting_you));
		switchToScreen(mCurScreen); // This will show the invitation popup
	}

	// *********************************************************************
	// *********************************************************************
	// RoomStatusUpdateListener methods
	// *********************************************************************
	// *********************************************************************

	@Override
	public void onConnectedToRoom(Room room) {
		Log.d(TAG, "onConnectedToRoom.");

		gameState.setMyId(room.getParticipantId(getGamesClient()
				.getCurrentPlayerId()));

		// get room ID, participants and my ID:
		gameState.setRoom(room);

		for (Participant p : room.getParticipants()) {
			if (!p.getParticipantId().equals(gameState.getMyId())) {
				gameState.setRemoteId(p.getParticipantId());
				break;
			}
		}

		// print out the list of participants (for debug purposes)
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
	public void onNewGameRecevied(String whitePlayerId, String blackPlayerId,
			int time, int increment, boolean rated) {

		showNewGameRequestDialog(whitePlayerId, blackPlayerId, rated, time,
				increment);
	}

	@Override
	public void onStartRecevied() {
		startGame();
	}

	@Override
	public void onReadyRecevied(double remoteRating, double remoteRD) {
		dismissWaitingRoom();
		displayShortMessage("Ready to play!");
	}

	@Override
	public void onMoveRecevied(String move, int thinkingTime) {
		this.chessTableUI.getCtrl().makeRemoteMove(move);
	}

	@Override
	public void onResignRecevied() {
		Log.d(TAG, "Remote resigned!");
		this.chessTableUI.getCtrl().resignGame();
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
		this.sendMove(move);
	}

	@Override
	public void onDrawRequested() {
		this.sendMove("draw");
	}

	@Override
	public void onResign() {
		this.sendMove("resign");
	}

	@Override
	public void onFlag() {
		this.sendMove("flag");
	}

	@Override
	public void onAbortRequested() {
		this.sendMove("abort");
	}

	@Override
	public void onRematchRequested() {

		if (gameState.isLocalPlayerRoomOwner()) {
			showNewGameDialog(this);
		} else {
			this.sendMove("rematch");
		}
	}
	
	@Override
	public void onChatReceived(String message){
		this.chessTableUI.appendChatMesssage(message);
	}
	
	@Override
	public void onTableExit() {
		leaveRoom();
	}

	// *********************************************************************
	// *********************************************************************
	// Private section
	// *********************************************************************
	// *********************************************************************

	private void sendMove(String move) {
		realTimeChessGame.move(move, 0);				
	}

	// Handle the result of the "Select players UI" we launched when the user
	// clicked the
	// "Invite friends" button. We react by creating a room with those players.
	private void handleSelectPlayersResult(int response, Intent data) {
		if (response != Activity.RESULT_OK) {
			Log.w(TAG, "*** select players UI cancelled, " + response);
			switchToMainScreen();
			return;
		}

		Log.d(TAG, "Select players UI succeeded.");

		// get the invitee list
		final ArrayList<String> invitees = data
				.getStringArrayListExtra(GamesClient.EXTRA_PLAYERS);
		Log.d(TAG, "Invitee count: " + invitees.size());
		Log.d(TAG, "Invitee: " + invitees.toString());

		// get the automatch criteria
		Bundle autoMatchCriteria = null;
		int minAutoMatchPlayers = data.getIntExtra(
				GamesClient.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
		int maxAutoMatchPlayers = data.getIntExtra(
				GamesClient.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
		if (minAutoMatchPlayers > 0 || maxAutoMatchPlayers > 0) {
			autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
					minAutoMatchPlayers, maxAutoMatchPlayers, 0);
			Log.d(TAG, "Automatch criteria: " + autoMatchCriteria);
		}

		// create the room
		Log.d(TAG, "Creating room...");
		RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
		rtmConfigBuilder.addPlayersToInvite(invitees);
		rtmConfigBuilder.setMessageReceivedListener(this.realTimeChessGame);
		rtmConfigBuilder.setRoomStatusUpdateListener(this);
		if (autoMatchCriteria != null) {
			rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
		}
		switchToScreen(R.id.screen_wait);
		keepScreenOn();
		resetGameVars();
		getGamesClient().createRoom(rtmConfigBuilder.build());
		Log.d(TAG, "Room created, waiting for it to be ready...");
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
		resetGameVars();
		getGamesClient().joinRoom(roomConfigBuilder.build());
	}

	// Show error message about game being cancelled and return to main screen.
	private void showGameError() {
		showAlert(getString(R.string.error), getString(R.string.game_problem));
		switchToMainScreen();
	}

	// Reset game variables in preparation for a new game.
	private void resetGameVars() {

	}

	// Start the gameplay phase of the game.
	private void startGame() {
		if (gameState.getStartGameRequest() != null) {
			String[] tcs = getResources().getStringArray(
					R.array.time_control_values);
			int time = Integer.parseInt(tcs[Integer.valueOf(gameState
					.getStartGameRequest().getTime())]);
			String[] tis = getResources().getStringArray(
					R.array.time_increment_values);
			int inc = Integer.parseInt(tis[Integer.valueOf(gameState
					.getStartGameRequest().getIncrement())]);
			this.startGame(gameState.getStartGameRequest().getWhitePlayerId(),
					gameState.getStartGameRequest().getBlackPlayerId(), time,
					inc);
		}
	}

	private void startGame(String whitePlayerId, String blackPlayerId,
			int timeControll, int increment) {
		this.chessTableUI.getCtrl().setTimeLimit(timeControll, 0, increment);
		this.chessTableUI.getCtrl().newGame(
				gameState.getMyId().equals(whitePlayerId) ? new ChessboardMode(
						ChessboardMode.TWO_PLAYERS_BLACK_REMOTE)
						: new ChessboardMode(
								ChessboardMode.TWO_PLAYERS_WHITE_REMOTE));
		this.chessTableUI.getCtrl().startGame();
		this.chessTableUI.flipBoard(!gameState.getMyId().equals(whitePlayerId));
		gameState.setStartGameRequest(null);
		displayShortMessage("Game started!");
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
		Log.d(TAG, "updateRoom :" + room.toString());
	}

	private void broadcastReady() {

		if (gameState.isLocalPlayerRoomOwner()) {
			realTimeChessGame.ready();			
		}
	}

	private void showNewGameRequestDialog(final String whitePlayerId,
			final String blackPlayerId, boolean isRated,
			final int timeControll, final int increment) {

		GameRequestDialog grd = new GameRequestDialog();
		grd.setGameDetails(whitePlayerId + " vs " + blackPlayerId + " , "
				+ (isRated ? "rated" : "friendly") + " game.");

		grd.setListener(new GameRequestDialogListener() {

			@Override
			public void onGameRequestRejected() {
				realTimeChessGame.abort();				
			}

			@Override
			public void onGameRequestAccepted() {
				realTimeChessGame.start();				
				String[] tcs = getResources().getStringArray(
						R.array.time_control_values);
				int time = Integer.parseInt(tcs[Integer.valueOf(timeControll)]);
				String[] tis = getResources().getStringArray(
						R.array.time_increment_values);
				int inc = Integer.parseInt(tis[Integer.valueOf(increment)]);
				startGame(whitePlayerId, blackPlayerId, time, inc);
			}
		});

		grd.show(this.getSupportFragmentManager(), TAG);
	}

	private void displayShortMessage(String string) {
		Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT)
				.show();
	}

	private void switchToScreen(int screenId) {
		// make the requested screen visible; hide all others.
		for (int id : SCREENS) {
			findViewById(id).setVisibility(
					screenId == id ? View.VISIBLE : View.GONE);
		}
		mCurScreen = screenId;

		// should we show the invitation popup?
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

	@Override
	public void onNewGameRejected() {
		Log.d(TAG, "New game dialog canceled!");
	}

	@Override
	public void onNewGameCreated(String color, boolean isRated,
			int timeControll, int increment) {
		Log.d(TAG, "onNewGameCreated :: color :" + color + " , isRated :"
				+ isRated + " , timeControll" + timeControll);

		realTimeChessGame.newGame(
				color.equals("white") ? gameState.getMyId() : gameState
						.getRemoteId(),
				color.equals("white") ? gameState.getRemoteId() : gameState
						.getMyId(), timeControll, increment, isRated);
	}

	private void updatePlayerStateView() {
		StringBuffer sb = new StringBuffer();
		sb.append("Welcome ").append(
				getGamesClient().getCurrentPlayer().getDisplayName());
		sb.append("\n")
				.append("ELO :" + gameState.getOwner().getRating().getRating())
				.append(" , Wins :" + gameState.getOwner().getWins());
		sb.append(" , Draws:" + this.gameState.getOwner().getDraws()
				+ " , Loses :" + this.gameState.getOwner().getLoses());
		Log.d(TAG, "Player state :" + sb.toString());
		((TextView) findViewById(R.id.playerStateView)).setText(sb.toString());
	}
}
