package com.chessyoup.ui;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.text.Layout;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.Toast;

import com.chessyoup.R;
import com.chessyoup.chessboard.ChessboardController;
import com.chessyoup.chessboard.ChessboardStatus;
import com.chessyoup.chessboard.ChessboardUIInterface;
import com.chessyoup.game.view.ChessBoardPlay;
import com.chessyoup.game.view.PgnScreenTextView;
import com.chessyoup.model.Game.GameState;
import com.chessyoup.model.Move;
import com.chessyoup.model.Position;
import com.chessyoup.model.TextIO;
import com.chessyoup.model.pgn.PGNOptions;
import com.chessyoup.ui.fragment.FragmenChat;
import com.chessyoup.ui.fragment.FragmentGame;
import com.chessyoup.ui.fragment.MainViewPagerAdapter;

public class ChessTableUI implements ChessboardUIInterface {

	public interface ChessTableUIListener {

		void onChat(String chatMessage);

		void onMove(String move);

		void onDrawRequested();

		void onResign();

		void onAbortRequested();

		void onRematchRequested();

		void onTableExit();
	}

	protected static final String TAG = "ChessTableUI";

	private ChessTableUIListener chessTableUIListener;

	private ChessBoardPlay boardPlay;

	private ChessboardController ctrl;

	private FragmentGame fGame;

	private FragmenChat fChat;

	private ViewPager gameViewPager;

	private ImageButton abortButton;

	private ImageButton resignButton;

	private ImageButton drawButton;

	private ImageButton exitButton;

	private ImageButton rematchButton;

	private PgnScreenTextView pgnScreenTextView;

	private FragmentActivity parent;

	public ChessTableUI(FragmentActivity parent) {
		this.parent = parent;
		this.boardPlay = (ChessBoardPlay) parent.findViewById(R.id.chessboard);
		this.abortButton = (ImageButton) parent
				.findViewById(R.id.abortGameButton);
		this.resignButton = (ImageButton) parent
				.findViewById(R.id.resignGameButton);
		this.drawButton = (ImageButton) parent
				.findViewById(R.id.drawGameButton);
		this.exitButton = (ImageButton) parent
				.findViewById(R.id.exitGameButton);
		this.rematchButton = (ImageButton) parent
				.findViewById(R.id.rematchGameButton);
		this.gameViewPager = (ViewPager) parent
				.findViewById(R.id.chessBoardViewPager);
		this.fChat = new FragmenChat();
		this.fGame = new FragmentGame();
		MainViewPagerAdapter fAdapter = new MainViewPagerAdapter(
				parent.getSupportFragmentManager());
		fAdapter.addFragment(this.fGame);
		fAdapter.addFragment(this.fChat);
		this.gameViewPager.setAdapter(fAdapter);
		this.gameViewPager.setCurrentItem(1);
		this.gameViewPager.setCurrentItem(0);
		PGNOptions pgOptions = new PGNOptions();
		this.pgnScreenTextView = new PgnScreenTextView(pgOptions);
		this.ctrl = new ChessboardController(this, this.pgnScreenTextView,
				pgOptions);
		this.installListeners(parent);
	}

	public ChessboardController getCtrl() {
		return ctrl;
	}

	public ChessTableUIListener getChessTableUIListener() {
		return chessTableUIListener;
	}

	public void setChessTableUIListener(
			ChessTableUIListener chessTableUIListener) {
		this.chessTableUIListener = chessTableUIListener;
	}

	@Override
	public void setPosition(Position pos, String variantInfo,
			ArrayList<Move> variantMoves) {
		Log.d(TAG, "set position " + TextIO.toFEN(pos));
		boardPlay.setPosition(pos);
	}

	@Override
	public void setSelection(int sq) {
		boardPlay.setSelection(sq);
		boardPlay.userSelectedSquare = false;
	}

	@Override
	public void setStatus(ChessboardStatus s) {
		String str;
		switch (s.state) {
		case ALIVE:
			str = Integer.valueOf(s.moveNr).toString();
			if (s.white)
				str += ". " + this.parent.getString(R.string.whites_move);
			else
				str += "... " + this.parent.getString(R.string.blacks_move);
			if (s.ponder)
				str += " (" + this.parent.getString(R.string.ponder) + ")";
			if (s.thinking)
				str += " (" + this.parent.getString(R.string.thinking) + ")";
			if (s.analyzing)
				str += " (" + this.parent.getString(R.string.analyzing) + ")";
			break;
		case WHITE_MATE:
			str = this.parent.getString(R.string.white_mate);
			break;
		case BLACK_MATE:
			str = this.parent.getString(R.string.black_mate);
			break;
		case WHITE_STALEMATE:
		case BLACK_STALEMATE:
			str = this.parent.getString(R.string.stalemate);
			break;
		case DRAW_REP: {
			str = this.parent.getString(R.string.draw_rep);
			if (s.drawInfo.length() > 0)
				str = str + " [" + s.drawInfo + "]";
			break;
		}
		case DRAW_50: {
			str = this.parent.getString(R.string.draw_50);
			if (s.drawInfo.length() > 0)
				str = str + " [" + s.drawInfo + "]";
			break;
		}
		case DRAW_NO_MATE:
			str = this.parent.getString(R.string.draw_no_mate);
			break;
		case DRAW_AGREE:
			str = this.parent.getString(R.string.draw_agree);
			break;
		case RESIGN_WHITE:
			str = this.parent.getString(R.string.resign_white);
			break;
		case RESIGN_BLACK:
			str = this.parent.getString(R.string.resign_black);
			break;
		default:
			str = "unknown";
		}
		
		Log.d(TAG, "setStatus :: "+str);
	}

