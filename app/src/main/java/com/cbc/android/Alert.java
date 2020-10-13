package com.cbc.android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class Alert {
    private AlertDialog dialog;

    public Alert(Context context, DialogInterface.OnClickListener listener, boolean cancel) {
        dialog = new AlertDialog.Builder(context).create();
        if (listener == null)
            listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        };
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", listener);

        if (cancel) dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", listener);
    }
    public Alert(Context context) {
        this(context, null, false);
    }
    public void display(String title, String message) {
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.show();
  }
}
