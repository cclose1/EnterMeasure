package com.cbc.android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class Alert {
  private AlertDialog dialog;

  public Alert(Context context) {
    dialog = new AlertDialog.Builder(context).create();

    dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {

      }
    });
  }
  public void display(String title, String message) {
    dialog.setTitle(title);
    dialog.setMessage(message);
    dialog.show();
  }
}
