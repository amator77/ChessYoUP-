package com.chessyoup.model.pgn;

import com.chessyoup.model.GameTree;

public interface PgnTokenReceiver {

	/** If this method returns false, the object needs a full re-initialization, using clear() and processToken(). */
	public abstract boolean isUpToDate();

	/** Clear object state. */
	public abstract void clear();

	/** Update object state with one token from a PGN game. */
	public abstract void processToken(GameTree.Node node, int type, String token);

	/** Change current move number. */
	public abstract void setCurrent(GameTree.Node node);

}