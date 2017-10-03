package org.oucho.radio2.radio;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

public class RadioApplication extends Application {

    private static RadioApplication sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);

        setInstance(this);
    }

    public static synchronized RadioApplication getInstance() {
        return sInstance;
    }

    private static void setInstance(RadioApplication value) {
        sInstance = value;
    }
}
