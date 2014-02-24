package com.chessyoup.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.chessyoup.R;
import com.google.android.gms.games.multiplayer.Invitation;

public class IncomingInvitationsFragment extends Fragment {
    
    private InvitationsAdapter invitationsAdapter;
    
    private ListView invitationsListView;
    
    private Invitation selectedInvitation;
    
    private Runnable onInvitationSelected;
    
    public IncomingInvitationsFragment(){
    }
    
    public InvitationsAdapter getInvitationsAdapter() {
        return invitationsAdapter;
    }

    public void setInvitationsAdapter(InvitationsAdapter invitationsAdapter) {
        this.invitationsAdapter = invitationsAdapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
        
        View view = inflater.inflate(R.layout.incoming, container, false);
        invitationsListView = (ListView) view
                        .findViewById(R.id.invitationsListView);
        invitationsListView.setAdapter(invitationsAdapter);
        
        invitationsListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                selectedInvitation = (Invitation)invitationsAdapter.getItem(position);

                if (onInvitationSelected != null) {
                    onInvitationSelected.run();                  
                }
            }
        });
        
        return view;
    }

    public Invitation getSelectedInvitation() {
        return selectedInvitation;
    }
}
