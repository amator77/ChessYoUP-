package com.chessyoup.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.chessyoup.R;

public class FragmentMoves extends Fragment {
    
    public TextView moveList;

    public Runnable runInstallListener;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
        
        View view = inflater.inflate(R.layout.moves, container, false);
        
        this.moveList = (TextView) view.findViewById(R.id.moveList);        
        
        if( runInstallListener != null ){
            runInstallListener.run();
        }
        
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