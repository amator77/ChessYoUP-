package com.chessyoup.game;

public class StartGameRequest {
	
	private boolean rated;
	
	private String whitePlayerId;
	
	private String blackPlayerId;
	
	private int time;
	
	private int increment;
	
	
	public StartGameRequest(String whitePlayerId,String blackPlayerId,int time , int increment,boolean rated){
		this.whitePlayerId = whitePlayerId;
		this.blackPlayerId = blackPlayerId;
		this.time = time;
		this.increment = increment;
		this.rated = rated;
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

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public int getIncrement() {
		return increment;
	}

	public void setIncrement(int increment) {
		this.increment = increment;
	}

	public boolean isRated() {
		return rated;
	}

	public void setRated(boolean rated) {
		this.rated = rated;
	}

	@Override
	public String toString() {
		return "StartGameRequest [rated=" + rated + ", whitePlayerId="
				+ whitePlayerId + ", blackPlayerId=" + blackPlayerId
				+ ", time=" + time + ", increment=" + increment + "]";
	}
}