	@Override
	public void moveListUpdated() {
		// TODO Auto-generated method stub
		this.parent.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (fGame != null && fGame.moveListView != null
						&& pgnScreenTextView != null
						&& pgnScreenTextView.getSpannableData() != null) {
					fGame.moveListView.setText(pgnScreenTextView
							.getSpannableData());
					Layout layout = fGame.moveListView.getLayout();
					if (layout != null) {
						int currPos = pgnScreenTextView.getCurrPos();
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
	public void requestPromotePiece() {
		promoteDialog().show();
	}

	@Override
	public void runOnUIThread(Runnable runnable) {
		this.parent.runOnUiThread(runnable);
	}

	@Override
	public void reportInvalidMove(Move m) {
		// TODO Auto-generated method stub
		String msg = String.format("%s %s-%s",
				parent.getString(R.string.invalid_move),
				TextIO.squareToString(m.from), TextIO.squareToString(m.to));
		Toast.makeText(parent.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
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
		// TODO Auto-generated method stub

	}

	public void flipBoard(boolean flip) {
		boardPlay.setFlipped(flip);
	}

	private void installListeners(FragmentActivity parent) {

		final GestureDetector gd = new GestureDetector(parent,
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
						boardPlay.cancelLongPress();
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

							int sq = boardPlay.eventToSquare(e);
							Move m = boardPlay.mousePressed(sq);
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

									if (chessTableUIListener != null) {
										chessTableUIListener.onMove(TextIO
												.moveToUCIString(m));
									}
								}
							}
						}
					}
				});

		boardPlay.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				Log.d(TAG, "onTouch");
				return gd.onTouchEvent(event);
			}
		});

		fChat.runInstallListener = new Runnable() {

			@Override
			public void run() {
				fChat.chatSendMessageButton
						.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								Log.d(TAG, "Send message request.");

								if (chessTableUIListener != null) {
									chessTableUIListener
											.onChat(fChat.chatEditText
													.getEditableText()
													.toString());
								}

								fChat.chatEditText.setText("");
							}
						});

				fChat.chatEditText.setOnKeyListener(new View.OnKeyListener() {

					@Override
					public boolean onKey(View v, int keyCode, KeyEvent event) {
						Log.d("key event", event.toString());

						if (event != null
								&& (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

							if (chessTableUIListener != null) {
								chessTableUIListener.onChat(fChat.chatEditText
										.getEditableText().toString());
							}

							fChat.chatEditText.setText("");
							// InputMethodManager in = (InputMethodManager)
							// parent.getSystemService(Context.INPUT_METHOD_SERVICE);
							// in.hideSoftInputFromWindow(v.getWindowToken(),
							// 0);

							return true;
						} else {
							return false;
						}
					}
				});
			}
		};

		fGame.runInstallListeners = new Runnable() {

			@Override
			public void run() {
				abortButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						if (ctrl.getGame().getGameState() == GameState.ALIVE) {
							if (ctrl.isAbortRequested()) {
								ctrl.abortGame();
								fGame.moveListView.append(" aborted");
								ctrl.setAbortRequested(false);

								if (chessTableUIListener != null) {
									chessTableUIListener.onMove("abort");
								}
							} else {
								ctrl.setAbortRequested(true);
								
								if (chessTableUIListener != null) {
									chessTableUIListener.onAbortRequested();
								}
							}
						}
					}
				});

				resignButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						if (ctrl.getGame().getGameState() == GameState.ALIVE) {
							ctrl.resignGame();

							if (chessTableUIListener != null) {
								chessTableUIListener.onResign();
							}
						}
					}
				});

				drawButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						if (ctrl.getGame().getGameState() == GameState.ALIVE) {
							if (ctrl.isDrawRequested()) {
								ctrl.drawGame();
								ctrl.setDrawRequested(false);

								if (chessTableUIListener != null) {
									chessTableUIListener.onMove("draw");
								}

							} else {

								ctrl.offerDraw();
								ctrl.setDrawRequested(true);

								if (chessTableUIListener != null) {
									chessTableUIListener.onDrawRequested();
								}
							}
						}
					}
				});

				rematchButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						if (ctrl.getGame().getGameState() != GameState.ALIVE) {
														
							if (chessTableUIListener != null) {
								chessTableUIListener.onRematchRequested();
							}							
						}
					}
				});

				exitButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						if (ctrl.getGame().getGameState() == GameState.ALIVE) {

							AlertDialog.Builder db = new AlertDialog.Builder(
									ChessTableUI.this.parent
											.getApplicationContext());
							db.setTitle("Resign?");
							String actions[] = new String[2];
							actions[0] = "Ok";
							actions[1] = "Cancel";
							db.setItems(actions,
									new DialogInterface.OnClickListener() {

										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											switch (which) {
											case 0:
												ctrl.resignGame();

												if (chessTableUIListener != null) {
													chessTableUIListener
															.onMove("resign");
													chessTableUIListener
															.onTableExit();
												}
												break;
											case 1:
												break;
											default:

												break;
											}
										}
									});

							AlertDialog ad = db.create();
							ad.setCancelable(true);
							ad.setCanceledOnTouchOutside(false);
							ad.show();
						} else {

							if (chessTableUIListener != null) {
								chessTableUIListener.onTableExit();
							}

							ctrl.abortGame();
						}
					}
				});
			}
		};
	}

	public void appendChatMesssage(String string) {
		fChat.chatDisplay.append(string);
	}
	
	private final Dialog promoteDialog() {
        final CharSequence[] items = {
            parent.getString(R.string.queen), parent.getString(R.string.rook),
            parent.getString(R.string.bishop), parent.getString(R.string.knight)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(parent);
        builder.setTitle(R.string.promote_pawn_to);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                ctrl.reportPromotePiece(item);
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }
}
