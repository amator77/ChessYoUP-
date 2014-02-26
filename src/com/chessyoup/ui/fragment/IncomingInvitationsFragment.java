package com.chessyoup.ui.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
    
    private Runnable onInvitationAccepted;
    
    private Runnable onInvitationRejectd;
    
    public IncomingInvitationsFragment(){
    }
    
    public InvitationsAdapter getInvitationsAdapter() {
        return invitationsAdapter;
    }

    public void setInvitationsAdapter(InvitationsAdapter invitationsAdapter) {
        this.invitationsAdapter = invitationsAdapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,final ViewGroup container, 
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
                
                if (selectedInvitation != null) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(container.getContext());
                    builder.setTitle("Invitation");
                    builder.setItems(new String[] {"Accept", "Reject","Ignore"}, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                           
                            switch (item) {
                                case 0:
                                    if (onInvitationAccepted != null) {
                                        onInvitationAccepted.run();
                                    }
                                    dialog.dismiss();
                                    break;
                                case 1:
                                    if (onInvitationRejectd != null) {
                                        onInvitationRejectd.run();
                                    }
                                    dialog.dismiss();
                                    break;  
                                default:
                                    dialog.dismiss();
                                    break;
                            }
                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.show();
                }
            }
        });
        
        return view;
    }

    public Invitation getSelectedInvitation() {
        return selectedInvitation;
    }

    public Runnable getOnInvitationAccepted() {
        return onInvitationAccepted;
    }

    public void setOnInvitationAccepted(Runnable onInvitationAccepted) {
        this.onInvitationAccepted = onInvitationAccepted;
    }

    public Runnable getOnInvitationRejectd() {
        return onInvitationRejectd;
    }

    public void setOnInvitationRejectd(Runnable onInvitationRejectd) {
        this.onInvitationRejectd = onInvitationRejectd;
    }
}
