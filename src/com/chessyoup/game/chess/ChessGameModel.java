package com.chessyoup.game.chess;

import java.util.LinkedList;
import java.util.List;

import com.chessyoup.game.GameModel;
import com.chessyoup.game.GamePlayer;
import com.chessyoup.model.Game;

public class ChessGameModel extends GameModel {
    
    private Game game;
    
    private List<Game> gameHistory;
    
    private ChessGamePlayer whitePlayer;

    private ChessGamePlayer blackPlayer;
    
    private String lastWhitePlayerId = null;
    
    public ChessGameModel(){
        gameHistory = new LinkedList<Game>();
    }
    
    public void newGame(){
        if( game != null ){
            gameHistory.add(this.game);
        }
        
        ChessGameVariant variant = getGameVariant();
        this.game = new Game(null, variant.getTime(), variant.getMoves(), variant.getIncrement());
        this.game.setRated(variant.isRated());
    }
            
    public Game getGame() {
        return game;
    }
    
    public List<Game> getGameHistory() {
        return gameHistory;
    }

    public void setGameHistory(List<Game> gameHistory) {
        this.gameHistory = gameHistory;
    }

    public GamePlayer getWhitePlayer() {
        return whitePlayer;
    }
    
    public ChessGamePlayer getRemotePlayer() {
        return (ChessGamePlayer)super.getRemotePlayer();
    }

    public void setRemotePlayer(ChessGamePlayer remotePlayer) {
        this.setRemotePlayer(remotePlayer);
    }
    
    public void setWhitePlayer(ChessGamePlayer whitePlayer) {
        this.whitePlayer = whitePlayer;
    }

    public GamePlayer getBlackPlayer() {
        return blackPlayer;
    }

    public void setBlackPlayer(ChessGamePlayer blackPlayer) {
        this.blackPlayer = blackPlayer;
    }

    public String getLastWhitePlayerId() {
        return lastWhitePlayerId;
    }

    public void setLastWhitePlayerId(String lastWhitePlayerId) {
        this.lastWhitePlayerId = lastWhitePlayerId;
    }

    public ChessGameVariant getGameVariant() {
        return (ChessGameVariant)super.getGameVariant();
    }

    public void setGameVariant(ChessGameVariant gameVariant) {
        this.setGameVariant(gameVariant);
    }

    public void setRemoteRating(double remoteElo, double remoteRd, double volatility) {
        this.getRemotePlayer().setRating(remoteElo);
        this.getRemotePlayer().setRatingDeviation(remoteRd);
        this.getRemotePlayer().setVolatility(volatility);
    }

    public void switchSides() {
        ChessGameVariant gv = getGameVariant();
        gv.setWhite(gv.isWhite() ? false : true);
        
        ChessGamePlayer player = this.whitePlayer;
        this.whitePlayer = this.blackPlayer;
        this.blackPlayer = player;                
    }
}
