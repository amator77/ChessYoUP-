package com.chessyoup.chessboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.chessyoup.model.ChessParseError;
import com.chessyoup.model.Game;
import com.chessyoup.model.Game.GameState;
import com.chessyoup.model.GameTree.Node;
import com.chessyoup.model.Move;
import com.chessyoup.model.MoveGen;
import com.chessyoup.model.Piece;
import com.chessyoup.model.Position;
import com.chessyoup.model.TextIO;
import com.chessyoup.model.pgn.PGNOptions;
import com.chessyoup.model.pgn.PgnTokenReceiver;

public class ChessboardController {

	private PgnTokenReceiver gameTextListener = null;

	private Game game = null;

	private ChessboardUIInterface gui;
	
	private ChessboardMode gameMode;

	private PGNOptions pgnOptions;

	private int timeControl;

	private int movesPerSession;

	private int timeIncrement;

	private boolean guiPaused = false;

	private Move promoteMove;
	
	private boolean drawRequested;

	private boolean abortRequested;
	
	public ChessboardController(ChessboardUIInterface gui,
			PgnTokenReceiver gameTextListener, PGNOptions options) {
		this.gui = gui;
		this.gameTextListener = gameTextListener;
		this.gameMode = new ChessboardMode(ChessboardMode.ANALYSIS);
		this.pgnOptions = options;
		this.timeControl = 120000;
		this.movesPerSession = 60;
		this.timeIncrement = 0;
	}

	/** Start a new game. */
	public void newGame(ChessboardMode gameMode) {
		this.gameMode = gameMode;
		this.game = new Game(gameTextListener, timeControl, movesPerSession,
				timeIncrement);
				
		game.currPos().whiteMove = true;
		
		updateGUI();
		setPlayerNames(game);
		updateGameMode();
	}

	/** Start playing a new game. Should be called after newGame(). */
	public final synchronized void startGame() {
		setSelection();
		updateGUI();
		updateGameMode();
	}

	/** Set time control parameters. */
	public final synchronized void setTimeLimit(int time, int moves, int inc) {
		timeControl = time;
		movesPerSession = moves;
		timeIncrement = inc;
		if (game != null)
			game.timeController.setTimeControl(timeControl, movesPerSession,
					timeIncrement);
	}

	public Game getGame() {
		return this.game;
	}

	/**
	 * @return Array containing time control, moves per session and time
	 *         increment.
	 */
	public final int[] getTimeLimit() {
		int[] ret = new int[3];
		ret[0] = timeControl;
		ret[1] = movesPerSession;
		ret[2] = timeIncrement;
		return ret;
	}

	/** The chess clocks are stopped when the GUI is paused. */
	public final synchronized void setGuiPaused(boolean paused) {
		guiPaused = paused;
		updateGameMode();
	}

	/** Set game mode. */
	public final synchronized void setGameMode(ChessboardMode newMode) {
		if (!gameMode.equals(newMode)) {
			gameMode = newMode;
			if (!gameMode.playerWhite() || !gameMode.playerBlack())
				setPlayerNames(game); // If computer player involved, set player
										// names
			updateGameMode();
			updateGUI();
		}
	}

	public ChessboardMode getGameMode() {
		return gameMode;
	}
	
	
	
	public PGNOptions getPgnOptions() {
		return pgnOptions;
	}

	/** Return true if game mode is analysis. */
	public final boolean analysisMode() {
		return gameMode.analysisMode();
	}

	/** Notify controller that preferences has changed. */
	public final synchronized void prefsChanged(boolean translateMoves) {
		if (game == null)
			translateMoves = false;
		if (translateMoves)
			game.tree.translateMoves();
		updateMoveList();

		if (translateMoves)
			updateGUI();
	}

	/** De-serialize from byte array. */
	public final synchronized void fromByteArray(byte[] data) {
		game.fromByteArray(data);
		game.tree.translateMoves();
	}

