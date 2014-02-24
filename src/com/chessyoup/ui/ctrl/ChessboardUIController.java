package com.chessyoup.ui.ctrl;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.chessyoup.R;
import com.chessyoup.chessboard.ChessboardStatus;
import com.chessyoup.chessboard.ChessboardUIInterface;
import com.chessyoup.game.chess.ChessGameController;
import com.chessyoup.model.Game.GameState;
import com.chessyoup.model.Move;
import com.chessyoup.model.Position;
import com.chessyoup.model.TextIO;
import com.chessyoup.ui.ChessGameRoomUI;
import com.chessyoup.ui.util.UIUtil;

public class ChessboardUIController extends GestureDetector.SimpleOnGestureListener implements ChessboardUIInterface, Runnable {

    private final static String TAG = "GameUIController";

    private ChessGameRoomUI chessGameRoomUI;

    private Handler handlerTimer;

    private Dialog promoteDialog;
    
    public ChessboardUIController(ChessGameRoomUI chessGameRoomUI) {
        this.chessGameRoomUI = chessGameRoomUI;
        this.handlerTimer = new Handler();
        promoteDialog = createPromoteDialog();
    }

    @Override
    public boolean onDown(MotionEvent e) {
        handleClick(e);
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

        return true;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        chessGameRoomUI.getBoardPlayView().cancelLongPress();
        handleClick(e);
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP)
            handleClick(e);
        return true;
    }

    private final void handleClick(MotionEvent e) {
        if (true) {

            int sq = chessGameRoomUI.getBoardPlayView().eventToSquare(e);
            Move m = chessGameRoomUI.getBoardPlayView().mousePressed(sq);
            Log.d(TAG, "handleClick :: " + sq);

            if (m != null) {
                Log.d(TAG, "Move :" + m);
                chessGameRoomUI.getChessboardController().makeLocalMove(m);                                
            }
        }
    }


    @Override
    public void setPosition(Position pos, String variantInfo, ArrayList<Move> variantMoves) {
        Log.d(TAG, "setPosition :: pos=" + pos + ",variantInfo=" + variantInfo + ",variantMoves" + variantMoves);
        chessGameRoomUI.getBoardPlayView().setPosition(pos);
    }

    @Override
    public void setSelection(int sq) {
        Log.d(TAG, "setSelection :: square=" + sq);        
        chessGameRoomUI.getBoardPlayView().setSelection(sq);
        chessGameRoomUI.getBoardPlayView().userSelectedSquare = false;                
    }

    @Override
    public void setStatus(ChessboardStatus s) {
        Log.d(TAG, "setStatus :: status=" + s);
        
        String str;
        switch (s.state) {
        case ALIVE:
            str = Integer.valueOf(s.moveNr).toString();
            if (s.white)
                str += ". " + this.chessGameRoomUI.getString(R.string.whites_move);
            else
                str += "... " + this.chessGameRoomUI.getString(R.string.blacks_move);
            if (s.ponder)
                str += " (" + this.chessGameRoomUI.getString(R.string.ponder) + ")";
            if (s.thinking)
                str += " (" + this.chessGameRoomUI.getString(R.string.thinking) + ")";
            if (s.analyzing)
                str += " (" + this.chessGameRoomUI.getString(R.string.analyzing) + ")";
            break;
        case WHITE_MATE:
            str = this.chessGameRoomUI.getString(R.string.white_mate);
            break;
        case BLACK_MATE:
            str = this.chessGameRoomUI.getString(R.string.black_mate);
            break;
        case WHITE_STALEMATE:
        case BLACK_STALEMATE:
            str = this.chessGameRoomUI.getString(R.string.stalemate);
            break;
        case DRAW_REP: {
            str = this.chessGameRoomUI.getString(R.string.draw_rep);
            if (s.drawInfo.length() > 0)
                str = str + " [" + s.drawInfo + "]";
            break;
        }
        case DRAW_50: {
            str = this.chessGameRoomUI.getString(R.string.draw_50);
            if (s.drawInfo.length() > 0)
                str = str + " [" + s.drawInfo + "]";
            break;
        }
        case DRAW_NO_MATE:
            str = this.chessGameRoomUI.getString(R.string.draw_no_mate);
            break;
        case DRAW_AGREE:
            str = this.chessGameRoomUI.getString(R.string.draw_agree);
            break;
        case RESIGN_WHITE:
            str = this.chessGameRoomUI.getString(R.string.resign_white);
            break;
        case RESIGN_BLACK:
            str = this.chessGameRoomUI.getString(R.string.resign_black);
            break;
        default:
            str = "unknown";
        }
        
        if( s.state != GameState.ALIVE ){
            this.chessGameRoomUI.displayShortMessage(str);
            this.chessGameRoomUI.gameFinished();                       
        }
        
        Log.d(TAG, "setStatus :: "+str +" status :"+GameState.ALIVE);
    }

    @Override
    public void moveListUpdated() {
        Log.d(TAG, "moveListUpdated :: ");

    }

    @Override
    public void requestPromotePiece() {
        Log.d(TAG, "requestPromotePiece :: ");
        promoteDialog.show();
    }

    @Override
    public void runOnUIThread(Runnable runnable) {
        Log.d(TAG, "runOnUIThread :: ");
        this.chessGameRoomUI.runOnUiThread(runnable);
    }

    @Override
    public void reportInvalidMove(Move m) {
        Log.d(TAG, "reportInvalidMove :: move=" + m.toString());
        
        String msg = String.format("%s %s-%s",
                        chessGameRoomUI.getString(R.string.invalid_move),
                        TextIO.squareToString(m.from), TextIO.squareToString(m.to));
        
        this.chessGameRoomUI.displayShortMessage(msg);
    }

    @Override
    public void setRemainingTime(int wTime, int bTime, int nextUpdate) {
        Log.d(TAG, "setRemainingTime :: wTime=" + UIUtil.timeToString(wTime) + ",bTime" + UIUtil.timeToString(bTime) + ",nextUpdate=" + nextUpdate);


        if (chessGameRoomUI.getChessboardController().localTurn()) {
            
            if( chessGameRoomUI.getChessboardController().getGame().currPos().whiteMove ){
                if( wTime <= 0 ){
                    chessGameRoomUI.getChessboardController().resignGame();
                    ChessGameController.getController().getRealTimeGameClient().flag();
                }
            }
            else{
                if( bTime <= 0 ){
                    chessGameRoomUI.getChessboardController().resignGame();
                    ChessGameController.getController().getRealTimeGameClient().flag();
                }
            }
        }

        
        chessGameRoomUI.updateClocks(UIUtil.timeToString(wTime), UIUtil.timeToString(bTime));
        
        handlerTimer.removeCallbacks(this);
        if (nextUpdate > 0)
            handlerTimer.postDelayed(this, nextUpdate);
    }
    
    public void stopClock(){
        this.handlerTimer.removeCallbacks(this);        
    }
    
    @Override
    public void setAnimMove(Position sourcePos, Move move, boolean forward) {
        Log.d(TAG, "setAnimMove :: sourcePos=" + sourcePos + ",move=" + move + ",forward=" + forward);
    }

    @Override
    public String whitePlayerName() {
        return chessGameRoomUI.getWitePlayerName();
    }

    @Override
    public String blackPlayerName() {
        return chessGameRoomUI.getBlackPlayerName();
    }

    @Override
    public boolean discardVariations() {
        Log.d(TAG, "discardVariations :: ");
        return false;
    }

    @Override
    public void localMoveMade(Move m) {
        Log.d(TAG, "localMoveMade :: m=" + m +" , thinking time :"+chessGameRoomUI.getChessboardController().getGame().timeController.getLocalElapsed()+" ms");
                
        if( ChessGameController.getController().isInitilized()){
            ChessGameController.getController().getRealTimeGameClient().move(TextIO.moveToUCIString(m), (int)chessGameRoomUI.getChessboardController().getGame().timeController.getLocalElapsed());
        }
    }

    @Override
    public void run() {
        this.chessGameRoomUI.getChessboardController().updateRemainingTime();
    }

    private final Dialog createPromoteDialog() {
        final CharSequence[] items = {chessGameRoomUI.getString(R.string.queen), chessGameRoomUI.getString(R.string.rook), chessGameRoomUI.getString(R.string.bishop), chessGameRoomUI.getString(R.string.knight)};

        AlertDialog.Builder builder = new AlertDialog.Builder(chessGameRoomUI);
        builder.setTitle(R.string.promote_pawn_to);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                chessGameRoomUI.getChessboardController().reportPromotePiece(item);
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }
}
