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
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.Layout;
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
import android.widget.ImageButton;
import android.widget.TextView;

import com.chessyoup.R;
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
import com.chessyoup.ui.ChessTableUI.ChessTableUIListener;
import com.chessyoup.ui.fragment.FragmenChat;
import com.chessyoup.ui.fragment.FragmentGame;
import com.chessyoup.ui.fragment.MainViewPagerAdapter;
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
		OnInvitationReceivedListener , ChessTableUIListener{

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
	
	

	// *********************************************************************
	// *********************************************************************
	// GameHelperListener methods
	// *********************************************************************
	// *********************************************************************

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

		getGamesClient().registerInvitationListener(this);

		if (getInvitationId() != null) {
			acceptInviteToRoom(getInvitationId());
			return;
		}

		switchToMainScreen();		
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
		
		for (int id : CLICKABLES) {
			findViewById(id).setOnClickListener(this);
		}
	}

	@Override
	public void onClick(View v) {
		Intent intent;

		switch (v.getId()) {
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
		}
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

	// *********************************************************************
	// OnInvitationReceivedListener methods
	// *********************************************************************

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

	// *********************************************************************
	// *********************************************************************
	// RoomStatusUpdateListener methods
	// *********************************************************************
	// *********************************************************************

	// Called when we are connected to the room. We're not ready to play yet!
	// (maybe not everybody
	// is connected yet).
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

	// Called when we get disconnected from the room. We return to the main
	// screen.
	@Override
	public void onDisconnectedFromRoom(Room room) {
		mRoom = null;
		showGameError();
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

	// *********************************************************************
	// *********************************************************************
	// RealTimeMessageReceivedListener methods
	// *********************************************************************
	// *********************************************************************

	// Called when we receive a real-time message from the network.
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
			
			while(it.hasNext()){
				String key = it.next().toString();								
				payload.put(key, json.get(key).toString());				
			}
			
			this.handleIncomingGameCommand(payload.get("cmd") ,payload );
									
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
		Map<String, String> payload = new HashMap<String, String>();
		payload.put("m", chatMessage);
		sendGameCommand("chat", payload);		
	}

	@Override
	public void onMove(String move) {
		Map<String, String> payload = new HashMap<String, String>();
		payload.put("mv", move);
		sendGameCommand("move", payload);								
	}

	@Override
	public void onDrawRequested() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onResign() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAbortRequested() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRematchRequested() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onExit() {
		// TODO Auto-generated method stub
		
	}

	// *********************************************************************
	// *********************************************************************
	// Private section
	// *********************************************************************
	// *********************************************************************	

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
	private void startGame(String whitePlayerId,String blackPlayerId) {

		switchToScreen(R.id.screen_game);
		
		this.chessTableUI.getCtrl().newGame(mMyId.equals(whitePlayerId) ? new ChessboardMode(
				ChessboardMode.TWO_PLAYERS_BLACK_REMOTE) : new ChessboardMode(
				ChessboardMode.TWO_PLAYERS_WHITE_REMOTE));
		this.chessTableUI.getCtrl().startGame();
		this.chessTableUI.flipBoard(!mMyId.equals(whitePlayerId));		
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
		
		for(Participant p : room.getParticipants()){
			if( !p.getParticipantId().equals(mMyId) ){
				mRemoteId = p.getParticipantId();
			}
		}						
	}

	// Broadcast a message indicating that we're starting to play. Everyone else
	// will react
	// by dismissing their waiting room UIs and starting to play too.
	private void broadcastStart() {			
		String wp = getNextWhitePlayer();
		String bp = wp.equals(mMyId) ? mRemoteId : mMyId;
		Map<String, String> payload = new HashMap<String, String>();
		payload.put("wp", wp);
		payload.put("bp", bp);
		this.sendGameCommand("start", payload);
		
		// start the game!
		startGame(wp,bp);
	}

	private void sendGameCommand(String cmd, Map<String, String> payload) {
		JSONObject json = new JSONObject();

		try {
			json.put("cmd", cmd);

			if (payload != null) {
				for (String key : payload.keySet()) {
					json.put(key, payload.get(key));
				}
			}

			getGamesClient().sendReliableRealTimeMessage(null,
					json.toString().getBytes(), mRoom.getRoomId(),
					mRemoteId);

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void handleIncomingGameCommand(String command , Map<String,String> payload) {
		Log.d(TAG, "handleIncomingGameCommand :"+payload);
		
		if( command.equals("start")){
			dismissWaitingRoom();
			startGame(payload.get("wp"),payload.get("bp"));
		}
		else if( command.equals("move")){
			this.chessTableUI.getCtrl().makeRemoteMove(payload.get("mv"));
		}
		else if( command.equals("chat")){
			this.chessTableUI.appendChatMesssage(payload.get("m"));
		}
	}
	
	private String getNextWhitePlayer(){
		if( this.mLastWhitePlayerId == null ){
			return mMyId;
		}
		else{
			if( this.mLastWhitePlayerId.equals(mMyId) ){
				this.mLastWhitePlayerId = mRemoteId;
				return mRemoteId;
			}
			else{
				this.mLastWhitePlayerId = mMyId;
				return mMyId;
			}
		}
	}
	
	/*
	 * UI SECTION. Methods that implement the game's UI.
	 */

	// This array lists everything that's clickable, so we can install click
	// event handlers.

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
		} else {
			// if in multiplayer, only show invitation on main screen
			showInvPopup = (mCurScreen == R.id.screen_main);
		}

		findViewById(R.id.invitation_popup).setVisibility(
				showInvPopup ? View.VISIBLE : View.GONE);
	}

	void switchToMainScreen() {
		switchToScreen(isSignedIn() ? R.id.screen_main : R.id.screen_sign_in);
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
	private boolean verifyPlaceholderIdsReplaced() {
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
	private void keepScreenOn() {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	// Clears the flag that keeps the screen on.
	private void stopKeepingScreenOn() {
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}	
}
