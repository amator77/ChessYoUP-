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
		OnInvitationReceivedListener, ChessboardUIInterface {

	private final static String TAG = "ChessYoUpActivity";
	private final static int RC_SELECT_PLAYERS = 10000;
	private final static int RC_INVITATION_INBOX = 10001;
	private final static int RC_WAITING_ROOM = 10002;

	// Room ID where the currently active game is taking place; null if we're
	// not playing.
	private Room mRoom = null;

	// My participant ID in the currently active game
	private String mMyId = null;

	// Remote participant ID in the currently active game
	private String mRemoteId = null;
	
	// Remote participant ID in the currently active game
	private String mLastWhitePlayerId = null;
			
	// If non-null, this is the id of the invitation we received via the
	// invitation listener
	private String mIncomingInvitationId = null;

	private boolean iAmWhite = false;

	// flag indicating whether we're dismissing the waiting room because the
	// game is starting
	private boolean mWaitRoomDismissedFromCode = false;

	// chessboard controller
	private ChessboardController ctrl;

	private final static int[] CLICKABLES = {
			R.id.button_accept_popup_invitation, R.id.button_invite_players,
			R.id.button_see_invitations, R.id.button_sign_in,
			R.id.button_sign_out, };

	// This array lists all the individual screens our game has.
	private final static int[] SCREENS = { R.id.screen_game, R.id.screen_main,
			R.id.screen_sign_in, R.id.screen_wait };

	private int mCurScreen = -1;

	private FragmentGame fGame;

	private FragmenChat fChat;

	private ViewPager gameViewPager;

	private ImageButton abortButton;

	private ImageButton resignButton;

	private ImageButton drawButton;

	private ImageButton exitButton;

	private ImageButton rematchButton;

	private boolean drawRequested;

	private boolean abortRequested;

	private PgnScreenText gameTextListener;

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

		Log.d(TAG, getGamesClient().getCurrentGame().toString());
		Log.d(TAG, getGamesClient().getCurrentPlayer().toString());

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

		this.abortButton = (ImageButton) findViewById(R.id.abortGameButton);
		this.resignButton = (ImageButton) findViewById(R.id.resignGameButton);
		this.drawButton = (ImageButton) findViewById(R.id.drawGameButton);
		this.exitButton = (ImageButton) findViewById(R.id.exitGameButton);
		this.rematchButton = (ImageButton) findViewById(R.id.rematchGameButton);

		this.gameViewPager = (ViewPager) this
				.findViewById(R.id.chessBoardViewPager);
		this.fChat = new FragmenChat();
		this.fGame = new FragmentGame();
		MainViewPagerAdapter fAdapter = new MainViewPagerAdapter(
				getSupportFragmentManager());
		fAdapter.addFragment(this.fGame);
		fAdapter.addFragment(this.fChat);
		this.gameViewPager.setAdapter(fAdapter);
		this.gameViewPager.setCurrentItem(1);
		this.gameViewPager.setCurrentItem(0);

		PGNOptions pgOptions = new PGNOptions();
		this.gameTextListener = new PgnScreenText(pgOptions);
		this.ctrl = new ChessboardController(this, this.gameTextListener,
				pgOptions);
		this.installListeners();
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
	// ChessboardUIInterface methods
	// *********************************************************************
	// *********************************************************************

	@Override
	public String blackPlayerName() {

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

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (fGame != null && fGame.moveListView != null
						&& gameTextListener != null
						&& gameTextListener.getSpannableData() != null) {
					fGame.moveListView.setText(gameTextListener
							.getSpannableData());
					Layout layout = fGame.moveListView.getLayout();
					if (layout != null) {
						int currPos = gameTextListener.getCurrPos();
						int line = layout.getLineForOffset(currPos);
						int y = (int) ((line - 1.5) * fGame.moveListView
								.getLineHeight());
						fGame.moveListScroll.scrollTo(0, y);
					}
				}
			}
		});
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
		Log.d(TAG, "set position " + arg0.toString());
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

	// *********************************************************************
	// *********************************************************************
	// Private section
	// *********************************************************************
	// *********************************************************************

	private void sendMoveToRemote(String moveToUCIString) {
		Map<String, String> payload = new HashMap<String, String>();
		payload.put("mv", moveToUCIString);
		sendGameCommand("move", payload);				
	}

	private void installListeners() {
		for (int id : CLICKABLES) {
			findViewById(id).setOnClickListener(this);
		}

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
							Log.d(TAG, "handleClick" + sq);

							if (m != null) {
								Log.d(TAG, "Move :" + m);
								if (true) {
									Log.d(TAG,
											"Local turn  :"
													+ ctrl.getGame()
															.getGameState()
													+ " , "
													+ ctrl.getGame().currPos().whiteMove);
									ctrl.makeLocalMove(m);
									sendMoveToRemote(TextIO.moveToUCIString(m));
								}
							}
						}
					}
				});

		cb.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				Log.d(TAG, "onTouch");
				return gd.onTouchEvent(event);
			}
		});
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
	private void acceptInviteToRoom(String invId) {
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
		
		this.ctrl.newGame(mMyId.equals(whitePlayerId) ? new ChessboardMode(
				ChessboardMode.TWO_PLAYERS_BLACK_REMOTE) : new ChessboardMode(
				ChessboardMode.TWO_PLAYERS_WHITE_REMOTE));
		this.ctrl.startGame();
		if ( !mMyId.equals(whitePlayerId)) {
			ChessBoardPlay cb = (ChessBoardPlay) findViewById(R.id.chessboard);
			cb.setFlipped(true);
		}
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
				
		updatePeerScoresDisplay();
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
			this.ctrl.makeRemoteMove(payload.get("mv"));
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

			if (token == null) {
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
}
