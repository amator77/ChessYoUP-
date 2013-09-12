package com.chessyoup.game;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.google.android.gms.games.GamesClient;

public class RealTimeChessGame extends RealTimeGame {

	private static final String TAG = "RealTimeChessGame";

	private static final byte READY = 0;
	private static final byte START = 1;
	private static final byte MOVE = 2;
	private static final byte DRAW = 3;
	private static final byte RESIGN = 4;
	private static final byte FLAG = 5;
	private static final byte REMATCH = 6;
	private static final byte ABORT = 7;
	private static final byte NEW_GAME = 8;	
	private static final String ELO_KEY = "elo";
	private static final String RD_KEY = "rd";
	private static final String MOVE_KEY = "m";
	private static final String THINKING_TIME_KEY = "tt";
	private static final String WHITE_PLAYER_KEY = "wp";
	private static final String BLACK_PLAYER_KEY = "bp";
	private static final String TIME_KEY = "t";
	private static final String INCREMENT_KEY = "i";
	private static final String RATED_KEY = "r";

	private RealTimeChessGameListener listener;

	public interface RealTimeChessGameListener {

		public void onNewGameRecevied(String whitePlayerId, String blackPlayerId,
				int time, int increment, boolean rated);
		
		public void onStartRecevied();
		
		public void onReadyRecevied(double remoteRating, double remoteRD);

		public void onMoveRecevied(String move, int thinkingTime);

		public void onResignRecevied();

		public void onDrawRecevied();

		public void onFlagRecevied();

		public void onRematchRecevied();
		
		public void onAbortRecevied();
		
		public void onException(String message);
	}

	public RealTimeChessGame(GamesClient client, GameState gameState) {
		super(client, gameState);
	}

	@Override
	protected void handleMessageReceived(String senderId, byte[] messageData) {
		Log.d(TAG, "handleMessageReceived ::" + parseMessage(messageData));

		byte command = messageData[0];
		byte[] payload = new byte[messageData.length - 1];
		JSONObject jsonPayload = getPayloadJSON(payload);

		for (int i = 1; i < messageData.length; i++) {
			payload[i - 1] = messageData[i];
		}

		switch (command) {
		case READY:
			this.handleReadyReceived(jsonPayload);
			break;
		case NEW_GAME:
			this.handleNewGameReceived(jsonPayload);
			break;
		case START:
			this.handleStartReceived(jsonPayload);
			break;
		case MOVE:
			this.handleMoveReceived(jsonPayload);
			break;
		case RESIGN:
			this.handleResignReceived(jsonPayload);
			break;
		case DRAW:
			this.handleDrawReceived(jsonPayload);
			break;
		case FLAG:
			this.handleFlagReceived(jsonPayload);
			break;
		case REMATCH:
			this.handleRematchReceived(jsonPayload);
			break;
		case ABORT:
			this.handleAbortReceived(jsonPayload);
			break;
		default:
			this.handleUnknownCommandReceived(messageData);
			break;
		}
	}
	
	public RealTimeChessGameListener getListener() {
		return listener;
	}

	public void setListener(RealTimeChessGameListener listener) {
		this.listener = listener;
	}

	public void ready() {
		JSONObject json = new JSONObject();

		try {
			json.put(ELO_KEY, this.gameState.getOwner().getRating()
					.getGlicko2Rating());
			json.put(RD_KEY, this.gameState.getOwner().getRating()
					.getRatingDeviation());
		} catch (JSONException e) {
			Log.e(TAG, "Error on creating json object!", e);
		}

		this.sendChessGameMessage(READY, json.toString());
	}
	
	public void newGame(String whitePlayer,String  blackPlayer,int time,int increment,boolean rated){		
		JSONObject json = new JSONObject();

		try {
			json.put(WHITE_PLAYER_KEY, whitePlayer.equals(gameState.getMyId()) ? 0 : 1);
			json.put(BLACK_PLAYER_KEY, blackPlayer.equals(gameState.getMyId()) ? 1 : 0);
			json.put(TIME_KEY, time);
			json.put(INCREMENT_KEY, increment);
			json.put(RATED_KEY, rated);						
		} catch (JSONException e) {
			Log.e(TAG, "Error on creating json object!", e);
		}
		
		this.gameState.setStartGameRequest(new StartGameRequest(whitePlayer,blackPlayer,time,increment,rated));
		this.sendChessGameMessage(NEW_GAME, json.toString());
	}
	
	public void start() {
		this.sendChessGameMessage(START, null);
	}
	
	public void move(String move, int thinkingTime) {
		JSONObject json = new JSONObject();

		try {
			json.put(MOVE_KEY, move);
			json.put(THINKING_TIME_KEY, thinkingTime);
		} catch (JSONException e) {
			Log.e(TAG, "Error on creating json object!", e);
		}

		this.sendChessGameMessage(MOVE, json.toString());
	}

	public void draw() {
		this.sendChessGameMessage(DRAW, null);
	}

	public void flag() {
		this.sendChessGameMessage(FLAG, null);
	}

	public void rematch() {
		this.sendChessGameMessage(REMATCH, null);
	}
	
	public void abort() {
		this.sendChessGameMessage(ABORT, null);
	}
	
	private void sendChessGameMessage(byte command, String jsonPayload) {

		byte[] payload = jsonPayload.getBytes();
		byte[] message = new byte[payload.length + 1];
		message[0] = command;

		for (int i = 1; i < message.length; i++) {
			message[i] = payload[i - 1];
		}

		this.sendMessage(message);
	}

