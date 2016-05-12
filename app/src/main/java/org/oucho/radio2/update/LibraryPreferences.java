package org.oucho.radio2.update;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

class LibraryPreferences {
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    private static final String KeyAppUpdaterShow = "prefAppUpdaterShow";
    private static final String KeySuccessfulChecks = "prefSuccessfulChecks";

    @SuppressLint("CommitPrefEdits")
    public LibraryPreferences(Context context) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.editor = sharedPreferences.edit();
    }

    public Boolean getAppUpdaterShow() {
        return sharedPreferences.getBoolean(KeyAppUpdaterShow, true);
    }

    public Integer getSuccessfulChecks() {
        return sharedPreferences.getInt(KeySuccessfulChecks, 0);
    }

    public void setSuccessfulChecks(Integer checks) {
        editor.putInt(KeySuccessfulChecks, checks);
        editor.commit();
    }

}
