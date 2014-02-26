package com.chessyoup.game.chess;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;

import com.chessyoup.game.GameController;
import com.chessyoup.game.GameHelper;
import com.chessyoup.game.Util;
import com.chessyoup.game.GameHelper.GameHelperListener;
import com.google.android.gms.games.multiplayer.realtime.Room;

public class ChessGameController extends GameController {

    private static ChessGameController instance = new ChessGameController();
    
    private ChessGameController(){
        this.models = new LinkedList<ChessGameModel>();        
    }
    
    public static ChessGameController getController(){
        return ChessGameController.instance;
    }
    
    private List<ChessGameModel> models;
    
    public void initialize(Activity activity, GameHelperListener listener) {

        if (!this.initilized) {
            this.activity = activity;
            this.mHelper = new GameHelper(activity);
            mHelper.enableDebugLog(mDebugLog, mDebugTag);
            mHelper.setup(listener, mRequestedClients, mAdditionalScopes);
            this.realTimeGame = new ChessRealTimeGameClient(getGamesClient());
            this.initilized = true;
        }
    }
    
    public ChessGamePlayer getLocalPlayer(){
        return (ChessGamePlayer)this.localPlayer;
    }
    
    public ChessRealTimeGameClient getRealTimeGameClient(){
        return (ChessRealTimeGameClient)this.realTimeGame;
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
    
    public ChessGameModel findChessModelByRoomId(String roomId){
        
        for( ChessGameModel model : models ){
            if( model.getRoom() != null && model.getRoom().getRoomId().equals(roomId)){
                return model;
            }
        }
        
        return null;
    }
}
