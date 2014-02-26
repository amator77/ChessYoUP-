package com.chessyoup.ui.util;

import com.chessyoup.R;
import com.chessyoup.game.view.ChessTableUI;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

public class UIUtil {
    public static String timeToString(int time) {
        int secs = (int) Math.floor((time + 999) / 1000.0);
        boolean neg = false;
        if (secs < 0) {
            neg = true;
            secs = -secs;
        }
        int mins = secs / 60;
        secs -= mins * 60;
        StringBuilder ret = new StringBuilder();
        if (neg)
            ret.append('-');
        ret.append(mins);
        ret.append(':');
        if (secs < 10)
            ret.append('0');
        ret.append(secs);
        return ret.toString();
    }

    public static AlertDialog buildConfirmAlertDialog(Context context,String title , final Runnable okAction) {
        AlertDialog.Builder db = new AlertDialog.Builder(context);
        db.setTitle(title);        
        String actions[] = new String[2];
        actions[0] = context.getString(R.string.ok_option);
        actions[1] = context.getString(R.string.cancel_option);
        db.setItems(actions, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        okAction.run();
                        break;
                    case 1:
                        break;
                    default:
                        break;
                }
            }
        });

        AlertDialog ad = db.create();
        ad.setCancelable(true);
        ad.setCanceledOnTouchOutside(true);
        return ad;
    }
    
    public static void displayShortMessage(Context context, String string) {
        Toast.makeText(context, string, Toast.LENGTH_SHORT).show();
    }
}
