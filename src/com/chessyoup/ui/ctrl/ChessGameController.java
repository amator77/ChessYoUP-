package com.chessyoup.ui.ctrl;

import java.util.LinkedList;
import java.util.List;

import android.util.Log;

import com.chessyoup.R;
import com.chessyoup.game.Util;
import com.chessyoup.game.chess.ChessGamePlayer;
import com.chessyoup.game.chess.ChessGameState;
import com.chessyoup.game.chess.ChessGameTransport;
import com.chessyoup.game.chess.ChessGameVariant;
import com.chessyoup.ui.ChessYoUpActivity;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.PageDirection;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardScoreBuffer;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.Leaderboards;
import com.google.android.gms.games.leaderboard.Leaderboards.LoadScoresResult;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;

public class ChessGameController {

    private static final String TAG = "ChessGameController";

    private static ChessGameController instance = new ChessGameController();

    private List<ChessGameState> models;

    private List<ChessGamePlayer> knownPlayers;

    private ChessYoUpActivity mainActivity;

    private ChessGamePlayer localPlayer;

    private GoogleApiClient apiClient;

    private int scorePagesLoaded = 0;

    private ChessGameController() {
        this.models = new LinkedList<ChessGameState>();
        this.knownPlayers = new LinkedList<ChessGamePlayer>();
    }

    public void createRoom(String remotePlayer, int gameVariant, RoomEventsAdapter adapter) {
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(adapter);
        rtmConfigBuilder.addPlayersToInvite(new String[] {remotePlayer});
        rtmConfigBuilder.setVariant(gameVariant);
        ChessGameTransport gameTransport = new ChessGameTransport(apiClient);
        adapter.setGameTransport(gameTransport);
        rtmConfigBuilder.setMessageReceivedListener(gameTransport);
        rtmConfigBuilder.setRoomStatusUpdateListener(adapter);
        Games.RealTimeMultiplayer.create(this.apiClient, rtmConfigBuilder.build());
    }

    public void joinRoom(String invitationId, RoomEventsAdapter adapter) {
        RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(adapter);
        ChessGameTransport gameTransport = new ChessGameTransport(this.apiClient);
        adapter.setGameTransport(gameTransport);
        roomConfigBuilder.setInvitationIdToAccept(invitationId).setMessageReceivedListener(gameTransport).setRoomStatusUpdateListener(adapter);
        Games.RealTimeMultiplayer.join(this.apiClient, roomConfigBuilder.build());
    }

    public ChessGameState newGame(Room gameRoom, ChessGameTransport gameTransport, ChessGamePlayer remotePlayer) {
        ChessGameState model = new ChessGameState();
        ChessGameVariant gameVariant = Util.getGameVariant(gameRoom.getVariant());
        model.setRoom(gameRoom);
        model.setLocalPlayer(localPlayer);
        model.setRemotePlayer(remotePlayer);
        model.setGameVariant(gameVariant);
        model.setGameTransport(gameTransport);

        if (model.isLocalPlayerRoomCreator()) {
            if (gameVariant.isWhite()) {
                model.setWhitePlayer(localPlayer);
                model.setBlackPlayer(remotePlayer);
            } else {
                model.setWhitePlayer(remotePlayer);
                model.setBlackPlayer(localPlayer);
            }
        } else {
            if (gameVariant.isWhite()) {
                model.setWhitePlayer(remotePlayer);
                model.setBlackPlayer(localPlayer);
            } else {
                model.setWhitePlayer(remotePlayer);
                model.setBlackPlayer(localPlayer);
            }
        }

        model.newGame();
        this.models.add(model);
        return model;
    }

    public void leaveRoom(String roomId, RoomEventsAdapter adapter) {
        Log.d("ChessGameController", "leaveRoom :: " + roomId);

        if (adapter != null) {
            Games.RealTimeMultiplayer.leave(this.apiClient, adapter, roomId);
        } else {
            Games.RealTimeMultiplayer.leave(this.apiClient, new RoomUpdateListener() {

                @Override
                public void onRoomCreated(int statusCode, Room room) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onRoomConnected(int statusCode, Room room) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onLeftRoom(int statusCode, String roomId) {
                    Log.d("onLeftRoom :: ", "statusCode: " + statusCode + " , roomId: " + roomId);

                }

                @Override
                public void onJoinedRoom(int statusCode, Room room) {
                    // TODO Auto-generated method stub

                }
            }, roomId);
        }
    }

    public ChessGameState findChessModelByRoomId(String roomId) {

        for (ChessGameState model : models) {
            if (model.getRoom() != null && model.getRoom().getRoomId().equals(roomId)) {
                return model;
            }
        }

        return null;
    }

    public void setMainActivity(ChessYoUpActivity activity) {
        this.mainActivity = activity;
        this.apiClient = this.mainActivity.getGameHelper().getApiClient();
    }