	/** Serialize to byte array. */
	public final synchronized byte[] toByteArray() {
		return game.tree.toByteArray();
	}

	/** Return FEN string corresponding to a current position. */
	public final synchronized String getFEN() {
		return TextIO.toFEN(game.tree.currentPos);
	}

	/** Convert current game to PGN format. */
	public final synchronized String getPGN() {
		return game.tree.toPGN(pgnOptions);
	}

	/** Parse a string as FEN or PGN data. */
	public final synchronized void setFENOrPGN(String fenPgn)
			throws ChessParseError {
		Game newGame = new Game(gameTextListener, timeControl, movesPerSession,
				timeIncrement);
		try {
			Position pos = TextIO.readFEN(fenPgn);
			newGame.setPos(pos);
			setPlayerNames(newGame);
		} catch (ChessParseError e) {
			// Try read as PGN instead
			if (!newGame.readPGN(fenPgn, pgnOptions))
				throw e;
			newGame.tree.translateMoves();
		}

		game = newGame;
		gameTextListener.clear();
		updateGameMode();
		gui.setSelection(-1);
		updateGUI();
	}

	/** True if human's turn to make a move. (True in analysis mode.) */
	public boolean localTurn() {
		if ( game == null || game.getGameState() != GameState.ALIVE )
			return false;
		
		return gameMode.localTurn(game.currPos().whiteMove);
	}

	/** Make a move for a human player. */
	public final synchronized void makeLocalMove(Move m) {
		if (localTurn()) {
			Position oldPos = new Position(game.currPos());
			if (doMove(m)) {
				gui.localMoveMade(m);
				setAnimMove(oldPos, m, true);
				updateGUI();
			} else {
				gui.setSelection(-1);
			}
		}		
	}

	/**
	 * Report promotion choice for incomplete move.
	 * 
	 * @param choice
	 *            0=queen, 1=rook, 2=bishop, 3=knight.
	 */
	public final synchronized void reportPromotePiece(int choice) {
		if (promoteMove == null)
			return;
		final boolean white = game.currPos().whiteMove;
		int promoteTo;
		switch (choice) {
		case 1:
			promoteTo = white ? Piece.WROOK : Piece.BROOK;
			break;
		case 2:
			promoteTo = white ? Piece.WBISHOP : Piece.BBISHOP;
			break;
		case 3:
			promoteTo = white ? Piece.WKNIGHT : Piece.BKNIGHT;
			break;
		default:
			promoteTo = white ? Piece.WQUEEN : Piece.BQUEEN;
			break;
		}
		promoteMove.promoteTo = promoteTo;
		Move m = promoteMove;
		promoteMove = null;
		makeLocalMove(m);
	}

	/** Add a null-move to the game tree. */
	public final synchronized void makeHumanNullMove() {
		if (localTurn()) {
			int varNo = game.tree.addMove("--", "", 0, "", "");
			game.tree.goForward(varNo);
			updateGUI();
			gui.setSelection(-1);
		}
	}

	/**
	 * Help human to claim a draw by trying to find and execute a valid draw
	 * claim.
	 */
	public final synchronized boolean claimDrawIfPossible() {
		if (!findValidDrawClaim())
			return false;
		updateGUI();
		return true;
	}

	/** Resign game for current player. */
	public void resignGame() {
		if (game.getGameState() == GameState.ALIVE) {
			game.processString("resign");
			updateGUI();
		}
	}
	
	public void drawGame() {
		if (game.getGameState() == GameState.ALIVE) {			
			game.processString("draw accept");
			updateGUI();
			System.out.println( "game state :"+ game.getGameState());
		}				
	}
	
	/** Resign game for current player. */
	public final synchronized void abortGame() {
		if (game.getGameState() == GameState.ALIVE) {
			game.processString("abort");
			updateGUI();
		}
	}
	
