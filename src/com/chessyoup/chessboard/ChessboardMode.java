package com.chessyoup.chessboard;

public class ChessboardMode {
    private final int modeNr;
    
    public static final int TWO_PLAYERS_WHITE_REMOTE   = 1;
    public static final int TWO_PLAYERS_BLACK_REMOTE   = 2;
    public static final int ANALYSIS      = 3;
    public static final int EDIT_GAME     = 4;
        

    public ChessboardMode(int modeNr) {
        this.modeNr = modeNr;
    }

    public int getModeNr() {
        return modeNr;
    }

    /** Return true if white side is controlled by a human. */
    public final boolean playerWhite() {
    	System.out.println(modeNr);
        switch (modeNr) {
        case ANALYSIS:
        case EDIT_GAME:
        case TWO_PLAYERS_BLACK_REMOTE:
            return true;
        default:
            return false;
        }
    }

    /** Return true if black side is controlled by a human. */
    public final boolean playerBlack() {
        switch (modeNr) {
        case ANALYSIS:
        case EDIT_GAME:
        case TWO_PLAYERS_WHITE_REMOTE:	
            return true;
        default:
            return false;
        }
    }

    public final boolean analysisMode() {
        return modeNr == ANALYSIS;
    }
    
    /** Return true if it is a local turn to move. */
    public final boolean localTurn(boolean whiteMove) {
        return whiteMove ? playerWhite() : playerBlack();
    }

    /** Return true if the clocks are running. */
    public final boolean clocksActive() {
        switch (modeNr) {
        case TWO_PLAYERS_WHITE_REMOTE:
        case TWO_PLAYERS_BLACK_REMOTE:
            return true;
        default:
            return false;    
        }
    }

    @Override
    public boolean equals(Object o) {
        if ((o == null) || (o.getClass() != this.getClass()))
            return false;
        ChessboardMode other = (ChessboardMode)o;
        return modeNr == other.modeNr;
    }

    @Override
    public int hashCode() {
        return modeNr;
    }
}
