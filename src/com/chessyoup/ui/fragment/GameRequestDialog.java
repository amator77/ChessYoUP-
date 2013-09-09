package com.chessyoup.ui.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.chessyoup.R;

public class GameRequestDialog extends DialogFragment {

	public interface GameRequestDialogListener{
		public void onGameRequestAccepted();
		public void onGameRequestRejected();
	}
	
	private final static String TAG = "GameRequestDialog";
	
	private GameRequestDialogListener listener; 
	
	private TextView textView;
	
	public GameRequestDialogListener getListener() {
		return listener;
	}

	public void setListener(GameRequestDialogListener listener) {
		this.listener = listener;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.game_request_dialog, null);
		builder.setView(view);
		textView = (TextView)view.findViewById(R.id.textView1);
		
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				Log.d(TAG, "ok option");
				
				if( listener != null ){
					listener.onGameRequestAccepted();
				}
			}
		}).setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Log.d(TAG, "cancel option");
						if( listener != null ){
							listener.onGameRequestRejected();
						}
					}
				});

		
		builder.setTitle(R.string.option_game_request_details);
		
		return builder.create();
	}

	public void setGameDetails(String details) {		
		textView.setText(details);		
	}
}