    public static ChessGameController getController() {
        return ChessGameController.instance;
    }

    public void setLocalPlayer(ChessGamePlayer localPlayer) {
        this.localPlayer = localPlayer;
    }

    public ChessGamePlayer getLocalPlayer() {
        return localPlayer;
    }

    public void loadPlayersRating(final boolean forceReload) {
        this.knownPlayers.clear();

        PendingResult<Leaderboards.LoadScoresResult> topScores =
                Games.Leaderboards.loadTopScores(apiClient, mainActivity.getResources().getString(R.string.leaderboard_top_rating), LeaderboardVariant.TIME_SPAN_ALL_TIME,
                        LeaderboardVariant.COLLECTION_SOCIAL, 25, forceReload);

        topScores.setResultCallback(new ResultCallback<Leaderboards.LoadScoresResult>() {

            @Override
            public void onResult(LoadScoresResult result) {

                if (result.getStatus().isSuccess()) {
                    LeaderboardScoreBuffer buffer = result.getScores();

                    for (int i = 0; i < buffer.getCount(); i++) {
                        knownPlayers.add(scoreToPlayer(buffer.get(i)));
                    }

                    if (buffer.getCount() == 25) {
                        loadMoreScores(result, new Runnable() {

                            @Override
                            public void run() {
                                scorePagesLoaded = 0;
                                Log.d(TAG, "loadPlayersRating topScoreFinish :: " + knownPlayers);

                                PendingResult<Leaderboards.LoadScoresResult> lowScores =
                                        Games.Leaderboards.loadTopScores(apiClient, mainActivity.getResources().getString(R.string.leaderboard_low_rating), LeaderboardVariant.TIME_SPAN_ALL_TIME,
                                                LeaderboardVariant.COLLECTION_SOCIAL, 25, forceReload);

                                lowScores.setResultCallback(new ResultCallback<Leaderboards.LoadScoresResult>() {

                                    @Override
                                    public void onResult(LoadScoresResult result) {

                                        if (result.getStatus().isSuccess()) {
                                            LeaderboardScoreBuffer buffer = result.getScores();

                                            for (int i = 0; i < buffer.getCount(); i++) {
                                                ChessGamePlayer chessPlayer = getChessPlayer(buffer.get(i).getScoreHolder());
                                                chessPlayer.setLowScore(buffer.get(i).getRawScore());
                                            }
                                            
                                            if (buffer.getCount() == 25) {
                                                loadMoreScores(result, new Runnable() {
                                                    
                                                    @Override
                                                    public void run() {
                                                        Log.d(TAG, "loadPlayersRating lowScoreFinish :: " + knownPlayers);                                                        
                                                    }
                                                });
                                            }
                                        }
                                    }
                                });
                            }
                        });
                    }
                }
            }
        });
    }

    private void loadMoreScores(LoadScoresResult result, final Runnable runOnFinish) {

        PendingResult<Leaderboards.LoadScoresResult> scores = Games.Leaderboards.loadMoreScores(apiClient, result.getScores(), 25, PageDirection.NEXT);

        scores.setResultCallback(new ResultCallback<Leaderboards.LoadScoresResult>() {

            @Override
            public void onResult(LoadScoresResult result) {
                scorePagesLoaded++;

                if (result.getStatus().isSuccess()) {
                    LeaderboardScoreBuffer buffer = result.getScores();

                    for (int i = 0; i < buffer.getCount(); i++) {
                        ChessGamePlayer chessPlayer = getChessPlayer(buffer.get(i).getScoreHolder());
                        
                        if( chessPlayer == null ){
                            knownPlayers.add(scoreToPlayer(buffer.get(i)));
                        }
                        else{
                            chessPlayer.setLowScore(buffer.get(i).getRawScore());
                        }
                    }

                    if (buffer.getCount() == 25) {

                        if (scorePagesLoaded < 10) {
                            loadMoreScores(result, runOnFinish);
                        } else {
                            if (runOnFinish != null) {
                                runOnFinish.run();
                            }
                        }
                    } else {
                        if (runOnFinish != null) {
                            runOnFinish.run();
                        }
                    }
                }
            }
        });
    }

    private ChessGamePlayer scoreToPlayer(LeaderboardScore leaderboardScore) {

        ChessGamePlayer chessPlayer = new ChessGamePlayer();
        chessPlayer.setPlayer(leaderboardScore.getScoreHolder());
        chessPlayer.setTopScore(leaderboardScore.getRawScore());

        return chessPlayer;
    }

    private ChessGamePlayer getChessPlayer(Player player) {

        for (ChessGamePlayer chessPlayer : this.knownPlayers) {
            if (player.getPlayerId().equals(chessPlayer.getPlayer().getPlayerId())) {
                return chessPlayer;
            }
        }

        return null;
    }
}
