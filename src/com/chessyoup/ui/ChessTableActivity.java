package com.chessyoup.ui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.Toast;

import com.chessyoup.R;
import com.chessyoup.chessboard.ChessboardController;
import com.chessyoup.chessboard.ChessboardMode;
import com.chessyoup.chessboard.ChessboardStatus;
import com.chessyoup.chessboard.ChessboardUIInterface;
import com.chessyoup.game.ChessGameClient;
import com.chessyoup.game.ChessGameClientListener;
import com.chessyoup.game.view.ChessBoardPlay;
import com.chessyoup.game.view.ColorTheme;
import com.chessyoup.game.view.PgnScreenTextView;
import com.chessyoup.model.Move;
import com.chessyoup.model.Position;
import com.chessyoup.model.pgn.PGNOptions;

public class ChessTableActivity extends FragmentActivity implements
		ChessboardUIInterface, ChessGameClientListener {

	private static final String TAG = "ChessTableActivity";

	private ChessBoardPlay chessboardView;

	private ChessboardController ctrl;

	private FragmentGame fGame;

	private FragmenChat fChat;

	private ViewPager gameViewPager;

	private DateFormat dateFormat;

	public ImageButton abortButton;

	public ImageButton resignButton;

	public ImageButton drawButton;

	public ImageButton exitButton;

	public ImageButton rematchButton;

	@Override
	public void onMoveReceived(String chessMove, int thinkingTime) {
		Log.d(TAG, "onMoveReceived :: " + chessMove + " , thinkingTime:"
				+ thinkingTime);
		ctrl.makeRemoteMove(chessMove);
	}

	@Override
	public void onStartGame(String whitePlayerId, String blackPlayerId,
			String remotePlayerId) {
		Log.d(TAG, "onStartGame :: whitePlayerId : " + whitePlayerId
				+ " , blackPlayerId:" + blackPlayerId + " , remotePlayerId :"
				+ remotePlayerId);
		this.ctrl
				.newGame(whitePlayerId.equals(remotePlayerId) ? new ChessboardMode(
						ChessboardMode.TWO_PLAYERS_WHITE_REMOTE)
						: new ChessboardMode(
								ChessboardMode.TWO_PLAYERS_BLACK_REMOTE));
		this.ctrl.startGame();
	}

	public void onCreate(Bundle savedInstanceState) {
		Log.d("ChessboardActivity", "on create");
		super.onCreate(savedInstanceState);
		dateFormat = new SimpleDateFormat("EEEE, kk:mm", Locale.getDefault());
		this.ctrl = new ChessboardController(this, new PgnScreenTextView(
				new PGNOptions()), new PGNOptions());
		this.initUI();
		this.installListeners();
	}

	@SuppressWarnings("deprecation")
	private void initUI() {
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.chessboard);
		ColorTheme.instance().readColors(
				PreferenceManager.getDefaultSharedPreferences(this));

		chessboardView = (ChessBoardPlay) findViewById(R.id.chessboard);
		chessboardView.setFocusable(true);
		chessboardView.requestFocus();
		chessboardView.setClickable(true);
		chessboardView.setPgnOptions(new com.chessyoup.model.pgn.PGNOptions());

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

	@Override
	public void setPosition(Position pos, String variantInfo,
			ArrayList<Move> variantMoves) {
		Log.d(TAG, "setPosition " + pos.toString());
		this.chessboardView.setPosition(pos);
	}

	@Override
	public void setSelection(int sq) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStatus(ChessboardStatus status) {
		// TODO Auto-generated method stub

	}

	@Override
	public void moveListUpdated() {
		// TODO Auto-generated method stub

	}

	@Override
	public void requestPromotePiece() {
		// TODO Auto-generated method stub

	}

	@Override
	public void runOnUIThread(Runnable runnable) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reportInvalidMove(Move m) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setRemainingTime(long wTime, long bTime, long nextUpdate) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setAnimMove(Position sourcePos, Move move, boolean forward) {
		// TODO Auto-generated method stub

	}

	@Override
	public String whitePlayerName() {
		// TODO Auto-generated method stub
		return null;
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
	public void localMoveMade(Move m) {
		ChessGameClient.getChessClient().sendMove(m, 0);
	}

	private void installListeners() {

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
						chessboardView.cancelLongPress();
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

							int sq = chessboardView.eventToSquare(e);
							Move m = chessboardView.mousePressed(sq);
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
								}
							}
						}
					}
				});

		this.chessboardView.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				Log.d(TAG, "onTouch");
				return gd.onTouchEvent(event);
			}
		});
	}
}
