package com.chessyoup.chessboard;

import com.chessyoup.model.Game;

public class ChessboardStatus {
	
    public Game.GameState state = Game.GameState.ALIVE;
    
    public int moveNr = 0;
    
    /** Move required to claim draw, or empty string. */
    public String drawInfo = "";
    
    public boolean white = false;
    
    public boolean ponder = false;
    
    public boolean thinking = false;
    
    public boolean analyzing = false;
}