	/** Undo last move. Does not truncate game tree. */
	public final synchronized void undoMove() {
		if (game.getLastMove() != null) {

			boolean didUndo = undoMoveNoUpdate();

			setSelection();
			if (didUndo)
				setAnimMove(game.currPos(), game.getNextMove(), false);
			updateGUI();
		}
	}

	/** Redo last move. Follows default variation. */
	public final synchronized void redoMove() {
		if (game.canRedoMove()) {
			redoMoveNoUpdate();
			setSelection();
			setAnimMove(game.prevPos(), game.getLastMove(), true);
			updateGUI();
		}
	}

	/**
	 * Go back/forward to a given move number. Follows default variations when
	 * going forward.
	 */
	public final synchronized void gotoMove(int moveNr) {
		boolean needUpdate = false;
		while (game.currPos().fullMoveCounter > moveNr) { // Go backward
			int before = game.currPos().fullMoveCounter * 2
					+ (game.currPos().whiteMove ? 0 : 1);
			undoMoveNoUpdate();
			int after = game.currPos().fullMoveCounter * 2
					+ (game.currPos().whiteMove ? 0 : 1);
			if (after >= before)
				break;
			needUpdate = true;
		}
		while (game.currPos().fullMoveCounter < moveNr) { // Go forward
			int before = game.currPos().fullMoveCounter * 2
					+ (game.currPos().whiteMove ? 0 : 1);
			redoMoveNoUpdate();
			int after = game.currPos().fullMoveCounter * 2
					+ (game.currPos().whiteMove ? 0 : 1);
			if (after <= before)
				break;
			needUpdate = true;
		}
		if (needUpdate) {
			setSelection();
			updateGUI();
		}
	}

	/** Go to start of the current variation. */
	public final synchronized void gotoStartOfVariation() {
		boolean needUpdate = false;
		while (true) {
			if (!undoMoveNoUpdate())
				break;
			needUpdate = true;
			if (game.numVariations() > 1)
				break;
		}
		if (needUpdate) {
			setSelection();
			updateGUI();
		}
	}

	/** Go to given node in game tree. */
	public final synchronized void goNode(Node node) {
		if (node == null)
			return;
		if (!game.goNode(node))
			return;
		if (!localTurn()) {
			if (game.getLastMove() != null) {
				game.undoMove();
				if (!localTurn())
					game.redoMove();
			}
		}
		setSelection();
		updateGUI();
	}

	/** Get number of variations in current game position. */
	public final synchronized int numVariations() {
		return game.numVariations();
	}

	/** Get current variation in current position. */
	public final synchronized int currVariation() {
		return game.currVariation();
	}

	/** Go to a new variation in the game tree. */
	public final synchronized void changeVariation(int delta) {
		if (game.numVariations() > 1) {
			game.changeVariation(delta);
			setSelection();
			updateGUI();
		}
	}

	/** Delete whole game sub-tree rooted at current position. */
	public final synchronized void removeSubTree() {
		game.removeSubTree();
		setSelection();
		updateGUI();
	}

	/** Move current variation up/down in the game tree. */
	public final synchronized void moveVariation(int delta) {
		if (game.numVariations() > 1) {
			game.moveVariation(delta);
			updateGUI();
		}
	}

	/**
	 * Add a variation to the game tree.
	 * 
	 * @param preComment
	 *            Comment to add before first move.
	 * @param pvMoves
	 *            List of moves in variation.
	 * @param updateDefault
	 *            If true, make this the default variation.
	 */
	public final synchronized void addVariation(String preComment,
			List<Move> pvMoves, boolean updateDefault) {
		for (int i = 0; i < pvMoves.size(); i++) {
			Move m = pvMoves.get(i);
			String moveStr = TextIO.moveToUCIString(m);
			String pre = (i == 0) ? preComment : "";
			int varNo = game.tree.addMove(moveStr, "", 0, pre, "");
			game.tree.goForward(varNo, updateDefault);
		}
		for (int i = 0; i < pvMoves.size(); i++)
			game.tree.goBack();
		gameTextListener.clear();
		updateGUI();
	}

