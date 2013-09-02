package com.chessyoup.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.chessyoup.R;

public class FragmentGame extends Fragment {
	
	public ScrollView moveListScroll;

	public TextView moveListView;

	public ImageView whitePlayerImageView;

	public ImageView blackPlayerImageView;
	
	public Runnable runInstallListeners;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater
				.inflate(R.layout.chessboard_game, container, false);
		
		this.whitePlayerImageView = (ImageView) view
				.findViewById(R.id.chessboard_white_player_image_view);
		this.blackPlayerImageView = (ImageView) view
				.findViewById(R.id.chessboard_black_player_image_view);

		this.moveListView = (TextView) view.findViewById(R.id.moveList);
		this.moveListScroll = (ScrollView) view
				.findViewById(R.id.moveListScroll);
		
		if( runInstallListeners != null ){
			runInstallListeners.run();
		}
		
		return view;
	}
	
}