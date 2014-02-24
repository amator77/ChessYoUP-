package com.chessyoup.game.chess;

import com.chessyoup.game.GameVariant;

public class ChessGameVariant extends GameVariant {
		
	public int time;
	
	public int increment;
	
	public int moves;
			
	public boolean white;

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

	public boolean isWhite() {
		return white;
	}

	public void setWhite(boolean white) {
		this.white = white;
	}

    @Override
    public int toVariant() {
        StringBuffer sb = new StringBuffer(String.valueOf(time/1000));
        sb.append(getType());
        sb.append(fill(String.valueOf(time),3));
        sb.append(fill(String.valueOf(increment),2));
        sb.append(fill(String.valueOf(moves),2));
        sb.append(isRated() ? "1" : "0");
        sb.append(white ? "1" : "0");
        return Integer.parseInt(sb.toString());        
    }
    
    private String fill(String value,int size){
        StringBuffer sb = new StringBuffer();
        
        for( int i = 0; i < size ; i++){
            if( (sb.length() + value.length())  < size ){
                sb.append("0");             
            }
        }
        
        return sb.append(value).toString();
    }
}
