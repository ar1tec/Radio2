package org.oucho.radio2.update;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
import android.view.View;

import org.oucho.radio2.R;

import java.net.URL;

class UtilsDisplay {


    static void showUpdateAvailableDialog(final Context context, String title, String content, String btnNegative, String btnPositive, final URL apk) {

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(content)

        .setPositiveButton(btnPositive, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        UtilsLibrary.goToUpdate(context, apk);
                    }
                })

        .setNegativeButton(btnNegative, null).show();

    }

    static void showUpdateNotAvailableDialog(final Context context, String title, String content) {


        new AlertDialog.Builder(context)
                .setTitle(title)

                .setMessage(content)
                .setPositiveButton(context.getResources().getString(android.R.string.ok), null)
                .show();
    }

    static void showUpdateAvailableSnackbar(final Context context, String content, Boolean indefinite, final URL apk) {
        Activity activity = (Activity) context;
        int snackbarTime = indefinite ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG;


        Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content), content, snackbarTime);
        snackbar.setAction(context.getResources().getString(R.string.appupdater_btn_update), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UtilsLibrary.goToUpdate(context, apk);
            }
        }).show();
    }

    static void showUpdateNotAvailableSnackbar(final Context context, String content, Boolean indefinite) {
        Activity activity = (Activity) context;
        int snackbarTime = indefinite ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG;


        Snackbar.make(activity.findViewById(android.R.id.content), content, snackbarTime).show();
    }

}