	/** Update remaining time and trigger GUI update of clocks. */
	public final synchronized void updateRemainingTime() {
		long now = System.currentTimeMillis();
		long wTime = game.timeController.getRemainingTime(true, now);
		long bTime = game.timeController.getRemainingTime(false, now);
		long nextUpdate = 0;
		if (game.timeController.clockRunning()) {
			long t = game.currPos().whiteMove ? wTime : bTime;
			nextUpdate = t % 1000;
			if (nextUpdate < 0)
				nextUpdate += 1000;
			nextUpdate += 1;
		}
		gui.setRemainingTime(wTime, bTime, nextUpdate);
	}

	/** Get PGN header tags and values. */
	public final synchronized void getHeaders(Map<String, String> headers) {
		if (game != null)
			game.tree.getHeaders(headers);
	}

	/** Set PGN header tags and values. */
	public final synchronized void setHeaders(Map<String, String> headers) {
		game.tree.setHeaders(headers);
		gameTextListener.clear();
		updateGUI();
	}

	/** Comments associated with a move. */
	public static final class CommentInfo {
		public String move;
		public String preComment, postComment;
		public int nag;
	}

	/** Get comments associated with current position. */
	public final synchronized CommentInfo getComments() {
		Node cur = game.tree.currentNode;
		CommentInfo ret = new CommentInfo();
		ret.move = cur.moveStrLocal;
		ret.preComment = cur.preComment;
		ret.postComment = cur.postComment;
		ret.nag = cur.nag;
		return ret;
	}

	/** Set comments associated with current position. */
	public final synchronized void setComments(CommentInfo commInfo) {
		Node cur = game.tree.currentNode;
		cur.preComment = commInfo.preComment;
		cur.postComment = commInfo.postComment;
		cur.nag = commInfo.nag;
		gameTextListener.clear();
		updateGUI();
	}

	/** Return true if localized piece names should be used. */
	private final boolean localPt() {
		switch (pgnOptions.view.pieceType) {
		case PGNOptions.PT_ENGLISH:
			return false;
		case PGNOptions.PT_LOCAL:
		case PGNOptions.PT_FIGURINE:
		default:
			return true;
		}
	}

	private final void updateGameMode() {
		if (game != null) {
			boolean gamePaused = !gameMode.clocksActive()
					|| (localTurn() && guiPaused);
			game.setGamePaused(gamePaused);
			updateRemainingTime();
			Game.AddMoveBehavior amb;
			if (gui.discardVariations())
				amb = Game.AddMoveBehavior.REPLACE;
			else if (gameMode.clocksActive())
				amb = Game.AddMoveBehavior.ADD_FIRST;
			else
				amb = Game.AddMoveBehavior.ADD_LAST;
			game.setAddFirst(amb);
		}
	}

	public void offerDraw() {		
		game.processString("draw offer ");									
		updateGUI();
	}
	
	public void makeRemoteMove(final String cmd) {
		Position oldPos = new Position(game.currPos());
		game.processString(cmd);		
		updateGameMode();		
		setSelection();
		setAnimMove(oldPos, game.getLastMove(), true);
		updateGUI();
		System.out.println( "game state :"+ game.getGameState() + " after command :"+cmd);
	}
			
	public boolean isDrawRequested() {
		return drawRequested;
	}

	public void setDrawRequested(boolean drawRequested) {
		this.drawRequested = drawRequested;
	}

	public boolean isAbortRequested() {
		return abortRequested;
	}

	public void setAbortRequested(boolean abortRequested) {
		this.abortRequested = abortRequested;
	}

	private final void setPlayerNames(Game game) {
		if (game != null) {
			game.tree.setPlayerNames(gui.whitePlayerName(),
					gui.blackPlayerName());
		}
	}

