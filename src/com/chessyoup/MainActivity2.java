/* Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chessyoup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.TextView;

import com.chessyoup.chessboard.ChessboardController;
import com.chessyoup.chessboard.ChessboardMode;
import com.chessyoup.chessboard.ChessboardStatus;
import com.chessyoup.chessboard.ChessboardUIInterface;
import com.chessyoup.game.view.ChessBoardPlay;
import com.chessyoup.game.view.ColorTheme;
import com.chessyoup.model.GameTree.Node;
import com.chessyoup.model.Move;
import com.chessyoup.model.Position;
import com.chessyoup.model.TextIO;
import com.chessyoup.model.pgn.PGNOptions;
import com.chessyoup.model.pgn.PgnToken;
import com.chessyoup.model.pgn.PgnTokenReceiver;
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

/**
 * Button Clicker 2000. A minimalistic game showing the multiplayer features of
 * the Google Play game services API. The objective of this game is clicking a
 * button. Whoever clicks the button the most times within a 20 second interval
 * wins. It's that simple. This game can be played with 2, 3 or 4 players. The
 * code is organized in sections in order to make understanding as clear as
 * possible. We start with the integration section where we show how the game is
 * integrated with the Google Play game services API, then move on to
 * game-specific UI and logic. INSTRUCTIONS: To run this sample, please set up a
 * project in the Developer Console. Then, place your app ID on
 * res/values/ids.xml. Also, change the package name to the package name you
 * used to create the client ID in Developer Console. Make sure you sign the APK
 * with the certificate whose fingerprint you entered in Developer Console when
 * creating your Client Id.
 * 
 * @author Bruno Oliveira (btco), 2013-04-26
 */
