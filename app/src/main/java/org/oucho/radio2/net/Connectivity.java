package org.oucho.radio2.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import org.oucho.radio2.utils.Counter;
import org.oucho.radio2.utils.Later;
import org.oucho.radio2.PlayerService;
import org.oucho.radio2.utils.State;

public class Connectivity extends BroadcastReceiver {

   private static ConnectivityManager connectivity = null;

   private Context context = null;
   private PlayerService player = null;
   private static final int TYPE_NONE = -1;

   private static int previous_type = TYPE_NONE;

    private static AsyncTask<Integer,Void,Void> disable_task = null;
    private int then = 0;


   public Connectivity(Context a_context, PlayerService a_player) {

      //Log.d("Connectivity", "Connectivity");

      context = a_context;
      player = a_player;

      initConnectivity(context);
      context.registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
   }

   static private void initConnectivity(Context context) {

      //Log.d("Connectivity", "initConnectivity");


      if ( connectivity == null )
         connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      if ( connectivity != null )
         previous_type = getType();
   }

   public void destroy() {

      //Log.d("Connectivity", "destroy");


      context.unregisterReceiver(this);
   }

   static private int getType() {

      //Log.d("Connectivity", "getType");


      return getType(null);
   }

   static private int getType(Intent intent) {

      //Log.d("Connectivity", "getType2");


      if (connectivity == null)
         return TYPE_NONE;

      if ( intent != null && intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false) )
         return TYPE_NONE;

      NetworkInfo network = connectivity.getActiveNetworkInfo();
      if ( network != null && network.isConnected() ) {

         int type = network.getType();
         switch (type) {
            // These cases all fall through.
            case ConnectivityManager.TYPE_WIFI:
            case ConnectivityManager.TYPE_MOBILE:
            case ConnectivityManager.TYPE_WIMAX:
               if ( network.getState() == NetworkInfo.State.CONNECTED )
                  return type;

             default:
                 break;
         }
      }

      return TYPE_NONE;
   }

   public static boolean onWifi() {

      //Log.d("Connectivity", "onWifi: " + ConnectivityManager.TYPE_WIFI);


      return previous_type == ConnectivityManager.TYPE_WIFI;
   }

   static public boolean isConnected(Context context) {

       initConnectivity(context);


       //Log.d("Connectivity", "isConnected: " + (getType() != TYPE_NONE));


      return (getType() != TYPE_NONE);
   }



   @Override
   public void onReceive(Context context, Intent intent) {

      //Log.d("Connectivity", "onReceive");


      int type = getType(intent);

       //Log.d("Connectivity", "onReceive:" + getType(intent) );

      boolean want_network_playing = State.isWantPlaying() && player.isNetworkUrl();

      if ( type == TYPE_NONE && previous_type != TYPE_NONE && want_network_playing )
         dropped_connection();

      if ( previous_type == TYPE_NONE && type != previous_type && Counter.still(then) )
         restart();

      if ( previous_type != TYPE_NONE && type != TYPE_NONE && type != previous_type && want_network_playing )
         restart();

      previous_type = type;
   }







   public void dropped_connection() {  // We've lost connectivity.

      //Log.d("Connectivity", "dropped_connection");


      player.stop();
      then = Counter.now();
      State.setState(context, State.STATE_DISCONNECTED, true);

      if ( disable_task != null )
         disable_task.cancel(true);

      disable_task = new Later(300) {
            @Override
            public void later() {
               player.stop();
               disable_task = null;
            }
         }.start();
   }

   private void restart() {

      //Log.d("Connectivity", "restart");


      if ( disable_task != null ) {
         disable_task.cancel(true);
         disable_task = null;
      }

      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
      if ( settings.getBoolean("reconnect", false) )
         player.play();
   }
}

