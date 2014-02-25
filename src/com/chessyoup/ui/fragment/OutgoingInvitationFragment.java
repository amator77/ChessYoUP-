package com.chessyoup.ui.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.chessyoup.R;
import com.google.android.gms.games.multiplayer.realtime.Room;

public class OutgoingInvitationFragment extends Fragment {

    private RoomsAdapter roomsAdapter;

    private ListView roomListView;

    private Room selectedRoom;
    
    private Runnable onRoomCanceled;

    public OutgoingInvitationFragment() {}

    public RoomsAdapter getRoomsAdapter() {
        return roomsAdapter;
    }

    public void setRoomsAdapter(RoomsAdapter roomsAdapter) {
        this.roomsAdapter = roomsAdapter;
    }


    public Room getSelectedRoom() {
        return selectedRoom;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.outgoing, container, false);
        roomListView = (ListView) view.findViewById(R.id.roomsListView);
        roomListView.setAdapter(roomsAdapter);

        roomListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedRoom = (Room) roomsAdapter.getItem(position);

                if (selectedRoom != null) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(container.getContext());
                    builder.setTitle("Cancel Invitation");
                    builder.setItems(new String[] {"Ok", "Cancel"}, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            switch (item) {
                                case 0:
                                    if (onRoomCanceled != null) {
                                        onRoomCanceled.run();
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

    public Runnable getOnRoomCanceled() {
        return onRoomCanceled;
    }

    public void setOnRoomCanceled(Runnable onRoomCanceled) {
        this.onRoomCanceled = onRoomCanceled;
    }
}
