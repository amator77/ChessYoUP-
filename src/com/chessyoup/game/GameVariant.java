package com.chessyoup.game;

public abstract class GameVariant {        
    
    private int type;        
    
    private boolean rated;

    public abstract int toVariant();
    
    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isRated() {
        return rated;
    }

    public void setRated(boolean rated) {
        this.rated = rated;
    }    
}
