package com.chessyoup.ui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.Toast;

import com.chessyoup.R;
import com.chessyoup.game.ChessGameClient;
import com.chessyoup.game.view.ChessBoardPlay;
import com.chessyoup.game.view.ColorTheme;
import com.google.android.gms.games.GamesActivityResultCodes;

public class ChessTableActivity extends FragmentActivity{
	
	private static final String TAG = "ChessTableActivity";
	
	private boolean boardGestures = true;

	private ChessBoardPlay cb;
		
	private FragmentGame fGame;

	private FragmenChat fChat;

	private ViewPager gameViewPager;

	private DateFormat dateFormat;

	public ImageButton abortButton;

	public ImageButton resignButton;

	public ImageButton drawButton;

	public ImageButton exitButton;

	public ImageButton rematchButton;

	private boolean drawRequested;

	private boolean abortRequested;

	public void onCreate(Bundle savedInstanceState) {
		Log.d("ChessboardActivity", "on create");
		super.onCreate(savedInstanceState);
		dateFormat = new SimpleDateFormat("EEEE, kk:mm", Locale.getDefault());		
		this.initUI();				
		ChessGameClient.getChessClient().waitingForRemotePlayer(this);										
	}
	
	@Override
	public void onActivityResult(int requestCode, int responseCode,
			Intent intent) {
		super.onActivityResult(requestCode, responseCode, intent);

		Log.d(TAG, "onActivityResult :: "+" requestCode :"+requestCode+" , responseCode :"+responseCode+" , "+intent);
		
		// we got the result from the "waiting room" UI.
		if (responseCode == Activity.RESULT_OK) {
			// player wants to start playing
			Log.d(TAG,
					"Starting game because user requested via waiting room UI.");

			
		} else if (responseCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
			// player actively indicated that they want to leave the room
			Log.d(TAG,
					"GamesActivityResultCodes.RESULT_LEFT_ROOM");
			finish();
			
		} else if (responseCode == Activity.RESULT_CANCELED) {
			// player actively indicated that they want to leave the room
			Log.d(TAG,"Activity.RESULT_CANCELED");
			finish();
		}
	}
	
	@SuppressWarnings("deprecation")
	private void initUI() {
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.chessboard);
		ColorTheme.instance().readColors(
				PreferenceManager.getDefaultSharedPreferences(this));

		cb = (ChessBoardPlay) findViewById(R.id.chessboard);
		cb.setFocusable(true);
		cb.requestFocus();
		cb.setClickable(true);
		cb.setPgnOptions(new com.chessyoup.model.pgn.PGNOptions());

		this.abortButton = (ImageButton) findViewById(R.id.abortGameButton);
		this.resignButton = (ImageButton) findViewById(R.id.resignGameButton);
		this.drawButton = (ImageButton) findViewById(R.id.drawGameButton);
		this.exitButton = (ImageButton) findViewById(R.id.exitGameButton);
		this.rematchButton = (ImageButton) findViewById(R.id.rematchGameButton);

		this.gameViewPager = (ViewPager) this
				.findViewById(R.id.chessBoardViewPager);
		this.fChat = new FragmenChat();
		this.fGame = new FragmentGame();
		MainViewPagerAdapter fAdapter = new MainViewPagerAdapter(getSupportFragmentManager());
		fAdapter.addFragment(this.fGame);
		fAdapter.addFragment(this.fChat);
		this.gameViewPager.setAdapter(fAdapter);
		this.gameViewPager.setCurrentItem(1);
		this.gameViewPager.setCurrentItem(0);
		Bitmap bmp = BitmapFactory.decodeResource(getResources(),
				R.drawable.border);
		BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bmp);
		bitmapDrawable.setTileModeXY(Shader.TileMode.REPEAT,
				Shader.TileMode.REPEAT);
		this.findViewById(R.id.chessboardLayout).setBackgroundDrawable(
				bitmapDrawable);
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d("ChessboardActivity", "on start");
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d("ChessboardActivity", "on spause");
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d("ChessboardActivity", "on resume");
		Toast.makeText(this, "Whiting for oponent!", Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d("ChessboardActivity", "on stop");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d("ChessboardActivity", "on destroy");
	}
}
