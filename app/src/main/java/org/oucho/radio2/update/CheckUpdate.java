package org.oucho.radio2.update;

import android.app.Activity;

public class CheckUpdate {

    static final String updateURL = "http://oucho.free.fr/app_android/Radio/update_radio2.xml";

    public static void onStart(Activity activity){

        new AppUpdate(activity)
                .setUpdateXML()
                .setDisplay(Display.SNACKBAR)
                .start();
    }

    public static void withInfo(Activity activity) {
        new AppUpdate(activity)
                .setUpdateXML()
                .setDisplay(Display.DIALOG)
                .showAppUpdated()
                .start();
    }

}
