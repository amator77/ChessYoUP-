package com.chessyoup.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.chessyoup.R;
import com.chessyoup.game.ChessGameClient;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.example.games.basegameutils.BaseGameActivity;

public class ChessYoUpActivity extends BaseGameActivity  {

	private static final String TAG = "ChessYoUpActivity";

	// Request codes for the UIs that we show with startActivityForResult:
	private final static int RC_SELECT_PLAYERS = 10000;
	private final static int RC_INVITATION_INBOX = 10001;
	private final static int RC_WAITING_ROOM = 10002;

	// This array lists all the individual screens our game has.
	private final static int[] SCREENS = { R.id.screen_game, R.id.screen_main,
			R.id.screen_sign_in, R.id.screen_wait };
	private int mCurScreen = -1;

	private String mIncomingInvitationId = null;

	private boolean mWaitRoomDismissedFromCode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ChessGameClient.init(getGamesClient(),this.getApplicationContext());
		beginUserInitiatedSignIn();
	}

	@Override
	public void onSignInFailed() {
		Log.d(TAG, "onSignInFailed");
	}

	@Override
	public void onSignInSucceeded() {
		Log.d(TAG, "onSignInSucceeded");
		Intent intent = getGamesClient().getSelectPlayersIntent(1, 2);
		switchToScreen(R.id.screen_wait);
		startActivityForResult(intent, RC_SELECT_PLAYERS);
		;
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
				startGame();
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

	private void leaveRoom() {
		Log.d(TAG, "leaveRoom");

	}

	private void startGame() {
		Log.d(TAG, "startGame");

	}

	private void broadcastStart() {
		Log.d(TAG, "broadcastStart");
	}

	private void handleInvitationInboxResult(int responseCode, Intent intent) {
		Log.d(TAG, "broadcastStart");

	}

	private void handleSelectPlayersResult(int responseCode, Intent intent) {
		Log.d(TAG, "handleSelectPlayersResult ::" + responseCode + " , "
				+ intent);

		if (responseCode != Activity.RESULT_OK) {
			switchToScreen(R.id.screen_main);
			return;
		}

		final ArrayList<String> invitees = intent.getStringArrayListExtra(GamesClient.EXTRA_PLAYERS);
		Log.d(TAG, "Invitee: " + invitees.toString());
		
		switchToScreen(R.id.screen_wait);	
		ChessGameClient.getChessClient().invitePlayer(invitees);				
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
}
