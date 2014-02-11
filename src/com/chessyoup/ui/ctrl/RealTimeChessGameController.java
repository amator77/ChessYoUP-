package com.chessyoup.ui.ctrl;

import android.util.Log;

import com.chessyoup.game.GameController;
import com.chessyoup.game.GameModel;
import com.chessyoup.game.GameVariant;
import com.chessyoup.game.RealTimeChessGame.RealTimeChessGameListener;
import com.chessyoup.model.Game;
import com.chessyoup.ui.ChessGameRoomUI;

public class RealTimeChessGameController implements RealTimeChessGameListener {

    private final static String TAG = "RealTimeChessGameController";

    private ChessGameRoomUI chessGameRoomUI;

    public RealTimeChessGameController(ChessGameRoomUI chessGameRoomUI) {
        this.chessGameRoomUI = chessGameRoomUI;
        GameController.getInstance().getRealTimeChessGame().setListener(this);
    }

    @Override
    public void onChallangeRecevied(GameVariant gameVariant) {
        Log.d(TAG, "onChallangeRecevied :: gameVariant=" + gameVariant);
    }

    @Override
    public void onStartRecevied() {
        Log.d(TAG, "onStartRecevied :: ");
    }

    @Override
    public void onReadyRecevied(double remoteRating, double remoteRD, double volatility) {
        Log.d(TAG, "onReadyRecevied :: remoteRating=" + remoteRating + ",remoteRD=" + remoteRD + ",volatility" + volatility);

        chessGameRoomUI.getGameModel().getRemotePlayer().setRating(remoteRating);
        chessGameRoomUI.getGameModel().getRemotePlayer().setRatingDeviation(remoteRD);
        chessGameRoomUI.getGameModel().getRemotePlayer().setVolatility(volatility);
        chessGameRoomUI.roomReady();
    }

    @Override
    public void onMoveRecevied(String move, int thinkingTime) {
        Log.d(TAG, "onMoveRecevied :: move=" + move + ",thinkingTime=" + thinkingTime);
        chessGameRoomUI.getChessboardController().makeRemoteMove(move);
    }

    @Override
    public void onResignRecevied() {
        Log.d(TAG, "onResignRecevied ::");
        GameModel model = this.chessGameRoomUI.getGameModel();

        if (model.getBlackPlayer().getParticipant().getParticipantId().equals(model.getRemotePlayer().getParticipant().getParticipantId())) {
            this.chessGameRoomUI.getChessboardController().resignGameForBlack();            
        } else {
            this.chessGameRoomUI.getChessboardController().resignGameForWhite();
        }

        this.chessGameRoomUI.displayShortMessage(model.getRemotePlayer().getParticipant().getDisplayName() +" resigned!");
        this.chessGameRoomUI.gameFinished();
    }

    @Override
    public void onDrawRecevied() {
        Log.d(TAG, "onDrawRecevied ::");
    }

    @Override
    public void onFlagRecevied() {
        Log.d(TAG, "onFlagRecevied ::");
    }

    @Override
    public void onRematchRecevied() {
        Log.d(TAG, "onRematchRecevied ::");
    }

    @Override
    public void onAbortRecevied() {
        Log.d(TAG, "onAbortRecevied ::");
    }

    @Override
    public void onException(String message) {
        Log.d(TAG, "onException :: message" + message);
    }

    @Override
    public void onChatReceived(String message) {
        Log.d(TAG, "onChatReceived :: message" + message);
    }

    public ChessGameRoomUI getChessGameRoomUI() {
        return chessGameRoomUI;
    }

    public void setChessGameRoomUI(ChessGameRoomUI chessGameRoomUI) {
        this.chessGameRoomUI = chessGameRoomUI;
    }

