package com.chessyoup.game.chess;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.chessyoup.game.GameTransport;
import com.chessyoup.game.Util;
import com.google.android.gms.common.api.GoogleApiClient;

public class ChessGameTransport extends GameTransport {

    private static final String TAG = "ChessGameTransport";

    private static final byte READY = 0;
    private static final byte START = 1;
    private static final byte MOVE = 2;
    private static final byte DRAW = 3;
    private static final byte RESIGN = 4;
    private static final byte FLAG = 5;
    private static final byte REMATCH = 6;
    private static final byte ABORT = 7;
    private static final byte CHALLANGE = 8;
    private static final byte CHAT = 9;
    private static final String ELO_KEY = "elo";
    private static final String RD_KEY = "rd";
    private static final String VOLATILITY_KEY = "vol";
    private static final String MOVE_KEY = "m";
    private static final String THINKING_TIME_KEY = "tt";
    private static final String GAME_VARIANT = "gv";
    private static final String GAME_REMATCH = "gr";
    private static final String CHAT_KEY = "c";

    private RealTimeChessGameListener listener;

    public interface RealTimeChessGameListener {

        public void onChallangeRecevied(ChessGameVariant gameVariant,boolean isRematch);

        public void onStartRecevied();

        public void onReadyRecevied(double remoteRating, double remoteRD, double volatility);

        public void onMoveRecevied(String move, int thinkingTime);

        public void onResignRecevied();

        public void onDrawRecevied();

        public void onFlagRecevied();

        public void onRematchRecevied();

        public void onAbortRecevied();

        public void onExitRecevied();
        
        public void onException(String message);

        public void onChatReceived(String message);
    }
    
    private String roomId;
    
    private String remoteId;
    
    public ChessGameTransport( GoogleApiClient client) {
        super(client);
    }
    
    public void configure(String roomId,String remoteId){
    	this.roomId = roomId;
    	this.remoteId = remoteId;
    }
    
    public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	public String getRemoteId() {
		return remoteId;
	}

	public void setRemoteId(String remoteId) {
		this.remoteId = remoteId;
	}

	@Override
    protected void handleMessageReceived(String senderId, byte[] messageData) {
        Log.d(TAG, "handleMessageReceived ::" + parseMessage(messageData));

        byte command = messageData[0];
        byte[] payload = new byte[messageData.length - 1];

        for (int i = 1; i < messageData.length; i++) {
            payload[i - 1] = messageData[i];
        }

        JSONObject jsonPayload = getPayloadJSON(payload);

        switch (command) {
            case READY:
                this.handleReadyReceived(jsonPayload);
                break;
            case CHALLANGE:
                this.handleChallangeReceived(jsonPayload);
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
            case CHAT:
                this.handleChatReceived(jsonPayload);
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

    public void ready(ChessGamePlayer player) {
        JSONObject json = new JSONObject();

        try {
            json.put(ELO_KEY, player.getRating());
            json.put(RD_KEY, player.getRatingDeviation());
            json.put(VOLATILITY_KEY,player.getVolatility());
        } catch (JSONException e) {
            Log.e(TAG, "Error on creating json object!", e);
        }

        this.sendChessGameMessage(READY, json.toString());
    }

    public void sendChallange(int gameVariant , boolean isRematch) {
        JSONObject json = new JSONObject();        

        try {
            json.put(GAME_REMATCH, isRematch);
            json.put(GAME_VARIANT, gameVariant);
        } catch (JSONException e) {
            Log.e(TAG, "Error on creating json object!", e);
        }

        this.sendChessGameMessage(CHALLANGE, json.toString());
    }

    public void sendChallange(int gameType, int time, int increment, int moves, boolean isRated, boolean isWhite) {
        this.sendChallange(Util.getGameVariant(gameType, time, increment, moves, isRated, isWhite),false);                
    }

    public void sendChatMessage(String message) {
        JSONObject json = new JSONObject();

        try {
            json.put(CHAT_KEY, message);
        } catch (JSONException e) {
            Log.e(TAG, "Error on creating json object!", e);
        }

        this.sendChessGameMessage(CHAT, json.toString());
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

    public void resign() {
        this.sendChessGameMessage(RESIGN, null);
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
    
    public void remoteLeft(){
        if( this.listener != null ){
            this.listener.onExitRecevied();
        }
    }
    
    private void sendChessGameMessage(byte command, String jsonPayload) {

        Log.d(TAG, "sendChessGameMessage :: command :" + command + " json :" + jsonPayload);

        byte[] payload = jsonPayload != null ? jsonPayload.getBytes() : new byte[0];
        byte[] message = new byte[payload.length + 1];
        message[0] = command;

        for (int i = 1; i < message.length; i++) {
            message[i] = payload[i - 1];
        }

        this.sendMessage(this.roomId,this.remoteId, message);
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

    private void handleChatReceived(JSONObject jsonPayload) {
        Log.d(TAG, "handleChatReceived :: " + jsonPayload.toString());

        if (this.listener != null) {
            try {
                this.listener.onChatReceived(jsonPayload.getString(CHAT_KEY));
            } catch (JSONException e) {
                Log.e(TAG, "Invalid chat message!", e);
                this.listener.onException("Invalid chat message!");
            }
        }
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

    private void handleChallangeReceived(JSONObject jsonPayload) {
        Log.d(TAG, "handleChallangeReceived :: " + jsonPayload.toString());

        if (this.listener != null) {
            try {
                ChessGameVariant gameVariant = Util.getGameVariant(Integer.parseInt(jsonPayload.getString(GAME_VARIANT)));
                this.listener.onChallangeRecevied(gameVariant,Boolean.parseBoolean(jsonPayload.getString(GAME_REMATCH)));
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
                this.listener.onMoveRecevied(jsonPayload.getString(MOVE_KEY), jsonPayload.getInt(THINKING_TIME_KEY));
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
                double volatility = jsonPayload.getDouble(VOLATILITY_KEY);
                this.listener.onReadyRecevied(remoteElo, remoteRd, volatility);
            } catch (JSONException e) {
                Log.e(TAG, "Invalid ready message!", e);
                this.listener.onException("Invalid ready message!");
            }
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
            case CHALLANGE:
                cmd = "CHALLANGE";
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

        for (int i = 1; i < messageData.length; i++) {
            payload[i - 1] = messageData[i];
        }

        JSONObject jsonPayload = getPayloadJSON(payload);

        return cmd + " , paylaod:" + jsonPayload.toString();
    }

	@Override
	public String toString() {
		return "ChessGameTransport [listener=" + listener + ", roomId="
				+ roomId + ", remoteId=" + remoteId + "]";
	}
}
