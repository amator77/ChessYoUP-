package com.chessyoup.chessboard;

import java.util.ArrayList;

import com.chessyoup.model.Move;
import com.chessyoup.model.Position;

public interface ChessboardUIInterface {
    
    public void setPosition(Position pos, String variantInfo, ArrayList<Move> variantMoves);

    public void setSelection(int sq);

    /** Set the status text. */
    public void setStatus(ChessboardStatus status);

    /** Update the list of moves. */
    public void moveListUpdated();

    /** Ask what to promote a pawn to. Should call reportPromotePiece() when done. */
    public void requestPromotePiece();

    /** Run code on the GUI thread. */
    public void runOnUIThread(Runnable runnable);

    /** Report that user attempted to make an invalid move. */
    public void reportInvalidMove(Move m);

    /** Report remaining thinking time to GUI. */
    public void setRemainingTime(int wTime, int bTime, int nextUpdate);

    /** Report a move made that is a candidate for GUI animation. */
    public void setAnimMove(Position sourcePos, Move move, boolean forward);

    /** Get the default player name. */
    public String whitePlayerName();
    
    /** Get the default player name. */
    public String blackPlayerName();

    /** Return true if only main-line moves are to be kept. */
    public boolean discardVariations();
    
	public void localMoveMade(Move m);
}
