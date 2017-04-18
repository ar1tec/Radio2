package org.oucho.radio2.utils;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.oucho.radio2.PlayerService;
import org.oucho.radio2.interfaces.RadioKeys;

public class State implements RadioKeys {


   public static final String STATE_STOP         = "Stop";
   public static final String STATE_ERROR        = "Error";
   public static final String STATE_PAUSE        = "Pause";
   public static final String STATE_PLAY         = "Play";
   public static final String STATE_BUFFER       = "Loading...";
    public static final String STATE_COMPLETED     = "Completed";
   public static final String STATE_DUCK         = "\\_o< coin";
   public static final String STATE_DISCONNECTED = "Disconnected";


    private static String current_state = STATE_STOP;

   private static boolean current_isNetworkUrl = false;


   public static void setState(Context context, String s, boolean isNetworkUrl) {

      if ( s == null )
         return;

      current_state = s;

      current_isNetworkUrl = isNetworkUrl;

      Intent intent = new Intent(INTENT_STATE);
      intent.putExtra("state", current_state);
      intent.putExtra("url", PlayerService.url);
      intent.putExtra("name", PlayerService.name);

      context.sendBroadcast(intent);
   }


    public static void getState(Context context) {
        setState(context, current_state, current_isNetworkUrl);
    }


   public static boolean is(String s) {
       return current_state.equals(s);
   }

   public static String text() {

      if (is(STATE_STOP))
          return "Stop";

       if (is(STATE_PLAY))
           return "Play";

       if (is(STATE_PAUSE))
           return "Pause";

       if (is(STATE_BUFFER))
           return "Loading...";

       if (is(STATE_DUCK))
           return "\\_o< coin";

       if (is(STATE_COMPLETED))
           return "Completed";

      if (is(STATE_ERROR))
          return "Error";

      if (is(STATE_DISCONNECTED))
          return "Disconnected";

      // Should not happen.
      return "Unknown";
   }

   public static boolean isPlaying() {
       return is(STATE_PLAY) || is(STATE_BUFFER) || is(STATE_DUCK);
   }

   public static boolean isStopped() {
       return State.is(STATE_STOP) || State.is(STATE_ERROR) || State.is(STATE_COMPLETED);
   }

    public static boolean isPaused() {
        return State.is(STATE_PAUSE);
    }

   public static boolean isWantPlaying() {
       return isPlaying() || is(STATE_ERROR);
   }


    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

}