    private void handleGameFinished(Game game, String whitePlayerId, String blackPlayerId) {
        Log.d(TAG, "handleGameFinished :: game state :" + game.getGameState() + " , wp :" + whitePlayerId + ",bp:" + blackPlayerId);

        if (game.isRated()) {
            switch (game.getGameState()) {
                case ABORTED:
                    updateLocalPlayerLevel();
                    break;
                case BLACK_MATE:
                    updateRatingOnResult(blackPlayerId, whitePlayerId);
                    break;
                case WHITE_MATE:
                    updateRatingOnResult(whitePlayerId, blackPlayerId);
                    break;
                case DRAW_50:
                    updateRatingOnDraw(whitePlayerId, blackPlayerId);
                    break;
                case DRAW_AGREE:
                    updateRatingOnDraw(whitePlayerId, blackPlayerId);
                    break;
                case DRAW_NO_MATE:
                    updateRatingOnDraw(whitePlayerId, blackPlayerId);
                    break;
                case DRAW_REP:
                    updateRatingOnDraw(whitePlayerId, blackPlayerId);
                    break;
                case WHITE_STALEMATE:
                    updateRatingOnDraw(whitePlayerId, blackPlayerId);
                    break;
                case BLACK_STALEMATE:
                    updateRatingOnDraw(whitePlayerId, blackPlayerId);
                    break;
                case RESIGN_WHITE:
                    Log.d(TAG, "RESIGN_WHITE");
                    updateRatingOnResult(blackPlayerId, whitePlayerId);
                    break;
                case RESIGN_BLACK:
                    Log.d(TAG, "RESIGN_BLACK");
                    updateRatingOnResult(whitePlayerId, blackPlayerId);
                    break;
                case ALIVE:
                    Log.d(TAG, "Game not finished!");
                    break;
                default:
                    Log.d(TAG, "Game not finished!");
                    break;
            }
        } else {
            Log.d(TAG, "Frendly game.Increment only level");
            updateLocalPlayerLevel();
        }
    }

    private void updateLocalPlayerLevel() {
        // TODO Auto-generated method stub

    }

    private void updateRatingOnResult(String winerId, String loserId) {
        // Rating winerRating = winerId.equals(gameState.getMyId()) ? gameState
        // .getOwnerRating() : gameState.getRemoteRating();
        // Rating loserRating = loserId.equals(gameState.getMyId()) ? gameState
        // .getOwnerRating() : gameState.getRemoteRating();
        //
        // Log.d(TAG, "Initial ratings :winer " + winerRating.toString());
        // Log.d(TAG, "Initial ratings :loserRating " + loserRating.toString());
        //
        // Util.computeRatingOnResult(winerRating, loserRating);
        //
        // Log.d(TAG, "Updated ratings :winer " + winerRating.toString());
        // Log.d(TAG, "Updated ratings :loserRating " + loserRating.toString());
        //
        // if (winerRating.getUid().equals(gameState.getMyId())) {
        // gameState.getOwner().setRating(winerRating.getRating());
        // gameState.getOwner().setRatingDeviation(
        // winerRating.getRatingDeviation());
        // gameState.getOwner().setVolatility(winerRating.getVolatility());
        // gameState.setRemoteRating(loserRating.getRating(),
        // loserRating.getRatingDeviation(),
        // loserRating.getVolatility());
        // gameState.getOwner().setWins(gameState.getOwner().getWins() + 1);
        // } else {
        // gameState.getOwner().setRating(loserRating.getRating());
        // gameState.getOwner().setRatingDeviation(
        // loserRating.getRatingDeviation());
        // gameState.getOwner().setVolatility(loserRating.getVolatility());
        // gameState.setRemoteRating(winerRating.getRating(),
        // winerRating.getRatingDeviation(),
        // winerRating.getVolatility());
        // gameState.getOwner().setLoses(gameState.getOwner().getLoses() + 1);
        // }
        //
        // updatePlayerStateView();
        // getAppStateClient().updateState(0,
        // gameState.getOwner().toJSON().getBytes());
    }


    private void updateRatingOnDraw(String whitePlayerId, String blackPlayerId) {

    }

}
