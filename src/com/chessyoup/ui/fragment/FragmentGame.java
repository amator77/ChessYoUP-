package com.chessyoup.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.chessyoup.R;
import com.chessyoup.game.view.ChessBoardPlayView;

public class FragmentGame extends Fragment {
	
    public ChessBoardPlayView chessBoardPlayView;
    
    public TextView localClockView, remoteClockView, localPlayerView, remotePlayerView;
    
    public ImageButton gameNavStart , gameNavPrev ,gameNavNext ,gameNavEnd  ;
    
	public Runnable runInstallListeners;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater
				.inflate(R.layout.chessboard, container, false);
			
		this.chessBoardPlayView = (ChessBoardPlayView) view.findViewById(R.id.chessboard);
		this.localClockView = (TextView) view.findViewById(R.id.localPlayerClockView);
        this.remoteClockView = (TextView) view.findViewById(R.id.remotePlayerClockView);
        this.localPlayerView = (TextView) view.findViewById(R.id.localPlayerDisplayNameView);
        this.remotePlayerView = (TextView) view.findViewById(R.id.remotePlayerDisplayNameView);
		this.gameNavStart =  (ImageButton)view.findViewById(R.id.game_nav_start);
		this.gameNavPrev = (ImageButton)view.findViewById(R.id.game_nav_prev);
		this.gameNavNext = (ImageButton)view.findViewById(R.id.game_nav_next);
        this.gameNavEnd = (ImageButton)view.findViewById(R.id.game_nav_end);
		        
        		
		return view;
	}
	
	
	public void onActivityCreated(Bundle savedInstanceState){
	    super.onActivityCreated(savedInstanceState);
	    if( runInstallListeners != null ){
            runInstallListeners.run();
        }        
    }
}