public class MainActivity2 extends BaseGameActivity implements
		View.OnClickListener, RealTimeMessageReceivedListener,
		RoomStatusUpdateListener, RoomUpdateListener,
		OnInvitationReceivedListener, ChessboardUIInterface {

	/*
	 * API INTEGRATION SECTION. This section contains the code that integrates
	 * the game with the Google Play game services API.
	 */

	// Debug tag
	final static boolean ENABLE_DEBUG = true;
	final static String TAG = "ButtonClicker2000";

	// Request codes for the UIs that we show with startActivityForResult:
	final static int RC_SELECT_PLAYERS = 10000;
	final static int RC_INVITATION_INBOX = 10001;
	final static int RC_WAITING_ROOM = 10002;

	// Room ID where the currently active game is taking place; null if we're
	// not playing.
	String mRoomId = null;

	// Are we playing in multiplayer mode?
	boolean mMultiplayer = false;

	// The participants in the currently active game
	ArrayList<Participant> mParticipants = null;

	// My participant ID in the currently active game
	String mMyId = null;

	// If non-null, this is the id of the invitation we received via the
	// invitation listener
	String mIncomingInvitationId = null;
	
	boolean iAmWhite = false;
	
	// Message buffer for sending messages
	byte[] mMsgBuf = new byte[2];

	// flag indicating whether we're dismissing the waiting room because the
	// game is starting
	boolean mWaitRoomDismissedFromCode = false;

	PGNOptions pgnOptions;
	PgnScreenText gameTextListener;
	ChessboardController ctrl;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		enableDebugLog(ENABLE_DEBUG, TAG);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// set up a click listener for everything we care about
		for (int id : CLICKABLES) {
			findViewById(id).setOnClickListener(this);
		}

		pgnOptions = new PGNOptions();
		this.gameTextListener = new PgnScreenText(pgnOptions);
		this.ctrl = new ChessboardController(this, this.gameTextListener,
				pgnOptions);
		this.installListeners();
	}

	/**
	 * Called by the base class (BaseGameActivity) when sign-in has failed. For
	 * example, because the user hasn't authenticated yet. We react to this by
	 * showing the sign-in button.
	 */
	@Override
	public void onSignInFailed() {
		Log.d(TAG, "Sign-in failed.");
		switchToScreen(R.id.screen_sign_in);
	}

	/**
	 * Called by the base class (BaseGameActivity) when sign-in succeeded. We
	 * react by going to our main screen.
	 */
	@Override
	public void onSignInSucceeded() {
		Log.d(TAG, "Sign-in succeeded.");

		// install invitation listener so we get notified if we receive an
		// invitation to play
		// a game.
		getGamesClient().registerInvitationListener(this);

		// if we received an invite via notification, accept it; otherwise, go
		// to main screen
		if (getInvitationId() != null) {
			acceptInviteToRoom(getInvitationId());
			return;
		}
		switchToMainScreen();
	}

	@Override
	public void onClick(View v) {
		Intent intent;

		switch (v.getId()) {
		case R.id.button_single_player:
		case R.id.button_single_player_2:
			resetGameVars();
			startGame(false);
			break;
		case R.id.button_sign_in:
			// user wants to sign in
			if (!verifyPlaceholderIdsReplaced()) {
				showAlert("Error",
						"Sample not set up correctly. Please see README.");
				return;
			}
			beginUserInitiatedSignIn();
			break;
		case R.id.button_sign_out:
			signOut();
			switchToScreen(R.id.screen_sign_in);
			break;
		case R.id.button_invite_players:
			// show list of invitable players
			intent = getGamesClient().getSelectPlayersIntent(1, 3);
			switchToScreen(R.id.screen_wait);
			startActivityForResult(intent, RC_SELECT_PLAYERS);
			break;
		case R.id.button_see_invitations:
			// show list of pending invitations
			intent = getGamesClient().getInvitationInboxIntent();
			switchToScreen(R.id.screen_wait);
			startActivityForResult(intent, RC_INVITATION_INBOX);
			break;
		case R.id.button_accept_popup_invitation:
			// user wants to accept the invitation shown on the invitation
			// popup
			// (the one we got through the OnInvitationReceivedListener).
			acceptInviteToRoom(mIncomingInvitationId);
			mIncomingInvitationId = null;
			break;
		case R.id.button_quick_game:
			// user wants to play against a random opponent right now
			startQuickGame();
			break;
		}
	}

	void startQuickGame() {
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

	@Override
	public void onActivityResult(int requestCode, int responseCode,
			Intent intent) {
		super.onActivityResult(requestCode, responseCode, intent);

		switch (requestCode) {
		case RC_SELECT_PLAYERS:
			// we got the result from the "select players" UI -- ready to create
			// the room
			handleSelectPlayersResult(responseCode, intent);
			break;
		case RC_INVITATION_INBOX:
			// we got the result from the "select invitation" UI (invitation
			// inbox). We're
			// ready to accept the selected invitation:
			handleInvitationInboxResult(responseCode, intent);
			break;
		case RC_WAITING_ROOM:
			// ignore result if we dismissed the waiting room from code:
			if (mWaitRoomDismissedFromCode)
				break;

			// we got the result from the "waiting room" UI.
			if (responseCode == Activity.RESULT_OK) {
				// player wants to start playing
				Log.d(TAG,
						"Starting game because user requested via waiting room UI.");

				// let other players know we're starting.
				broadcastStart();

				// start the game!
				startGame(true);
			} else if (responseCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
				// player actively indicated that they want to leave the room
				leaveRoom();
			} else if (responseCode == Activity.RESULT_CANCELED) {
				/*
				 * Dialog was cancelled (user pressed back key, for instance).
				 * In our game, this means leaving the room too. In more
				 * elaborate games,this could mean something else (like
				 * minimizing the waiting room UI but continue in the handshake
				 * process).
				 */
				leaveRoom();
			}

			break;
		}
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
		iAmWhite = true;
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
	void acceptInviteToRoom(String invId) {
		// accept the invitation
		iAmWhite = false;
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

	// Activity is going to the background. We have to leave the current room.
	@Override
	public void onStop() {
		Log.d(TAG, "**** got onStop");

		// if we're in a room, leave it.
		leaveRoom();

		// stop trying to keep the screen on
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

	// Leave the room.
	void leaveRoom() {
		Log.d(TAG, "Leaving room.");
		mSecondsLeft = 0;
		stopKeepingScreenOn();
		if (mRoomId != null) {
			getGamesClient().leaveRoom(this, mRoomId);
			mRoomId = null;
			switchToScreen(R.id.screen_wait);
		} else {
			switchToMainScreen();
		}
	}

	// Show the waiting room UI to track the progress of other players as they
	// enter the
	// room and get connected.
	void showWaitingRoom(Room room) {
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
	void dismissWaitingRoom() {
		mWaitRoomDismissedFromCode = true;
		finishActivity(RC_WAITING_ROOM);
	}

	// Called when we get an invitation to play a game. We react by showing that
	// to the user.
	@Override
	public void onInvitationReceived(Invitation invitation) {
		// We got an invitation to play a game! So, store it in
		// mIncomingInvitationId
		// and show the popup on the screen.
		mIncomingInvitationId = invitation.getInvitationId();
		((TextView) findViewById(R.id.incoming_invitation_text))
				.setText(invitation.getInviter().getDisplayName() + " "
						+ getString(R.string.is_inviting_you));
		switchToScreen(mCurScreen); // This will show the invitation popup
	}

	/*
	 * CALLBACKS SECTION. This section shows how we implement the several games
	 * API callbacks.
	 */

	// Called when we are connected to the room. We're not ready to play yet!
	// (maybe not everybody
	// is connected yet).
	@Override
	public void onConnectedToRoom(Room room) {
		Log.d(TAG, "onConnectedToRoom.");

		// get room ID, participants and my ID:
		mRoomId = room.getRoomId();
		mParticipants = room.getParticipants();
		mMyId = room.getParticipantId(getGamesClient().getCurrentPlayerId());

		// print out the list of participants (for debug purposes)
		Log.d(TAG, "Room ID: " + mRoomId);
		Log.d(TAG, "My ID " + mMyId);
		Log.d(TAG, "<< CONNECTED TO ROOM>>");
	}

	// Called when we've successfully left the room (this happens a result of
	// voluntarily leaving
	// via a call to leaveRoom(). If we get disconnected, we get
	// onDisconnectedFromRoom()).
	@Override
	public void onLeftRoom(int statusCode, String roomId) {
		// we have left the room; return to main screen.
		Log.d(TAG, "onLeftRoom, code " + statusCode);
		switchToMainScreen();
	}

	// Called when we get disconnected from the room. We return to the main
	// screen.
	@Override
	public void onDisconnectedFromRoom(Room room) {
		mRoomId = null;
		showGameError();
	}

	// Show error message about game being cancelled and return to main screen.
	void showGameError() {
		showAlert(getString(R.string.error), getString(R.string.game_problem));
		switchToMainScreen();
	}

	// Called when room has been created
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

	// Called when room is fully connected.
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

	// We treat most of the room update callbacks in the same way: we update our
	// list of
	// participants and update the display. In a real game we would also have to
	// check if that
	// change requires some action like removing the corresponding player avatar
	// from the screen,
	// etc.
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

	void updateRoom(Room room) {
		mParticipants = room.getParticipants();
		updatePeerScoresDisplay();
	}

	/*
	 * GAME LOGIC SECTION. Methods that implement the game's rules.
	 */

	// Current state of the game:
	int mSecondsLeft = -1; // how long until the game ends (seconds)
	final static int GAME_DURATION = 20; // game duration, seconds.
	int mScore = 0; // user's current score

	// Reset game variables in preparation for a new game.
	void resetGameVars() {
		mSecondsLeft = GAME_DURATION;
		mScore = 0;
		mParticipantScore.clear();
		mFinishedParticipants.clear();
	}

	// Start the gameplay phase of the game.
	void startGame(boolean multiplayer) {
		mMultiplayer = multiplayer;
		updateScoreDisplay();
		broadcastScore(false);
		switchToScreen(R.id.screen_game);
		Log.d(TAG," inv id :"+mIncomingInvitationId);		
		this.ctrl.newGame(iAmWhite ? new ChessboardMode(ChessboardMode.TWO_PLAYERS_BLACK_REMOTE) : new ChessboardMode(ChessboardMode.TWO_PLAYERS_WHITE_REMOTE));
		this.ctrl.startGame();
		if( !iAmWhite ){
			ChessBoardPlay cb = (ChessBoardPlay) findViewById(R.id.chessboard);
			cb.setFlipped(true);
		}
	}

	// indicates the player scored one point
	void scoreOnePoint() {
		if (mSecondsLeft <= 0)
			return; // too late!
		++mScore;
		updateScoreDisplay();
		updatePeerScoresDisplay();

		// broadcast our new score to our peers
		broadcastScore(false);
	}

	/*
	 * COMMUNICATIONS SECTION. Methods that implement the game's network
	 * protocol.
	 */

	// Score of other participants. We update this as we receive their scores
	// from the network.
	Map<String, Integer> mParticipantScore = new HashMap<String, Integer>();

	// Participants who sent us their final score.
	Set<String> mFinishedParticipants = new HashSet<String>();

	// Called when we receive a real-time message from the network.
	// Messages in our game are made up of 2 bytes: the first one is 'F' or 'U'
	// indicating
	// whether it's a final or interim score. The second byte is the score.
	// There is also the
	// 'S' message, which indicates that the game should start.
	@Override
	public void onRealTimeMessageReceived(RealTimeMessage rtm) {
		byte[] buf = rtm.getMessageData();
		String sender = rtm.getSenderParticipantId();
		String move = null;
		
		Log.d(TAG,"onRealTimeMessageReceived :"+rtm.toString()+" , "+rtm.getSenderParticipantId()+" , "+new String(rtm.getMessageData()));
		
		
		try {
			JSONObject json = new JSONObject(new String(rtm.getMessageData()));
			move = json.getString("move");								
		} catch (JSONException e) {			
			e.printStackTrace();
			return;
		}
		
		Log.d(TAG, "Move received: " + move);
		
		ctrl.makeRemoteMove(move);
	}
	
	boolean isRoomOwnerWhite(){
		String remoteId = mParticipants.get(0).getParticipantId();
		Log.d(TAG," My id :"+mMyId);
		Log.d(TAG," Remove id :"+remoteId);
		
		Log.d(TAG,mParticipants.toString());
		
		return true;
	}
	
	// Broadcast my score to everybody else.
	void broadcastScore(boolean finalScore) {
		if (!mMultiplayer)
			return; // playing single-player mode

		// First byte in message indicates whether it's a final score or not
		mMsgBuf[0] = (byte) (finalScore ? 'F' : 'U');

		// Second byte is the score.
		mMsgBuf[1] = (byte) mScore;

		// Send to every other participant.
		for (Participant p : mParticipants) {
			if (p.getParticipantId().equals(mMyId))
				continue;
			if (p.getStatus() != Participant.STATUS_JOINED)
				continue;
			if (finalScore) {
				// final score notification must be sent via reliable message
				getGamesClient().sendReliableRealTimeMessage(null, mMsgBuf,
						mRoomId, p.getParticipantId());
			} else {
				// it's an interim score notification, so we can use unreliable
				getGamesClient().sendUnreliableRealTimeMessage(mMsgBuf,
						mRoomId, p.getParticipantId());
			}
		}
	}

	// Broadcast a message indicating that we're starting to play. Everyone else
	// will react
	// by dismissing their waiting room UIs and starting to play too.
	void broadcastStart() {
		if (!mMultiplayer)
			return; // playing single-player mode

		mMsgBuf[0] = 'S';
		mMsgBuf[1] = (byte) 0;
		for (Participant p : mParticipants) {
			if (p.getParticipantId().equals(mMyId))
				continue;
			if (p.getStatus() != Participant.STATUS_JOINED)
				continue;
			getGamesClient().sendReliableRealTimeMessage(null, mMsgBuf,
					mRoomId, p.getParticipantId());
		}
	}

	/*
	 * UI SECTION. Methods that implement the game's UI.
	 */

	// This array lists everything that's clickable, so we can install click
	// event handlers.
	final static int[] CLICKABLES = { R.id.button_accept_popup_invitation,
			R.id.button_invite_players, R.id.button_quick_game,
			R.id.button_see_invitations, R.id.button_sign_in,
			R.id.button_sign_out, R.id.button_single_player,
			R.id.button_single_player_2 };

	// This array lists all the individual screens our game has.
	final static int[] SCREENS = { R.id.screen_game, R.id.screen_main,
			R.id.screen_sign_in, R.id.screen_wait };
	int mCurScreen = -1;

	void switchToScreen(int screenId) {
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
		} else if (mMultiplayer) {
			// if in multiplayer, only show invitation on main screen
			showInvPopup = (mCurScreen == R.id.screen_main);
		} else {
			// single-player: show on main screen and gameplay screen
			showInvPopup = (mCurScreen == R.id.screen_main || mCurScreen == R.id.screen_game);
		}
		findViewById(R.id.invitation_popup).setVisibility(
				showInvPopup ? View.VISIBLE : View.GONE);
	}

	void switchToMainScreen() {
		switchToScreen(isSignedIn() ? R.id.screen_main : R.id.screen_sign_in);
	}

	// updates the label that shows my score
	void updateScoreDisplay() {

	}

	// formats a score as a three-digit number
	String formatScore(int i) {
		if (i < 0)
			i = 0;
		String s = String.valueOf(i);
		return s.length() == 1 ? "00" + s : s.length() == 2 ? "0" + s : s;
	}

	// updates the screen with the scores from our peers
	void updatePeerScoresDisplay() {

	}

	/*
	 * MISC SECTION. Miscellaneous methods.
	 */

	/**
	 * Checks that the developer (that's you!) read the instructions. IMPORTANT:
	 * a method like this SHOULD NOT EXIST in your production app! It merely
	 * exists here to check that anyone running THIS PARTICULAR SAMPLE did what
	 * they were supposed to in order for the sample to work.
	 */
	boolean verifyPlaceholderIdsReplaced() {
		final boolean CHECK_PKGNAME = true; // set to false to disable check
											// (not recommended!)

		// Did the developer forget to change the package name?
		if (CHECK_PKGNAME && getPackageName().startsWith("com.google.example."))
			return false;

		// Did the developer forget to replace a placeholder ID?
		int res_ids[] = new int[] { R.string.app_id };
		for (int i : res_ids) {
			if (getString(i).equalsIgnoreCase("ReplaceMe"))
				return false;
		}
		return true;
	}

	// Sets the flag to keep this screen on. It's recommended to do that during
	// the
	// handshake when setting up a game, because if the screen turns off, the
	// game will be
	// cancelled.
	void keepScreenOn() {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	// Clears the flag that keeps the screen on.
	void stopKeepingScreenOn() {
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	public void onP2PConnected(String participantId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onP2PDisconnected(String participantId) {
		// TODO Auto-generated method stub

	}

	static class PgnScreenText implements PgnTokenReceiver {
		private SpannableStringBuilder sb = new SpannableStringBuilder();
		private int prevType = PgnToken.EOF;
		int nestLevel = 0;
		boolean col0 = true;
		Node currNode = null;
		final static int indentStep = 15;
		int currPos = 0, endPos = 0;
		boolean upToDate = false;
		PGNOptions options;

		private static class NodeInfo {
			int l0, l1;

			NodeInfo(int ls, int le) {
				l0 = ls;
				l1 = le;
			}
		}

		HashMap<Node, NodeInfo> nodeToCharPos;

		PgnScreenText(PGNOptions options) {
			nodeToCharPos = new HashMap<Node, NodeInfo>();
			this.options = options;
		}

		public final SpannableStringBuilder getSpannableData() {
			return sb;
		}

		public final int getCurrPos() {
			return currPos;
		}

		public boolean isUpToDate() {
			return upToDate;
		}

		int paraStart = 0;
		int paraIndent = 0;
		boolean paraBold = false;

		private final void newLine() {
			newLine(false);
		}

		private final void newLine(boolean eof) {
			if (!col0) {
				if (paraIndent > 0) {
					int paraEnd = sb.length();
					int indent = paraIndent * indentStep;
					sb.setSpan(new LeadingMarginSpan.Standard(indent),
							paraStart, paraEnd,
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				if (paraBold) {
					int paraEnd = sb.length();
					sb.setSpan(new StyleSpan(Typeface.BOLD), paraStart,
							paraEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				if (!eof)
					sb.append('\n');
				paraStart = sb.length();
				paraIndent = nestLevel;
				paraBold = false;
			}
			col0 = true;
		}

		boolean pendingNewLine = false;

		/** Makes moves in the move list clickable. */
		private final static class MoveLink extends ClickableSpan {
			private Node node;

			MoveLink(Node n) {
				node = n;
			}

			@Override
			public void onClick(View widget) {
				// if (ctrl != null)
				// ctrl.goNode(node);
			}

			@Override
			public void updateDrawState(TextPaint ds) {
			}
		}

		public void processToken(Node node, int type, String token) {
			
			if( token == null ){
				return;
			}
			
			if ((prevType == PgnToken.RIGHT_BRACKET)
					&& (type != PgnToken.LEFT_BRACKET)) {
				if (options.view.headers) {
					col0 = false;
					newLine();
				} else {
					sb.clear();
					paraBold = false;
				}
			}
			if (pendingNewLine) {
				if (type != PgnToken.RIGHT_PAREN) {
					newLine();
					pendingNewLine = false;
				}
			}
			switch (type) {
			case PgnToken.STRING:
				sb.append(" \"");
				sb.append(token);
				sb.append('"');
				break;
			case PgnToken.INTEGER:
				if ((prevType != PgnToken.LEFT_PAREN)
						&& (prevType != PgnToken.RIGHT_BRACKET) && !col0)
					sb.append(' ');
				sb.append(token);
				col0 = false;
				break;
			case PgnToken.PERIOD:
				sb.append('.');
				col0 = false;
				break;
			case PgnToken.ASTERISK:
				sb.append(" *");
				col0 = false;
				break;
			case PgnToken.LEFT_BRACKET:
				sb.append('[');
				col0 = false;
				break;
			case PgnToken.RIGHT_BRACKET:
				sb.append("]\n");
				col0 = false;
				break;
			case PgnToken.LEFT_PAREN:
				nestLevel++;
				if (col0)
					paraIndent++;
				newLine();
				sb.append('(');
				col0 = false;
				break;
			case PgnToken.RIGHT_PAREN:
				sb.append(')');
				nestLevel--;
				pendingNewLine = true;
				break;
			case PgnToken.NAG:
				sb.append(Node.nagStr(Integer.parseInt(token)));
				col0 = false;
				break;
			case PgnToken.SYMBOL: {
				if ((prevType != PgnToken.RIGHT_BRACKET)
						&& (prevType != PgnToken.LEFT_BRACKET) && !col0)
					sb.append(' ');
				int l0 = sb.length();
				sb.append(token);
				int l1 = sb.length();
				nodeToCharPos.put(node, new NodeInfo(l0, l1));
				sb.setSpan(new MoveLink(node), l0, l1,
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				if (endPos < l0)
					endPos = l0;
				col0 = false;
				if (nestLevel == 0)
					paraBold = true;
				break;
			}
			case PgnToken.COMMENT:
				if (prevType == PgnToken.RIGHT_BRACKET) {
				} else if (nestLevel == 0) {
					nestLevel++;
					newLine();
					nestLevel--;
				} else {
					if ((prevType != PgnToken.LEFT_PAREN) && !col0) {
						sb.append(' ');
					}
				}
				int l0 = sb.length();
				sb.append(token.replaceAll("[ \t\r\n]+", " ").trim());
				int l1 = sb.length();
				int color = ColorTheme.instance().getColor(
						ColorTheme.PGN_COMMENT);
				sb.setSpan(new ForegroundColorSpan(color), l0, l1,
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				col0 = false;
				if (nestLevel == 0)
					newLine();
				break;
			case PgnToken.EOF:
				newLine(true);
				upToDate = true;
				break;
			}
			prevType = type;
		}

		@Override
		public void clear() {
			sb.clear();
			prevType = PgnToken.EOF;
			nestLevel = 0;
			col0 = true;
			currNode = null;
			currPos = 0;
			endPos = 0;
			nodeToCharPos.clear();
			paraStart = 0;
			paraIndent = 0;
			paraBold = false;
			pendingNewLine = false;

			upToDate = false;
		}

		BackgroundColorSpan bgSpan = new BackgroundColorSpan(0xff888888);

		@Override
		public void setCurrent(Node node) {
			sb.removeSpan(bgSpan);
			NodeInfo ni = nodeToCharPos.get(node);
			if ((ni == null) && (node != null) && (node.getParent() != null))
				ni = nodeToCharPos.get(node.getParent());
			if (ni != null) {
				int color = ColorTheme.instance().getColor(
						ColorTheme.CURRENT_MOVE);
				bgSpan = new BackgroundColorSpan(color);
				sb.setSpan(bgSpan, ni.l0, ni.l1,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				currPos = ni.l0;
			} else {
				currPos = 0;
			}
			currNode = node;
		}
	}

	@Override
	public String blackPlayerName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean discardVariations() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void localMoveMade(Move arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void moveListUpdated() {
		// TODO Auto-generated method stub

	}

	@Override
	public void reportInvalidMove(Move arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void requestPromotePiece() {
		// TODO Auto-generated method stub

	}

	@Override
	public void runOnUIThread(Runnable arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setAnimMove(Position arg0, Move arg1, boolean arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPosition(Position arg0, String arg1, ArrayList<Move> arg2) {
		// TODO Auto-generated method stub
		final ChessBoardPlay cb = (ChessBoardPlay) findViewById(R.id.chessboard);
		Log.d(TAG,"set position "+arg0.toString());
		cb.setPosition(arg0);		
	}

	@Override
	public void setRemainingTime(long arg0, long arg1, long arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSelection(int arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStatus(ChessboardStatus arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public String whitePlayerName() {
		// TODO Auto-generated method stub
		return null;
	}

	private void sendMoveToRemote(String moveToUCIString) {
		JSONObject json = new JSONObject();
		try {
			json.put("move", moveToUCIString);
			
			for( Participant p : mParticipants  ){
				if( !p.getParticipantId().equals(mMyId) ){
					getGamesClient().sendReliableRealTimeMessage(null,json.toString().getBytes() ,
							mRoomId, p.getParticipantId());
				}
			}						
						
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void installListeners() {

		final ChessBoardPlay cb = (ChessBoardPlay) findViewById(R.id.chessboard);

		final GestureDetector gd = new GestureDetector(this,
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
						cb.cancelLongPress();
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
							
							int sq = cb.eventToSquare(e);
							Move m = cb.mousePressed(sq);
							Log.d(TAG,"handleClick"+sq);
							
							if (m != null) {
								Log.d(TAG,"Move :"+m);
								if (true) {
									Log.d(TAG,"Local turn  :"+ctrl.getGame().getGameState() +" , "+ctrl.getGame().currPos().whiteMove);
									ctrl.makeLocalMove(m);
									sendMoveToRemote(TextIO.moveToUCIString(m));
								}
							}
						}
					}
				});
		
		cb.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				Log.d(TAG,"onTouch");
				return gd.onTouchEvent(event);
			}
		});
	}
}