	private final boolean undoMoveNoUpdate() {
		if (game.getLastMove() == null)
			return false;

		game.undoMove();
		if (!localTurn()) {
			if (game.getLastMove() != null) {
				game.undoMove();
				if (!localTurn()) {
					game.redoMove();
				}
			} else {
				// Don't undo first white move if playing black vs computer,
				// because that would cause computer to immediately make
				// a new move.
				if (gameMode.playerWhite() || gameMode.playerBlack()) {
					game.redoMove();
					return false;
				}
			}
		}
		return true;
	}

	private final void redoMoveNoUpdate() {
		if (game.canRedoMove()) {

			game.redoMove();
			if (!localTurn() && game.canRedoMove()) {
				game.redoMove();
				if (!localTurn())
					game.undoMove();
			}
		}
	}

	/**
	 * Move a piece from one square to another.
	 * 
	 * @return True if the move was legal, false otherwise.
	 */
	private final boolean doMove(Move move) {
		Position pos = game.currPos();
		ArrayList<Move> moves = new MoveGen().legalMoves(pos);
		int promoteTo = move.promoteTo;
		for (Move m : moves) {
			if ((m.from == move.from) && (m.to == move.to)) {
				if ((m.promoteTo != Piece.EMPTY) && (promoteTo == Piece.EMPTY)) {
					promoteMove = m;
					gui.requestPromotePiece();
					return false;
				}
				if (m.promoteTo == promoteTo) {
					String strMove = TextIO.moveToString(pos, m, false, false,
							moves);
					game.processString(strMove);
					return true;
				}
			}
		}
		gui.reportInvalidMove(move);
		return false;
	}

	private final void updateGUI() {
		ChessboardStatus s = new ChessboardStatus();
		s.state = game.getGameState();
		if (s.state == Game.GameState.ALIVE) {
			s.moveNr = game.currPos().fullMoveCounter;
			s.white = game.currPos().whiteMove;
		} else {
			if ((s.state == GameState.DRAW_REP)
					|| (s.state == GameState.DRAW_50))
				s.drawInfo = game.getDrawInfo(localPt());
		}
		gui.setStatus(s);
		updateMoveList();
								
				
		gui.setPosition(game.currPos(),"", game.tree.variations());

		updateRemainingTime();
	}

	private final void updateMoveList() {
		if (game == null)
			return;
		if (!gameTextListener.isUpToDate()) {
			PGNOptions tmpOptions = new PGNOptions();
			tmpOptions.exp.variations = pgnOptions.view.variations;
			tmpOptions.exp.comments = pgnOptions.view.comments;
			tmpOptions.exp.nag = pgnOptions.view.nag;
			tmpOptions.exp.playerAction = false;
			tmpOptions.exp.clockInfo = false;
			tmpOptions.exp.moveNrAfterNag = false;
			tmpOptions.exp.pieceType = pgnOptions.view.pieceType;
			gameTextListener.clear();
			game.tree.pgnTreeWalker(tmpOptions, gameTextListener);
		}
		gameTextListener.setCurrent(game.tree.currentNode);
		gui.moveListUpdated();
	}

	/** Mark last played move in the GUI. */
	private final void setSelection() {
		Move m = game.getLastMove();
		int sq = ((m != null) && (m.from != m.to)) ? m.to : -1;
		gui.setSelection(sq);
	}

	private void setAnimMove(Position sourcePos, Move move, boolean forward) {
		gui.setAnimMove(sourcePos, move, forward);
	}

	private final boolean findValidDrawClaim() {
		if (game.getGameState() != GameState.ALIVE)
			return true;
		game.processString("draw accept");
		if (game.getGameState() != GameState.ALIVE)
			return true;
		game.processString("draw rep");
		if (game.getGameState() != GameState.ALIVE)
			return true;
		game.processString("draw 50");
		if (game.getGameState() != GameState.ALIVE)
			return true;
		return false;
	}
	
	
	
}
