package com.chessyoup.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import com.chessyoup.ui.ChessTableUI.ChessTableUIListener;
import com.chessyoup.ui.fragment.GameRequestDialog;
import com.chessyoup.ui.fragment.GameRequestDialog.GameRequestDialogListener;
import com.chessyoup.ui.fragment.NewGameDialog;
import com.chessyoup.ui.fragment.NewGameDialog.NewGameDialogListener;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.example.games.basegameutils.BaseGameActivity;

public class ChessYoUpActivity extends BaseGameActivity implements
		View.OnClickListener, RealTimeMessageReceivedListener,
		RoomStatusUpdateListener, RoomUpdateListener,
		OnInvitationReceivedListener, ChessTableUIListener, NewGameDialogListener {

	private final static String TAG = "ChessYoUpActivity";

	private final static int RC_SELECT_PLAYERS = 10000;

	private final static int RC_INVITATION_INBOX = 10001;

	private final static int RC_WAITING_ROOM = 10002;

	private ChessTableUI chessTableUI;

	private Room mRoom = null;

	private String mMyId = null;

	private String mRemoteId = null;

	private String mLastWhitePlayerId = null;

	private String mIncomingInvitationId = null;

	private boolean mWaitRoomDismissedFromCode = false;

	private final static int[] CLICKABLES = {
			R.id.button_accept_popup_invitation, R.id.button_invite_players,
			R.id.button_see_invitations, R.id.button_sign_in,
			R.id.button_sign_out, };

	private final static int[] SCREENS = { R.id.screen_game, R.id.screen_main,
			R.id.screen_sign_in, R.id.screen_wait };

	private int mCurScreen = -1;

	private Map<String, String> newGameCommand;

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
			acceptInviteToRoom(mIncomingInvitationId);
			mIncomingInvitationId = null;
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
			if (mWaitRoomDismissedFromCode) {
				switchToScreen(R.id.screen_game);
				break;
			}

			if (responseCode == Activity.RESULT_OK) {
				Log.d(TAG, "Sending ready request to remote.");
				switchToScreen(R.id.screen_game);
				broadcastReady();

				if (mRoom.getCreatorId().equals(mMyId)) {
					showNewGameDialog(new NewGameDialogListener() {

						@Override
						public void onNewGameRejected() {
							Log.d(TAG, "New game dialog canceled!");
						}

						@Override
						public void onNewGameCreated(String color,
								boolean isRated, int timeControll , int increment) {
							Log.d(TAG, "onNewGameCreated :: color :" + color
									+ " , isRated :" + isRated
									+ " , timeControll" + timeControll);
							Map<String, String> cmd = new HashMap<String, String>();
							cmd.put("cmd", "newGame");
							cmd.put("wp", color.equals("white") ? mMyId
									: mRemoteId);
							cmd.put("bp", color.equals("white") ? mRemoteId
									: mMyId);
							cmd.put("ir", isRated + "");
							cmd.put("tc", String.valueOf(timeControll));
							cmd.put("inc", String.valueOf(increment));
							newGameCommand = cmd;
							sendGameCommand(cmd);
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
		mIncomingInvitationId = invitation.getInvitationId();
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

		mMyId = room.getParticipantId(getGamesClient().getCurrentPlayerId());

		// get room ID, participants and my ID:
		mRoom = room;

		for (Participant p : room.getParticipants()) {
			if (!p.getParticipantId().equals(mMyId)) {
				mRemoteId = p.getParticipantId();
				break;
			}
		}

		// print out the list of participants (for debug purposes)
		Log.d(TAG, "Room ID: " + room.getRoomId());
		Log.d(TAG, "My ID " + mMyId);
		Log.d(TAG, "Remote ID " + mRemoteId);
		Log.d(TAG, "<< CONNECTED TO ROOM>>");
	}

	@Override
	public void onDisconnectedFromRoom(Room room) {
		mRoom = null;
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
	// RealTimeMessageReceivedListener methods
	// *********************************************************************
	// *********************************************************************

	@Override
	public void onRealTimeMessageReceived(RealTimeMessage rtm) {
		Log.d(TAG,
				"onRealTimeMessageReceived :" + rtm.toString() + " , "
						+ rtm.getSenderParticipantId() + " , "
						+ new String(rtm.getMessageData()));

		try {
			JSONObject json = new JSONObject(new String(rtm.getMessageData()));
			Map<String, String> payload = new HashMap<String, String>();
			Iterator it = json.keys();

			while (it.hasNext()) {
				String key = it.next().toString();
				payload.put(key, json.get(key).toString());
			}

			this.handleIncomingGameCommand(payload.get("cmd"), payload);

		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}
	}

	// *********************************************************************
	// *********************************************************************
	// ChessTableUIListener methods
	// *********************************************************************
	// *********************************************************************

	@Override
	public void onChat(String chatMessage) {
		Map<String, String> command = new HashMap<String, String>();
		command.put("cmd", "chat");
		command.put("m", chatMessage);
		sendGameCommand(command);
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
	public void onAbortRequested() {
		this.sendMove("abort");
	}

	@Override
	public void onRematchRequested() {
		
		if (isRoomOwner()) {
			showNewGameDialog(this);
		} else {
			this.sendMove("rematch");
		}				
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
		Map<String, String> command = new HashMap<String, String>();
		command.put("cmd", "mv");
		command.put("mv", move);
		sendGameCommand(command);
	}

	private String getRemoteDisplayName() {
		if (this.mRoom != null) {
			if (this.mRemoteId != null) {
				for (Participant p : this.mRoom.getParticipants()) {
					if (p.getParticipantId().equals(this.mRemoteId)) {
						return p.getPlayer().getDisplayName();
					}
				}
			}
		}

		return null;
	}

	private String getDisplayName(String participantId) {
		if (this.mRoom != null) {

			for (Participant p : this.mRoom.getParticipants()) {
				if (p.getParticipantId().equals(participantId)) {
					return p.getPlayer().getDisplayName();
				}
			}
		}

		return null;
	}

	private void startQuickGame() {
		// quick-start a game with 1 randomly selected opponent
		final int MIN_OPPONENTS = 1, MAX_OPPONENTS = 1;
		Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
				MIN_OPPONENTS, MAX_OPPONENTS, 0);
		RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
		rtmConfigBuilder.setMessageReceivedListener(this);
		rtmConfigBuilder.setRoomStatusUpdateListener(this);
		rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
		switchToScreen(R.id.screen_wait);
		keepScreenOn();
		resetGameVars();
		getGamesClient().createRoom(rtmConfigBuilder.build());
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
		rtmConfigBuilder.setMessageReceivedListener(this);
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

	// Accept the given invitation.
	private void acceptInviteToRoom(String invId) {
		// accept the invitation
		Log.d(TAG, "Accepting invitation: " + invId);
		RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this);
		roomConfigBuilder.setInvitationIdToAccept(invId)
				.setMessageReceivedListener(this)
				.setRoomStatusUpdateListener(this);
		switchToScreen(R.id.screen_wait);
		keepScreenOn();
		resetGameVars();
		getGamesClient().joinRoom(roomConfigBuilder.build());
	}

	/*
	 * GAME LOGIC SECTION. Methods that implement the game's rules.
	 */

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

		if (newGameCommand != null) {
			String[] tcs = getResources().getStringArray(R.array.time_control_values);
			int time = Integer.parseInt( tcs[Integer.valueOf(newGameCommand.get("tc"))]);	
			String[] tis = getResources().getStringArray(R.array.time_increment_values);
			int inc = Integer.parseInt( tis[Integer.valueOf(newGameCommand.get("inc"))]);
			this.startGame(newGameCommand.get("wp"), newGameCommand.get("bp") , time ,  inc);
		}
	}

	private void startGame(String whitePlayerId, String blackPlayerId , int timeControll , int increment) {				
		this.chessTableUI.getCtrl().setTimeLimit(timeControll, 0, increment);
		this.chessTableUI.getCtrl().newGame(
				mMyId.equals(whitePlayerId) ? new ChessboardMode(
						ChessboardMode.TWO_PLAYERS_BLACK_REMOTE)
						: new ChessboardMode(
								ChessboardMode.TWO_PLAYERS_WHITE_REMOTE));
		this.chessTableUI.getCtrl().startGame();
		this.chessTableUI.flipBoard(!mMyId.equals(whitePlayerId));
		newGameCommand = null;
		displayShortMessage("Game started!");
	}

	// Leave the room.
	private void leaveRoom() {
		Log.d(TAG, "Leaving room.");
		stopKeepingScreenOn();
		if (mRoom != null) {
			getGamesClient().leaveRoom(this, mRoom.getRoomId());
			mRoom = null;
			switchToScreen(R.id.screen_wait);
		} else {
			switchToMainScreen();
		}
	}

	// Show the waiting room UI to track the progress of other players as they
	// enter the
	// room and get connected.
	private void showWaitingRoom(Room room) {
		mWaitRoomDismissedFromCode = false;

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
		mWaitRoomDismissedFromCode = true;
		finishActivity(RC_WAITING_ROOM);
	}

	private void updateRoom(Room room) {

		for (Participant p : room.getParticipants()) {
			if (!p.getParticipantId().equals(mMyId)) {
				mRemoteId = p.getParticipantId();
			}
		}
	}

	private boolean isRoomOwner() {
		return mMyId.equals(mRoom.getCreatorId());
	}

	private void broadcastReady() {

		if (isRoomOwner()) {
			Map<String, String> command = new HashMap<String, String>();
			command.put("cmd", "ready");
			this.sendGameCommand(command);
		}
	}

	private void sendGameCommand(Map<String, String> command) {
		JSONObject json = new JSONObject();

		try {

			for (String key : command.keySet()) {
				json.put(key, command.get(key));
			}

			Log.d(TAG, "Sending command :" + json.toString());

			getGamesClient().sendReliableRealTimeMessage(null,
					json.toString().getBytes(), mRoom.getRoomId(), mRemoteId);

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void handleIncomingGameCommand(String command,
			Map<String, String> payload) {
		Log.d(TAG, "handleIncomingGameCommand :" + payload);

		if (command.equals("ready")) {
			dismissWaitingRoom();
			displayShortMessage("Ready to play!");
		} else if (command.equals("newGame")) {
			showNewGameRequestDialog(payload.get("wp"), payload.get("bp"),
					payload.get("ir"), payload.get("tc") , payload.get("inc"));
		} else if (command.equals("gameRejected")) {
			newGameCommand = null;
			displayShortMessage("Game rejected!");
		} else if (command.equals("gameAccepted")) {
			startGame();
		} else if (command.equals("mv")) {

			String move = payload.get("mv");

			if (move.equals("draw")) {
				Log.d(TAG, "Draw requested!");

				if (this.chessTableUI.getCtrl().isDrawRequested()) {
					this.chessTableUI.getCtrl().drawGame();
					displayShortMessage("Draw by mutual agreement!");
				} else {
					this.chessTableUI.getCtrl().setDrawRequested(true);
					displayShortMessage(getRemoteDisplayName()
							+ " is requesting draw!");
				}
			} else if (move.equals("abort")) {
				Log.d(TAG, "Abort requested!");

				if (this.chessTableUI.getCtrl().isAbortRequested()) {
					this.chessTableUI.getCtrl().abortGame();
					displayShortMessage("Game aborted!");
				} else {
					this.chessTableUI.getCtrl().setAbortRequested(true);
					displayShortMessage(getRemoteDisplayName()
							+ " is requesting to abort the game!");
				}
			} else if (move.equals("resign")) {
				Log.d(TAG, "Remote resigned!");
				this.chessTableUI.getCtrl().resignGame();
				displayShortMessage(getRemoteDisplayName() + " resigned!");
			} else if (move.equals("rematch")) {				
				if (!isRoomOwner()) {
					displayShortMessage(getRemoteDisplayName()
							+ " is requesting rematch!");
				}				
			} else {
				this.chessTableUI.getCtrl().makeRemoteMove(payload.get("mv"));
			}

		} else if (command.equals("chat")) {
			this.chessTableUI.appendChatMesssage(payload.get("m"));
		}
	}

	private void showNewGameRequestDialog(final String whitePlayerId,
			final String blackPlayerId, String isRated, final String timeControll , final String increment) {

		GameRequestDialog grd = new GameRequestDialog();
		grd.setGameDetails(whitePlayerId + " vs " + blackPlayerId + " , "
				+ (isRated.equals("true") ? "rated" : "friendly") + " game.");

		grd.setListener(new GameRequestDialogListener() {

			@Override
			public void onGameRequestRejected() {
				Map<String, String> cmd = new HashMap<String, String>();
				cmd.put("cmd", "gameRejected");
				sendGameCommand(cmd);
			}

			@Override
			public void onGameRequestAccepted() {
				Map<String, String> cmd = new HashMap<String, String>();
				cmd.put("cmd", "gameAccepted");
				sendGameCommand(cmd);					
				String[] tcs = getResources().getStringArray(R.array.time_control_values);
				int time = Integer.parseInt( tcs[Integer.valueOf(timeControll)]);	
				String[] tis = getResources().getStringArray(R.array.time_increment_values);
				int inc = Integer.parseInt( tis[Integer.valueOf(increment)]);
				startGame(whitePlayerId,blackPlayerId,time,inc);
			}
		});
		
		grd.show(this.getSupportFragmentManager(), TAG);
	}

	private void displayShortMessage(String string) {
		Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT)
				.show();
	}

	private String getNextWhitePlayer() {
		if (this.mLastWhitePlayerId == null) {
			return mMyId;
		} else {
			if (this.mLastWhitePlayerId.equals(mMyId)) {
				this.mLastWhitePlayerId = mRemoteId;
				return mRemoteId;
			} else {
				this.mLastWhitePlayerId = mMyId;
				return mMyId;
			}
		}
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
		if (mIncomingInvitationId == null) {
			// no invitation, so no popup
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
	public void onNewGameCreated(String color,
			boolean isRated, int timeControll , int increment) {
		Log.d(TAG, "onNewGameCreated :: color :" + color
				+ " , isRated :" + isRated
				+ " , timeControll" + timeControll);
		Map<String, String> cmd = new HashMap<String, String>();
		cmd.put("cmd", "newGame");
		cmd.put("wp", color.equals("white") ? mMyId
				: mRemoteId);
		cmd.put("bp", color.equals("white") ? mRemoteId
				: mMyId);
		cmd.put("ir", isRated + "");
		cmd.put("tc", String.valueOf(timeControll));
		cmd.put("inc", String.valueOf(increment));
		newGameCommand = cmd;
		sendGameCommand(cmd);
	}
}
