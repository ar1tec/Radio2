package org.oucho.radio2;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.webkit.URLUtil;

import org.oucho.radio2.itf.RadioKeys;
import org.oucho.radio2.net.Connectivity;
import org.oucho.radio2.net.WifiLocker;
import org.oucho.radio2.utils.Counter;
import org.oucho.radio2.utils.Later;
import org.oucho.radio2.utils.Playlist;
import org.oucho.radio2.utils.State;

public class PlayerService extends Service
   implements
        RadioKeys,
      OnInfoListener,
      OnErrorListener,
      OnPreparedListener,
      OnAudioFocusChangeListener,
      OnCompletionListener {

   private static SharedPreferences préférences = null;

   private static Context context = null;

   private static final String default_url = null;
   private static final String default_name = null;
   public  static String name = null;
   public  static String url = null;

   private static MediaPlayer player = null;

   private static AudioManager audio_manager = null;

   private static Playlist playlist_task = null;
   private static AsyncTask<Integer,Void,Void> pause_task = null;

   private static Connectivity connectivity = null;
   private static final int initial_failure_ttl = 5;
   private static int failure_ttl = 0;

   private static String launch_url = null;

   private Later start_buffering_task = null;

   private Later stopSoonTask = null;

    @Override
   public void onCreate() {
      context = getApplicationContext();

      préférences = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
      url = préférences.getString("url", default_url);
      name = préférences.getString("name", default_name);

      audio_manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
      connectivity = new Connectivity(context,this);

   }

    public static String getName() {
        return name;
    }

   public void onDestroy() {

      stop();

      if ( player != null ) {

         player.release();
         player = null;


      }

      if ( connectivity != null ) {

         connectivity.destroy();
         connectivity = null;
      }

      super.onDestroy();
   }


   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {

      if ( intent == null || ! intent.hasExtra("action") )
         return done();


      if ( ! Counter.still(intent.getIntExtra("counter", Counter.now())) )
         return done();

      String action = intent.getStringExtra("action");

      if (action.equals(STOP)) {
         stop();
         return done();
      }
      
      if (action.equals(PAUSE)) {
         pause();
         return done();
      }

      if (action.equals(RESTART)) {
         restart();
         return done();
      }

      if (action.equals(PLAY)) {
         intentPlay(intent);
      }

      if (action.equals("vol1")) {
         setVolume(0.1f);
         return done();
      }

      if (action.equals("vol2")) {
         setVolume(0.2f);
         return done();
      }

      if (action.equals("vol3")) {
         setVolume(0.3f);
         return done();
      }

      if (action.equals("vol4")) {
         setVolume(0.4f);
         return done();
      }

      if (action.equals("vol5")) {
         setVolume(0.5f);
         return done();
      }

      if (action.equals("vol6")) {
         setVolume(0.6f);
         return done();
      }

      if (action.equals("vol7")) {
         setVolume(0.7f);
         return done();
      }

      if (action.equals("vol8")) {
         setVolume(0.8f);
         return done();
      }

      if (action.equals("vol9")) {
         setVolume(0.9f);
         return done();
      }

      if (action.equals("vol10")) {
         setVolume(1.0f);
         return done();
      }

      return done();
   }


   private int setVolume(float vol) {

      player.setVolume(vol, vol);
      return done();
   }

    @SuppressWarnings("UnusedReturnValue")
    private int intentPlay(Intent intent) {

        if ( intent.hasExtra("url") )
            url = intent.getStringExtra("url");

        if ( intent.hasExtra("name") )
            name = intent.getStringExtra("name");

        Editor editor = préférences.edit();
        editor.putString("url", url);
        editor.putString("name", name);
        editor.apply();

        failure_ttl = initial_failure_ttl;
        return play(url);
    }


   private int play() {
       return play(url);
   }

   private int play(String url) {

      stop(false);


      if ( ! URLUtil.isValidUrl(url) )
         return stop();


      if ( isNetworkUrl(url) && ! Connectivity.isConnected(context) ) {

         Log.d("PlayerService", "if ( isNetworkUrl(url) && ! Connectivity.isConnected(context) )");

         connectivity.dropped_connection();
         return done();
      }

      int focus = audio_manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

      if ( focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED )
         return stop();



      if ( player == null ) {

         player = new MediaPlayer();
         player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
         player.setAudioStreamType(AudioManager.STREAM_MUSIC);
         player.setOnPreparedListener(this);
         player.setOnInfoListener(this);
         player.setOnErrorListener(this);
         player.setOnCompletionListener(this);

      }

      if ( isNetworkUrl(url) )
         WifiLocker.lock(context, APPLICATION_NAME);

      playlist_task = new Playlist(this,url).start();

      start_buffering();


      Intent intent = new Intent();
      intent.setAction("org.oucho.musicplayer.STOP");
      intent.putExtra("halt", "stop");
      sendBroadcast(intent);

      return done(State.STATE_BUFFER);
   }


   @SuppressWarnings("UnusedReturnValue")
   public int playLaunch(String url) {

      launch_url = null;

      if ( ! URLUtil.isValidUrl(url) )
         return stop();


      launch_url = url;

      WifiLocker.unlock();

      if ( isNetworkUrl(url) )
         WifiLocker.lock(context, APPLICATION_NAME);

      try {

         player.setVolume(1.0f, 1.0f);
         player.setDataSource(context, Uri.parse(url));
         player.prepareAsync();

      } catch (Exception e) {
          return stop();
      }

      start_buffering();
      return done(State.STATE_BUFFER);
   }

   @Override
   public void onPrepared(MediaPlayer mp) {

      if ( mp.equals(player) ) {

         player.start();
         Counter.timePasses();
         failure_ttl = initial_failure_ttl;
         State.setState(context, State.STATE_PLAY, isNetworkUrl());
      }


   }

   public boolean isNetworkUrl() {
       return isNetworkUrl(launch_url);
   }

   private boolean isNetworkUrl(String check_url) {
       return ( check_url != null && URLUtil.isNetworkUrl(check_url) );
   }

   private int stop() {
       return stop(true);
   }

   private int stop(boolean update_state) {

      Log.d("PlayerService", "STOP");

      Counter.timePasses();
      launch_url = null;
      audio_manager.abandonAudioFocus(this);
      WifiLocker.unlock();

      if ( player != null ) {

         if ( player.isPlaying() )
            player.stop();

         player.reset();
         player.release();
         player = null;
      }

      if ( playlist_task != null ) {

         playlist_task.cancel(true);
         playlist_task = null;
      }

      if ( update_state )
         return done(State.STATE_STOP);
      else
         return done();
   }

   /************************************************************************************************
    * Reduce volume, for a short while, for a notification.
    ***********************************************************************************************/

    @SuppressWarnings("UnusedReturnValue")
    private int duck() {

        if ( State.is(State.STATE_DUCK) || ! State.isPlaying() )
            return done();

        player.setVolume(0.1f, 0.1f);
        return done(State.STATE_DUCK);
    }

    /***********************************************************************************************
     * Pause/restart...
     **********************************************************************************************/
   private int pause() {

      if ( player == null || State.is(State.STATE_PAUSE) || ! State.isPlaying() )
         return done();

      if ( pause_task != null )
         pause_task.cancel(true);

      pause_task =
         new Later() {

            @Override
            public void later() {

               pause_task = null;
               stop();
            }
         }.start();

      player.pause();
      return done(State.STATE_PAUSE);
   }

   private int restart() {

      if ( player == null || State.isStopped() )
         return play();

      player.setVolume(1.0f, 1.0f);

      if ( State.is(State.STATE_PLAY) || State.is(State.STATE_BUFFER) )
         return done();

      if ( State.is(State.STATE_DUCK) )
         return done(State.STATE_PLAY);

      int focus = audio_manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

      if ( focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED )
         return done();


      if ( pause_task != null )
          pause_task.cancel(true); pause_task = null;


      player.start();
      return done(State.STATE_PLAY);
   }

   private int done(String state) {

      if ( state != null )
         State.setState(context, state, isNetworkUrl());

      return done();
   }

   private int done() {
       return START_NOT_STICKY;
   }


   @Override
   public boolean onInfo(MediaPlayer player, int what, int extra) {

      switch (what) {
         case MediaPlayer.MEDIA_INFO_BUFFERING_START:
            State.setState(context, State.STATE_BUFFER, isNetworkUrl());
            break;

         case MediaPlayer.MEDIA_INFO_BUFFERING_END:
            failure_ttl = initial_failure_ttl;
            State.setState(context, State.STATE_PLAY, isNetworkUrl());
            break;

          default: //do nothing
            break;
      }
      return true;
   }


   private void start_buffering() {

      if ( start_buffering_task != null )
         start_buffering_task.cancel(true);

      // We'll give it 90 seconds for the stream to start.  Otherwise, we'll
      // declare an error.  onError() tries to restart, in some cases.
      start_buffering_task = (Later) new Later(90) {

            @Override
            public void later() {
               start_buffering_task = null;
               onError(null,0,0);
               Log.d("PlayerService", "start_buffering(), start_buffering_task" );
            }
         }.start();
   }


   private void stop_soon() {

      if ( stopSoonTask != null )
          stopSoonTask.cancel(true);

       stopSoonTask = (Later) new Later(300) {

            @Override
            public void later() {
                stopSoonTask = null;
               stop();
               Log.d("PlayerService", "stop_soon(), stopSoonTask" );

            }
         }.start();
   }

   private void tryRecover() {

      Log.d("PlayerService", "tryRecover()" );

      stop_soon();

      if ( isNetworkUrl() && 0 < failure_ttl ) {
         failure_ttl -= 1;

         if ( Connectivity.isConnected(context) )
            play();
         else
            connectivity.dropped_connection();
      }
   }

   @Override
   public boolean onError(MediaPlayer player, int what, int extra) {
      State.setState(context,State.STATE_ERROR, isNetworkUrl());
       tryRecover(); // This calls stop_soon().

      // Returning true, here, prevents the onCompletionlistener from being called.
      return true;
   }

   @Override
   public void onCompletion(MediaPlayer mp) {

      if ( ! isNetworkUrl() && (State.is(State.STATE_PLAY) || State.is(State.STATE_DUCK)) )
         State.setState(context, State.STATE_COMPLETE, isNetworkUrl());

      stop_soon();
   }

   @Override
   public void onAudioFocusChange(int change) {

      if ( player != null )
         switch (change) {

            case AudioManager.AUDIOFOCUS_GAIN:
               restart();
               break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
               // pause();
               // break;
               // Drop through.

            case AudioManager.AUDIOFOCUS_LOSS:
               pause();
               break;

             case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                 duck();
                 break;

             default: //do nothing
                 break;
         }
   }

   @Override
   public IBinder onBind(Intent intent) {
       return null;
   }
}
