package com.chessyoup.game;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMultiplayer.ReliableMessageSentCallback;
import com.google.android.gms.games.multiplayer.realtime.Room;

public abstract class RealTimeGameClient implements RealTimeMessageReceivedListener, ReliableMessageSentCallback {

    private static final String TAG = "RealTimeGame";

    protected GoogleApiClient apiClient;

    private Room activeRoom;

    public RealTimeGameClient(GoogleApiClient client) {
        this.apiClient = client;
    }

    public Room getRoom() {
        return activeRoom;
    }

    public void setRoom(Room room) {
        this.activeRoom = room;
    }

    public void sendMessage(byte[] messageData) {
        Log.d(TAG, "sendMessage :: size :" + messageData.length + " , message:" + parseMessage(messageData));

        List<Participant> remotes = getRemoteParticipants(this.activeRoom);

        for (Participant p : remotes) {
            Games.RealTimeMultiplayer.sendReliableMessage(apiClient, this, messageData, activeRoom.getRoomId(), p.getParticipantId());            
        }
    }

    private List<Participant> getRemoteParticipants(Room activeRoom) {
        List<Participant> remotes = new ArrayList<Participant>();

        for (Participant p : activeRoom.getParticipants()) {
            if (!p.getParticipantId().equals(this.activeRoom.getParticipantId( Games.Players.getCurrentPlayerId(apiClient) ))) {
                remotes.add(p);
            }
        }

        return remotes;
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
