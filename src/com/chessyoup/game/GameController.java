package com.chessyoup.game;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.WindowManager;

import com.chessyoup.game.GameHelper.GameHelperListener;
import com.google.android.gms.appstate.AppStateClient;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.android.gms.plus.PlusClient;

public class GameController {
	
    protected GameHelper mHelper;
    
    public static final int CLIENT_GAMES = GameHelper.CLIENT_GAMES;
    
    public static final int CLIENT_APPSTATE = GameHelper.CLIENT_APPSTATE;
    
    public static final int CLIENT_PLUS = GameHelper.CLIENT_PLUS;
    
    public static final int CLIENT_ALL = GameHelper.CLIENT_ALL;

    protected int mRequestedClients = CLIENT_ALL;
    
    private String[] mAdditionalScopes;

    protected String mDebugTag = "GameController";
    
    protected boolean mDebugLog = true;
	
	private static GameController instance = new GameController();
	
	private GamePlayer localPlayer;
	
	private boolean initilized;
	
	private Activity activity;
	
	private RealTimeChessGame realTimeChessGame;
	
	public RealTimeChessGame getRealTimeChessGame() {
		return realTimeChessGame;
	}

	public void setRealTimeChessGame(RealTimeChessGame realTimeChessGame) {
		this.realTimeChessGame = realTimeChessGame;
	}

	public static GameController getInstance(){
		return GameController.instance;
	}
	
	public void initialize(Activity activity,GameHelperListener listener){
		
		if( !this.initilized ){
			this.activity = activity;
			this.mHelper = new GameHelper(activity);			
            mHelper.enableDebugLog(mDebugLog, mDebugTag);	        
	        mHelper.setup(listener, mRequestedClients, mAdditionalScopes);
	        this.realTimeChessGame = new RealTimeChessGame(getGamesClient());
			this.initilized = true;
		}
	}
	
	public void createRoom(RoomUpdateListener roomUpdatelistener,RoomStatusUpdateListener roomStatusUpdateListener,String remotePlayer, int gameVariant){	    
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(roomUpdatelistener);
        rtmConfigBuilder.addPlayersToInvite(new String[] {remotePlayer});
        rtmConfigBuilder.setVariant(gameVariant);
        rtmConfigBuilder.setMessageReceivedListener(this.realTimeChessGame);
        rtmConfigBuilder.setRoomStatusUpdateListener(roomStatusUpdateListener);  
        mHelper.getGamesClient().createRoom(rtmConfigBuilder.build());        
	}
	
	public void joinRoom(RoomUpdateListener roomUpdatelistener,RoomStatusUpdateListener roomStatusUpdateListener,String invitationId){
	    RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(roomUpdatelistener);
        roomConfigBuilder.setInvitationIdToAccept(invitationId).setMessageReceivedListener(this.realTimeChessGame).setRoomStatusUpdateListener(roomStatusUpdateListener);        
        GameController.getInstance().getGamesClient().joinRoom(roomConfigBuilder.build());
    }
	
	public void leaveRoom(RoomUpdateListener roomUpdatelistener,String roomId){
	    Log.d(mDebugTag, "leaveRoom ::"+roomId);	    
        mHelper.getGamesClient().leaveRoom(roomUpdatelistener, roomId);
    }
	
	public void showAlert(String title, String message) {
        mHelper.showAlert(title, message);
    }

	public void showAlert(String message) {
        mHelper.showAlert(message);
    }

	public void enableDebugLog(boolean enabled, String tag) {
        mDebugLog = true;
        mDebugTag = tag;
        if (mHelper != null) {
            mHelper.enableDebugLog(enabled, tag);
        }
    }

	public void reconnectClients(int whichClients) {
        mHelper.reconnectClients(whichClients);
    }

	public String getScopes() {
        return mHelper.getScopes();
    }

	public String[] getScopesArray() {
        return mHelper.getScopesArray();
    }

	public boolean hasSignInError() {
        return mHelper.hasSignInError();
    }

	public GameHelper.SignInFailureReason getSignInError() {
        return mHelper.getSignInError();
    }
	
	public Invitation getInvitation(){
		return this.mHelper.getInvitation();
	}
	
	public void onActivityResult(int requestCode, int responseCode,Intent intent){
		 mHelper.onActivityResult(requestCode, responseCode, intent);
	}
	
	public void start(){
		this.mHelper.onStart(this.activity);
	}
	
	public void stop(){
		this.mHelper.onStop();
	}

	public GamePlayer getLocalPlayer() {
		return localPlayer;
	}
	
	public void beginUserInitiatedSignIn() {
        this.mHelper.beginUserInitiatedSignIn();
    }
	
	public void signOut() {
        this.mHelper.signOut();
    }
	
	public void setLocalPlayer(GamePlayer localPlayer) {
		this.localPlayer = localPlayer;
	}

	public GamesClient getGamesClient() {
		return this.mHelper.getGamesClient();
	}

	public AppStateClient getAppStateClient() {
		return this.mHelper.getAppStateClient();
	}


	public PlusClient getPlusClient() {
		return this.mHelper.getPlusClient();
	}
	
	public boolean isInitilized() {
		return initilized;
	}

	public void showGameError(String string, String string2) {
		this.mHelper.showAlert(string, string2);			
	}

	public boolean isSignedIn() {
		return this.mHelper.isSignedIn();
	}	
}
