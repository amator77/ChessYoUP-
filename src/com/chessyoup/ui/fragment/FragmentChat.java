package com.chessyoup.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.chessyoup.R;

public class FragmentChat extends Fragment {
	
	public TextView chatDisplay;

	public Button chatSendMessageButton;
	
	public EditText chatEditText;
	
	public Runnable runInstallListener;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.chat, container, false);
		
		this.chatDisplay = (TextView) view.findViewById(R.id.chatDisplay);
		this.chatEditText = (EditText) view.findViewById(R.id.editChatText);
		this.chatSendMessageButton = (Button) view
				.findViewById(R.id.sendChatButton);
				
        return view;
    }
	
	public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        if( runInstallListener != null ){
            runInstallListener.run();
            runInstallListener = null;
        }        
    }
}