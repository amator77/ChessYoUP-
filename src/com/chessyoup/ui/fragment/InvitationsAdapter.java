package com.chessyoup.ui.fragment;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.chessyoup.R;
import com.chessyoup.game.Util;
import com.chessyoup.game.chess.ChessGameVariant;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.multiplayer.Invitation;

public class InvitationsAdapter extends BaseAdapter {
    
    private List<Invitation> invitations;
    
    private LayoutInflater layoutInflater;
    
    private Context context;
    
    /**
     * The Class ViewHolder.
     */
    private static class ItemViewHolder {        
        ImageView contactAvatar;        
        TextView challangeDetails;
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, kk:mm:ss", Locale.getDefault());      
    }
    
    public InvitationsAdapter(Context context){
        this.context = context;
        this.invitations = new LinkedList<Invitation>();
        this.layoutInflater = LayoutInflater.from(context);
    }
    
    public void addInvitation(Invitation challenge) {
        this.invitations.add(challenge);        
        this.notifyDataSetChanged();
    }
    
    public void removeInvitation(Invitation invitation) {
        this.invitations.remove(invitation);        
        this.notifyDataSetChanged();
    }
    
    @Override
    public int getCount() {
        return this.invitations.size();
    }

    @Override
    public Object getItem(int position) {
        return this.invitations.get(position);
    }

    @Override
    public long getItemId(int position) {
        return this.invitations.get(position).getCreationTimestamp();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemViewHolder holder;

        if (convertView == null) {
            convertView = layoutInflater.inflate(
                    R.layout.invitation, parent, false);

            holder = new ItemViewHolder();
            holder.contactAvatar = (ImageView) convertView
                    .findViewById(R.id.invotation_avatar);          
            holder.challangeDetails = (TextView) convertView
                    .findViewById(R.id.invitationDetails);            
            convertView.setTag(holder);
        } else {
            holder = (ItemViewHolder) convertView.getTag();
        }

        Invitation invitation = (Invitation)getItem(position);
                                
        if (invitation.getInviter().getIconImageUri() != null) {
            ImageManager.create(this.context).loadImage(holder.contactAvatar, invitation.getInviter().getIconImageUri());
        }
        
        holder.challangeDetails.setText( getInvitationDetails(invitation));
                                
        return convertView;
    }

    private CharSequence getInvitationDetails(Invitation invitation) {
                
        StringBuffer sb = new StringBuffer();
        sb.append(invitation.getInviter().getDisplayName()).append("\n");
        ChessGameVariant gv = Util.getGameVariant(invitation.getVariant());
        sb.append( gv.isRated() ? "Rated Game ," : "Frednly Game").append(" ");
        sb.append(gv.getTime()).append("'+").append(gv.getIncrement()).append("''");
        
        return sb.toString();
    }

    public void removeChallenge(String invitationId) {
        for(Invitation inv : invitations){
            if( inv.getInvitationId().equals(invitationId) ){
                removeInvitation(inv);
            }
        }        
    }
    
}
