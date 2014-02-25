package com.chessyoup.game;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.chessyoup.game.GameHelper.GameHelperListener;
import com.google.android.gms.appstate.AppStateClient;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.android.gms.plus.PlusClient;

public abstract class GameController {
	  
    protected GameHelper mHelper;
    
    public static final int CLIENT_GAMES = GameHelper.CLIENT_GAMES;
    
    public static final int CLIENT_APPSTATE = GameHelper.CLIENT_APPSTATE;
    
    public static final int CLIENT_PLUS = GameHelper.CLIENT_PLUS;
    
    public static final int CLIENT_ALL = GameHelper.CLIENT_ALL;

    protected int mRequestedClients = CLIENT_ALL;
    
    protected String[] mAdditionalScopes;

    protected String mDebugTag = "GameController";
    
    protected boolean mDebugLog = true;	
	
    protected GamePlayer localPlayer;
	
	protected boolean initilized;
	
	protected Activity activity;
	
	protected RealTimeGameClient realTimeGame;		
	
	public abstract void initialize(Activity activity,GameHelperListener listener);
	
	private RoomUpdateListener roomUpdatelistener;
	
	private RoomStatusUpdateListener roomStatusUpdateListener;
	
	public void createRoom(String remotePlayer, int gameVariant){	    
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(roomUpdatelistener);
        rtmConfigBuilder.addPlayersToInvite(new String[] {remotePlayer});
        rtmConfigBuilder.setVariant(gameVariant);
        rtmConfigBuilder.setMessageReceivedListener(this.realTimeGame);
        rtmConfigBuilder.setRoomStatusUpdateListener(roomStatusUpdateListener);  
        mHelper.getGamesClient().createRoom(rtmConfigBuilder.build());        
	}
	
	public void joinRoom(String invitationId){
	    RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(roomUpdatelistener);
        roomConfigBuilder.setInvitationIdToAccept(invitationId).setMessageReceivedListener(this.realTimeGame).setRoomStatusUpdateListener(roomStatusUpdateListener);        
        getGamesClient().joinRoom(roomConfigBuilder.build());
    }
	
	public void leaveRoom(String roomId){
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
	
	public RoomUpdateListener getRoomUpdatelistener() {
        return roomUpdatelistener;
    }

    public void setRoomUpdatelistener(RoomUpdateListener roomUpdatelistener) {
        this.roomUpdatelistener = roomUpdatelistener;
    }

    public RoomStatusUpdateListener getRoomStatusUpdateListener() {
        return roomStatusUpdateListener;
    }

    public void setRoomStatusUpdateListener(RoomStatusUpdateListener roomStatusUpdateListener) {
        this.roomStatusUpdateListener = roomStatusUpdateListener;
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
