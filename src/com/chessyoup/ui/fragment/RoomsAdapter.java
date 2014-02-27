package com.chessyoup.ui.fragment;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.chessyoup.R;
import com.chessyoup.game.Util;
import com.chessyoup.game.chess.ChessGameVariant;
import com.chessyoup.ui.ctrl.ChessGameController;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.Room;

public class RoomsAdapter extends BaseAdapter  {
    
 private List<Room> rooms;
    
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
    
    public RoomsAdapter(Context context){
        this.context = context;
        this.rooms = new LinkedList<Room>();
        this.layoutInflater = LayoutInflater.from(context);
    }
    
    public void addRoom(Room room) {
        this.rooms.add(room);        
        this.notifyDataSetChanged();
    }
    
    public void removeRoom(Room room) {        
        this.rooms.remove(room);        
        this.notifyDataSetChanged();
    }
    
    @Override
    public int getCount() {
        return this.rooms.size();
    }

    @Override
    public Object getItem(int position) {
        return this.rooms.get(position);
    }

    @Override
    public long getItemId(int position) {
        return this.rooms.get(position).getCreationTimestamp();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemViewHolder holder;

        if (convertView == null) {
            convertView = layoutInflater.inflate(
                    R.layout.outgoing_room, parent, false);

            holder = new ItemViewHolder();
            holder.contactAvatar = (ImageView) convertView
                    .findViewById(R.id.remote_avatar);          
            holder.challangeDetails = (TextView) convertView
                    .findViewById(R.id.roomDetails);            
            convertView.setTag(holder);
        } else {
            holder = (ItemViewHolder) convertView.getTag();
        }

        Room room = (Room)getItem(position);
        Uri remoteIconUri = getRemoteIcontURI(room);        
        
        if (remoteIconUri != null) {
            System.out.println(remoteIconUri);
            ImageManager.create(this.context).loadImage(holder.contactAvatar, remoteIconUri);
        }
        
        holder.challangeDetails.setText( getRoomDetails(room));
                                
        return convertView;
    }

    private CharSequence getRoomDetails(Room room) {
        
        StringBuffer sb = new StringBuffer();
                        
        for( Participant p : room.getParticipants()){
            
            if(!p.getParticipantId().equals(room.getCreatorId())){
                sb.append(p.getDisplayName()).append("\n");
            }
        }
                        
        ChessGameVariant gv = Util.getGameVariant(room.getVariant());
        sb.append( gv.isRated() ? "Rated Game ," : "Frednly Game").append(" ");
        sb.append(gv.getTime()).append("''+").append(gv.getIncrement()).append("''");
        
        return sb.toString();
    }

    private Uri getRemoteIcontURI(Room room) {
                        
        for( Participant p : room.getParticipants()){
            
            if(!p.getParticipantId().equals(room.getCreatorId())){
                return p.getIconImageUri();
            }
        }
        
        return null;
    }
    
    public boolean  removeRoom(String roomId) {
        
        for(Room r : rooms){
            if(r.getRoomId().equals(roomId)){
                removeRoom(r);
                return true;
            }
        }
        
        return false;
    }
    
}
