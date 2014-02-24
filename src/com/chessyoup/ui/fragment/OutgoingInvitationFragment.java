package com.chessyoup.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chessyoup.R;

public class OutgoingInvitationFragment  extends Fragment {
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
        
        View view = inflater.inflate(R.layout.outgoing, container, false);
                
                
        return view;
    }
    
}
