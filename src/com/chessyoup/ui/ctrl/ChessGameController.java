package com.chessyoup.ui.ctrl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.chessyoup.game.Util;
import com.chessyoup.game.chess.ChessGameModel;
import com.chessyoup.game.chess.ChessGamePlayer;
import com.chessyoup.game.chess.ChessGameVariant;
import com.chessyoup.game.chess.ChessRealTimeGameClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;

public class ChessGameController{
    
    private static ChessGameController instance = new ChessGameController();
    
    private List<ChessGameModel> models;
    
    private Map<String,ChessRealTimeGameClient> chessClients;
    
    private ChessGamePlayer localPlayer;
    
    private RoomController roomController;
    
    private RoomStatusUpdateListener roomStatusUpdateListener;        
    
    private ChessGameController(){
        this.models = new LinkedList<ChessGameModel>();
        this.chessClients = new HashMap<String, ChessRealTimeGameClient>();
    }
              
    public ChessGameModel newChessGameModel(Room gameRoom, ChessGamePlayer remotePlayer , boolean isOwner){
        ChessGameModel model = new ChessGameModel();
        ChessGameVariant gameVariant = Util.getGameVariant(gameRoom.getVariant());
        model.setRoom(gameRoom);
        model.setRemotePlayer(remotePlayer);        
        model.setGameVariant(gameVariant);
        model.setLocalPlayerOwner(isOwner);
                
        if (gameVariant.isWhite()) {
            model.setWhitePlayer(this.getLocalPlayer());
            model.setBlackPlayer(remotePlayer);              
        }
        else{
            model.setWhitePlayer(remotePlayer);
            model.setBlackPlayer(this.getLocalPlayer());
        }
        
        model.newGame();
        this.models.add(model);        
        return model;
    }
    
    public ChessRealTimeGameClient getChessClientByRoomId(String roomId){               
        return this.chessClients.get(roomId);
    }
    
    public void setRoomChessClientByRoomId(String roomId,ChessRealTimeGameClient client){               
        this.chessClients.put(roomId, client);
    }
    
    public void createRoom(String remotePlayer, int gameVariant){       
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(roomController);
        rtmConfigBuilder.addPlayersToInvite(new String[] {remotePlayer});
        rtmConfigBuilder.setVariant(gameVariant);
        ChessRealTimeGameClient chessClient = new ChessRealTimeGameClient(GoogleAPIController.getInstance(null).getApiClient());
        roomController.registerChessClient(chessClient);
        rtmConfigBuilder.setMessageReceivedListener(chessClient);
        rtmConfigBuilder.setRoomStatusUpdateListener(roomStatusUpdateListener);
        Games.RealTimeMultiplayer.create(GoogleAPIController.getInstance(null).getApiClient(), rtmConfigBuilder.build());               
    }
    
    public void joinRoom(String invitationId){
        RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(roomController);
        ChessRealTimeGameClient chessClient = new ChessRealTimeGameClient(GoogleAPIController.getInstance(null).getApiClient());
        roomController.registerChessClient(chessClient);
        roomConfigBuilder.setInvitationIdToAccept(invitationId).setMessageReceivedListener(chessClient).setRoomStatusUpdateListener(roomStatusUpdateListener);
        Games.RealTimeMultiplayer.join(GoogleAPIController.getInstance(null).getApiClient(), roomConfigBuilder.build());        
    }
    
    public void leaveRoom(String roomId){
        Log.d("", "leaveRoom ::"+roomId);
        Games.RealTimeMultiplayer.leave(GoogleAPIController.getInstance(null).getApiClient(),roomController,roomId);        
    }
    
    public ChessGameModel findChessModelByRoomId(String roomId){
        
        for( ChessGameModel model : models ){
            if( model.getRoom() != null && model.getRoom().getRoomId().equals(roomId)){
                return model;
            }
        }
        
        return null;
    }
    
    public static ChessGameController getController(){
        return ChessGameController.instance;
    }        
    
        
    public ChessGamePlayer getLocalPlayer(){
        return (ChessGamePlayer)this.localPlayer;
    }

    public void setRoomController(RoomController roomController) {
        this.roomController = roomController;        
    }

    public void setLocalPlayer(ChessGamePlayer localPlayer) {
        this.localPlayer = localPlayer;        
    }
}
