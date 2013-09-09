package com.chessyoup.ui.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.chessyoup.R;

public class NewGameDialog extends DialogFragment implements OnItemSelectedListener {
	private final static String TAG = "NewGameDialog";
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
       
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());         		
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.new_game_dialog, null);
        Spinner spinner = (Spinner)view.findViewById(R.id.spinner1);
        
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getActivity(),
                R.array.time_control_texts, android.R.layout.simple_dropdown_item_1line);        
        
        Log.d(TAG,adapter.getItem(1)+"");
        
        spinner.setAdapter(adapter);
        spinner.setSelection(0);        
        
        builder.setView(view);
        
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   Log.d(TAG, "ok option");
                   }
               })
               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   Log.d(TAG, "cancel option");
                   }
               });
        
        
        spinner.setOnItemSelectedListener(this);
        builder.setTitle(R.string.option_new_game);
        
        
        Dialog d = builder.create(); 
        
        
        return d;
    }
	
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, 
            int pos, long id) {		

		Log.d(TAG, "onItemSelected ::"+parent.getItemAtPosition(pos));
    }

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		Log.d(TAG, "onNothingSelected");		
	}
}
