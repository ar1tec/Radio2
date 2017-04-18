package org.oucho.radio2.interfaces;


public interface RadioKeys {

    String APPLICATION_NAME = "Radio";

    int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    String DB_NAME = "WebRadio";
    String TABLE_NAME =  "WebRadio";

    String PREF_FILE = "org.oucho.radio2_preferences";

    String ACTION_PLAY = "play";
    String ACTION_STOP = "stop";
    String ACTION_PAUSE = "pause";
    String ACTION_RESTART = "restart";
    String ACTION_QUIT = "quit";

    String INTENT_STATE = "org.oucho.radio2.INTENT_STATE";

}