	private JSONObject getPayloadJSON(byte[] payload) {

		if (payload.length > 0) {

			try {
				return new JSONObject(new String(payload));
			} catch (JSONException e) {
				Log.e(TAG, "Invalid payload json game command! ", e);
			}
		}

		return new JSONObject();
	}

	private void handleUnknownCommandReceived(byte[] messageData) {
		Log.d(TAG, "Unknown game command! :" + new String(messageData));
	}

	private void handleRematchReceived(JSONObject jsonPayload) {
		Log.d(TAG, "handleRematchReceived :: " + jsonPayload.toString());

		if (this.listener != null) {
			this.listener.onRematchRecevied();
		}
	}

	private void handleFlagReceived(JSONObject jsonPayload) {
		Log.d(TAG, "handleFlagReceived :: " + jsonPayload.toString());

		if (this.listener != null) {
			this.listener.onFlagRecevied();
		}
	}

	private void handleDrawReceived(JSONObject jsonPayload) {
		Log.d(TAG, "handleDrawReceived :: " + jsonPayload.toString());

		if (this.listener != null) {
			this.listener.onDrawRecevied();
		}
	}

	private void handleResignReceived(JSONObject jsonPayload) {
		Log.d(TAG, "handleResignReceived :: " + jsonPayload.toString());

		if (this.listener != null) {
			this.listener.onResignRecevied();
		}
	}
	
	private void handleAbortReceived(JSONObject jsonPayload) {
		Log.d(TAG, "handleAbortReceived :: " + jsonPayload.toString());

		if (this.listener != null) {
			this.listener.onAbortRecevied();
		}		
	}
	
	private void handleNewGameReceived(JSONObject jsonPayload) {
		Log.d(TAG, "handleNewGameReceived :: " + jsonPayload.toString());

		if (this.listener != null) {
			try {
				this.gameState
						.setStartGameRequest(new StartGameRequest(
								getPlayerId(jsonPayload
										.getInt(WHITE_PLAYER_KEY)),
								getPlayerId(jsonPayload
										.getInt(BLACK_PLAYER_KEY)), jsonPayload
										.getInt(TIME_KEY), jsonPayload
										.getInt(INCREMENT_KEY), jsonPayload
										.getBoolean(RATED_KEY)));
				this.listener.onNewGameRecevied(
						getPlayerId(jsonPayload.getInt(WHITE_PLAYER_KEY)),
						getPlayerId(jsonPayload.getInt(BLACK_PLAYER_KEY)),
						jsonPayload.getInt(TIME_KEY),
						jsonPayload.getInt(INCREMENT_KEY),
						jsonPayload.getBoolean(RATED_KEY));
			} catch (JSONException e) {
				Log.e(TAG, "Invalid start message!", e);
				this.listener.onException("Invalid ready message!");
			}
		}		
	}
	
	private void handleStartReceived(JSONObject jsonPayload) {
		Log.d(TAG, "handleStartReceived :: " + jsonPayload.toString());

		if (this.listener != null) {			
			this.listener.onStartRecevied();			
		}
	}

	private void handleMoveReceived(JSONObject jsonPayload) {
		Log.d(TAG, "handleMoveReceived :: " + jsonPayload.toString());

		if (this.listener != null) {
			try {
				this.listener.onMoveRecevied(jsonPayload.getString(MOVE_KEY),
						jsonPayload.getInt(THINKING_TIME_KEY));
			} catch (JSONException e) {
				Log.e(TAG, "Invalid move message!", e);
				this.listener.onException("Invalid ready message!");
			}
		}
	}

	private void handleReadyReceived(JSONObject jsonPayload) {
		Log.d(TAG, "handleReadyReceived :: " + jsonPayload.toString());

		if (this.listener != null) {
			try {
				double remoteElo = jsonPayload.getDouble(ELO_KEY);
				double remoteRd = jsonPayload.getDouble(RD_KEY);
				this.gameState.setRemotePlayerRating(remoteElo);
				this.gameState.setRemotePlayerRatingDeviation(remoteRd);
				this.listener.onReadyRecevied(remoteElo, remoteRd);
			} catch (JSONException e) {
				Log.e(TAG, "Invalid ready message!", e);
				this.listener.onException("Invalid ready message!");
			}
		}
	}

	private String getPlayerId(int gamePosition) {
		if (gamePosition == 0) {
			return this.gameState.getMyId();
		} else {
			return this.gameState.getRemoteId();
		}
	}

	@Override
	protected String parseMessage(byte[] messageData) {
		String cmd = "UNKNOW";

		switch (messageData[0]) {
		case READY:
			cmd = "READY";
			break;
		case START:
			cmd = "START";
			break;
		case MOVE:
			cmd = "MOVE";
			break;
		case RESIGN:
			cmd = "RESIGN";
			break;
		case DRAW:
			cmd = "DRAW";
			break;
		case FLAG:
			cmd = "FLAG";
			break;
		case REMATCH:
			cmd = "REMATCH";
			break;
		default:
			cmd = "UNKNOW :" + messageData[0];
			break;
		}

		byte[] payload = new byte[messageData.length - 1];
		JSONObject jsonPayload = getPayloadJSON(payload);

		return cmd + " , paylaod:" + jsonPayload.toString();
	}
}