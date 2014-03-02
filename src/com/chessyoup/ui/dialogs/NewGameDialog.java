package com.chessyoup.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.chessyoup.R;
import com.chessyoup.ui.util.DownloadImageTask;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.plus.model.people.Person;

public class NewGameDialog extends DialogFragment {

    private final static String TAG = "NewGameDialog";

    private Runnable runOnCreate;

    private ImageView personAvatar;

    private TextView personDetailsView;

    public interface NewGameDialogListener {

        public void onNewGameCreated(String color, boolean isRated, int timeControll, int increment);

        public void onNewGameRejected();
    }

    private NewGameDialogListener listener;

    public NewGameDialogListener getListener() {
        return listener;
    }

    public void setListener(NewGameDialogListener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.new_game_dialog, null);
        final Spinner spinner1 = (Spinner) view.findViewById(R.id.spinner1);
        final Spinner spinner2 = (Spinner) view.findViewById(R.id.spinner2);
        this.personAvatar = (ImageView) view.findViewById(R.id.newGameOponentAvatar);
        this.personDetailsView = (TextView) view.findViewById(R.id.newGameOponentDetails);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getActivity(), R.array.time_control_texts, android.R.layout.simple_dropdown_item_1line);

        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this.getActivity(), R.array.time_increment_texts, android.R.layout.simple_dropdown_item_1line);

        Log.d(TAG, adapter.getItem(1) + "");

        spinner1.setAdapter(adapter);
        spinner1.setSelection(3);

        spinner2.setAdapter(adapter2);
        spinner2.setSelection(0);

        builder.setView(view);

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {

                if (listener != null) {
                    RadioGroup rg = (RadioGroup) view.findViewById(R.id.radioGroup1);
                    CheckBox cb = (CheckBox) view.findViewById(R.id.checkBox1);
                    String color = rg.getCheckedRadioButtonId() == R.id.radio0 ? "white" : "black";
                    int timeControll = spinner1.getSelectedItemPosition();
                    int increment = spinner2.getSelectedItemPosition();
                    listener.onNewGameCreated(color, cb.isChecked(), timeControll, increment);
                }
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (listener != null) {
                    listener.onNewGameRejected();
                }
            }
        });


        builder.setTitle(R.string.option_new_game);

        Dialog d = builder.create();

        if (runOnCreate != null) {
            runOnCreate.run();
            runOnCreate = null;
        }

        return d;
    }

    public Runnable getRunOnCreate() {
        return runOnCreate;
    }

    public void setRunOnCreate(Runnable runOnCreate) {
        this.runOnCreate = runOnCreate;
    }

    public void showPersonDetails(Person person) {
        DownloadImageTask task = new DownloadImageTask(personAvatar);
        String url = person.getImage().getUrl();
        url = url.replace("50", "100");
        task.execute(url);
        personDetailsView.setText(person.getDisplayName()+"("+person.getNickname()+")");
    }
}
