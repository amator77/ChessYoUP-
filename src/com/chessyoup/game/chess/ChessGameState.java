package com.chessyoup.game.chess;

import java.util.LinkedList;
import java.util.List;

import com.chessyoup.game.GamePlayer;
import com.chessyoup.game.GameState;
import com.chessyoup.model.Game;
import com.google.android.gms.games.multiplayer.realtime.Room;

public class ChessGameState implements GameState {
    
	private Room room;
	
	private Game game;
	
	private ChessGameVariant gameVariant;
	
	private ChessGamePlayer localPlayer;
	
	private ChessGamePlayer remotePlayer;
	
	private ChessGamePlayer whitePlayer;

    private ChessGamePlayer blackPlayer;
    
    private ChessGameTransport gameTransport;
    
    private List<Game> gameHistory;
    
    private String lastWhitePlayerId = null;
    
    private boolean ready;
    
    public ChessGameState(){
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
    
    public boolean isLocalPlayerRoomCreator(){
    	if( room != null ){
    		if( this.localPlayer != null ){
    			return room.getCreatorId().equals(this.localPlayer.getParticipant().getParticipantId());
    		}
    	}
    	
    	return false;
    }
    
    public ChessGameTransport getGameTransport() {
		return gameTransport;
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
        return this.remotePlayer;
    }

    public void setRemotePlayer(ChessGamePlayer remotePlayer) {
       this.remotePlayer = remotePlayer;
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
        return this.gameVariant;
    }

	public void setGameVariant(ChessGameVariant gameVariant) {
		this.gameVariant = gameVariant;
    }

    public void setRemoteRating(double remoteElo, double remoteRd, double volatility) {
        this.getRemotePlayer().setLiveRating(remoteElo);
        this.getRemotePlayer().setRatingDeviation(remoteRd);
        this.getRemotePlayer().setVolatility(volatility);
    }

    public void switchSides() {
        this.gameVariant.setWhite(this.gameVariant.isWhite() ? false : true);
        
        ChessGamePlayer player = this.whitePlayer;
        this.whitePlayer = this.blackPlayer;
        this.blackPlayer = player;                
    }

	@Override
	public ChessGamePlayer getLocalPlayer() {
		return this.localPlayer;
	}

	@Override
	public Room getRoom() {		
		return this.room;
	}

	@Override
	public boolean isReady() {
		return this.ready;
	}

	public void setRoom(Room room) {
		this.room = room;
	}

	public void setGame(Game game) {
		this.game = game;
	}

	public void setLocalPlayer(ChessGamePlayer localPlayer) {
		this.localPlayer = localPlayer;
	}

	public void setGameTransport(ChessGameTransport gameTransport) {
		this.gameTransport = gameTransport;
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

	@Override
	public String toString() {
		return "ChessGameModel [room=" + room + ", game=" + game
				+ ", gameVariant=" + gameVariant + ", localPlayer="
				+ localPlayer + ", remotePlayer=" + remotePlayer
				+ ", whitePlayer=" + whitePlayer + ", blackPlayer="
				+ blackPlayer + ", gameTransport=" + gameTransport
				+ ", gameHistory=" + gameHistory + ", lastWhitePlayerId="
				+ lastWhitePlayerId + ", ready=" + ready + "]";
	}
}
