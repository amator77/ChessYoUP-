package com.chessyoup.game;

public class StartGameRequest {
			
	private String whitePlayerId;
	
	private String blackPlayerId;
	
	private GameVariant gameVariant;	
	
	public StartGameRequest(String whitePlayerId,String blackPlayerId,int gameVariant){
		this.whitePlayerId = whitePlayerId;
		this.blackPlayerId = blackPlayerId;
		this.gameVariant = Util.getGameVariant(gameVariant);
	}
	
	public StartGameRequest(String whitePlayerId,String blackPlayerId,GameVariant gameVariant){
		this.whitePlayerId = whitePlayerId;
		this.blackPlayerId = blackPlayerId;
		this.gameVariant = gameVariant;
	}
	
	public String getWhitePlayerId() {
		return whitePlayerId;
	}

	public void setWhitePlayerId(String whitePlayerId) {
		this.whitePlayerId = whitePlayerId;
	}

	public String getBlackPlayerId() {
		return blackPlayerId;
	}

	public void setBlackPlayerId(String blackPlayerId) {
		this.blackPlayerId = blackPlayerId;
	}

	public GameVariant getGameVariant() {
		return gameVariant;
	}

	public void setGameVariant(GameVariant gameVariant) {
		this.gameVariant = gameVariant;
	}

	@Override
	public String toString() {
		return "StartGameRequest [whitePlayerId=" + whitePlayerId
				+ ", blackPlayerId=" + blackPlayerId + ", gameVariant="
				+ gameVariant + "]";
	}
}
