package com.chessyoup.game;

public class GameVariant {
	
	public int gameType;
	
	public int time;
	
	public int increment;
	
	public int moves;
	
	public boolean rated;
	
	public boolean white;

	public int getGameType() {
		return gameType;
	}

	public void setGameType(int gameType) {
		this.gameType = gameType;
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

	public int getMoves() {
		return moves;
	}

	public void setMoves(int moves) {
		this.moves = moves;
	}

	public boolean isRated() {
		return rated;
	}

	public void setRated(boolean rated) {
		this.rated = rated;
	}

	public boolean isWhite() {
		return white;
	}

	public void setWhite(boolean white) {
		this.white = white;
	}

	@Override
	public String toString() {
		return "GameVariant [gameType=" + gameType + ", time=" + time
				+ ", increment=" + increment + ", moves=" + moves + ", rated="
				+ rated + ", white=" + white + "]";
	}
}
