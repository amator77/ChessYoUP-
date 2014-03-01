package com.chessyoup.game;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMultiplayer.ReliableMessageSentCallback;

public abstract class GameTransport implements RealTimeMessageReceivedListener, ReliableMessageSentCallback {

    private static final String TAG = "GameTransport";

    protected GoogleApiClient apiClient;

    public GameTransport(GoogleApiClient client) {
        this.apiClient = client;
    }

    public void sendMessage(String roomId, String remoteId, byte[] messageData) {
        Log.d(TAG, "sendMessage :: size :" + messageData.length + " , message:" + parseMessage(messageData));
        Games.RealTimeMultiplayer.sendReliableMessage(apiClient, this, messageData, roomId,remoteId); 
    }

    @Override
    public void onRealTimeMessageSent(int statusCode, int tokenId, String recipientParticipantId) {

        Log.d(TAG, "onRealTimeMessageSent :: statusCode:" + statusCode + " ,tokenId:" + tokenId + " ,recipientParticipantId:" + recipientParticipantId);

        switch (statusCode) {

            case GamesStatusCodes.STATUS_OK:
                break;
            case GamesStatusCodes.STATUS_REAL_TIME_MESSAGE_SEND_FAILED:
                break;
            case GamesStatusCodes.STATUS_REAL_TIME_ROOM_NOT_JOINED:
                break;
            default:
                break;
        }
    }

    @Override
    public void onRealTimeMessageReceived(RealTimeMessage message) {
        this.handleMessageReceived(message.getSenderParticipantId(), message.getMessageData());
    }

    protected abstract void handleMessageReceived(String senderId, byte[] messageData);

    protected abstract String parseMessage(byte[] messageData);
}
