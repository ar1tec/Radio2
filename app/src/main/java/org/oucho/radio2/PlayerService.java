package org.oucho.radio2;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.webkit.URLUtil;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;

import org.oucho.radio2.gui.Notification;
import org.oucho.radio2.net.CustomHttpDataSource;
import org.oucho.radio2.interfaces.RadioKeys;
import org.oucho.radio2.net.Connectivity;
import org.oucho.radio2.net.WifiLocker;
import org.oucho.radio2.utils.Counter;
import org.oucho.radio2.utils.Later;
import org.oucho.radio2.utils.Playlist;
import org.oucho.radio2.utils.State;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import static com.google.android.exoplayer2.ExoPlaybackException.TYPE_RENDERER;
import static com.google.android.exoplayer2.ExoPlaybackException.TYPE_SOURCE;
import static com.google.android.exoplayer2.ExoPlaybackException.TYPE_UNEXPECTED;
import static com.google.android.exoplayer2.ExoPlayer.STATE_BUFFERING;
import static com.google.android.exoplayer2.ExoPlayer.STATE_ENDED;
import static com.google.android.exoplayer2.ExoPlayer.STATE_IDLE;
import static com.google.android.exoplayer2.ExoPlayer.STATE_READY;
import static org.oucho.radio2.utils.State.isPlaying;

public class PlayerService extends Service
   implements
        RadioKeys,
        ExoPlayer.EventListener,
        OnAudioFocusChangeListener {


   private Context context = null;
   private String mUserAgent;
   private String launch_url = null;

   public static String url = null;
   public static String name = null;
   private final String default_url = null;
   private final String default_name = null;

   private Later stopSoonTask = null;
   private Playlist playlist_task = null;
   private Connectivity connectivity = null;
   private SimpleExoPlayer mExoPlayer = null;
   private AudioManager audio_manager = null;
   private SharedPreferences préférences = null;
   private AsyncTask<Integer,Void,Void> pause_task = null;

   private int failure_ttl = 0;
   private final int initial_failure_ttl = 5;
   private float currentVol = 1.0f;

   private final String TAG = "Player Service";

   private NotifUpdate notifUpdate_Receiver;


   @Override
   public void onCreate() {

      Log.i(TAG, "onCreate");

      context = getApplicationContext();
      préférences = getSharedPreferences(PREF_FILE, MODE_PRIVATE);

      url = préférences.getString("url", default_url);
      name = préférences.getString("name", default_name);

      notifUpdate_Receiver = new NotifUpdate();
      IntentFilter filter = new IntentFilter(INTENT_STATE);
      registerReceiver(notifUpdate_Receiver, filter);

      audio_manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
      connectivity = new Connectivity(context,this);

      mUserAgent = Util.getUserAgent(this, APPLICATION_NAME);

      createExoPlayer();
    }


   public static String getUrl() {
       return url;
    }


   public void onDestroy() {
      super.onDestroy();

      Log.i(TAG, "onDestroy");

      stopPlayback();

      if ( mExoPlayer != null ) {
         releaseExoPlayer();
      }

      if ( connectivity != null ) {
         connectivity.destroy();
         connectivity = null;
      }

      if ( notifUpdate_Receiver != null )
         unregisterReceiver(notifUpdate_Receiver);

      WifiLocker.unlock();
   }



   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {

      if ( intent == null ) {
         return done();
      }

      if ( ! Counter.still(intent.getIntExtra("counter", Counter.now())) )
         return done();

      String action = null;
      float voldown = 0.0f;

      if ( intent.hasExtra("action") )
         action = intent.getStringExtra("action");

      if ( intent.hasExtra("voldown") )
         voldown = intent.getFloatExtra("voldown", 1.0f);

      if (action != null && action.equals(ACTION_PLAY)) {
         intentPlay(intent); // récupère les infos pour les variables url, name etc.
         startPlayback(url);
      }

      if (action != null && action.equals(ACTION_STOP)) {
         stopPlayback();
         return done();
      }

      if (action != null && action.equals(ACTION_PAUSE)) {
         pause();
         return done();
      }

      if (action != null && action.equals(ACTION_RESTART)) {
         restart();
         return done();
      }

      if (voldown != 0.0f && voldown != currentVol) {
         setVolume(voldown);
         currentVol = voldown;
         return done();
      }

      return done();
   }


   private int startPlayback(String url) {

      Log.i(TAG, "startPlayback");

      stopPlayback(false);

      if ( ! URLUtil.isValidUrl(url) )
         return stopPlayback();

      if ( isNetworkUrl(url) && ! Connectivity.isConnected(context) ) {
         connectivity.dropped_connection();
         return done();
      }

      int focus = audio_manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

      if ( focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED )
         return stopPlayback();

      if ( isNetworkUrl(url) )
         WifiLocker.lock(context);



      playlist_task = new Playlist(this,url).start();

      Intent intent = new Intent();
      intent.setAction("org.oucho.musicplayer.STOP");
      intent.putExtra("halt", "stop");
      sendBroadcast(intent);

      return done(State.STATE_BUFFER);
   }


   /***********************************************************************************************
    * Play/Pause/restart...
    **********************************************************************************************/

   @SuppressWarnings("UnusedReturnValue")
   public int playLaunch(String url) {

      Log.i(TAG, "playLaunch");

      launch_url = null;

      if ( ! URLUtil.isValidUrl(url) )
         return stopPlayback();

      launch_url = url;

      try {

         mExoPlayer.setVolume(1.0f);

         if (mExoPlayer.getPlayWhenReady()) {
            mExoPlayer.setPlayWhenReady(false);
            mExoPlayer.stop();
         }

         if (url != null) {
            initializeExoPlayer();
            mExoPlayer.setPlayWhenReady(true);
         }

      } catch (Exception e) {
         return stopPlayback();
      }

      return done(State.STATE_BUFFER);
   }


   private int stopPlayback() {
      return stopPlayback(true);
   }

   private int stopPlayback(boolean update_state) {

      Log.i(TAG, "playLaunch");


      Counter.timePasses();
      launch_url = null;
      audio_manager.abandonAudioFocus(this);
      WifiLocker.unlock();

      mExoPlayer.stop();

      if ( playlist_task != null ) {
         playlist_task.cancel(true);
         playlist_task = null;
      }

      if ( update_state )
         return done(State.STATE_STOP);
      else
         return done();
   }

   private int pause() {

      if ( mExoPlayer == null || State.is(State.STATE_PAUSE) || ! isPlaying() )
         return done();

      if ( pause_task != null )
         pause_task.cancel(true);

      pause_task = new Later() {
                 @Override
                 public void later() {
                    pause_task = null;
                    stopPlayback();
                 }
              }.start();

      mExoPlayer.setPlayWhenReady(false);

      return done(State.STATE_PAUSE);
   }


   private int restart() {

      if ( mExoPlayer == null || State.isStopped() )
         return startPlayback(url);

      mExoPlayer.setVolume(1.0f);

      if ( State.is(State.STATE_PLAY) || State.is(State.STATE_BUFFER) )
         return done();

      if ( State.is(State.STATE_DUCK) )
         return done(State.STATE_PLAY);

      int focus = audio_manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

      if ( focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED )
         return done();


      if ( pause_task != null )
         pause_task.cancel(true); pause_task = null;

      mExoPlayer.setPlayWhenReady(true);

      return done(State.STATE_PLAY);
   }


   @SuppressWarnings("UnusedReturnValue")
   private void intentPlay(Intent intent) {

      if ( intent.hasExtra("url") )
         url = intent.getStringExtra("url");

      if ( intent.hasExtra("name") )
         name = intent.getStringExtra("name");

      Editor editor = préférences.edit();
      editor.putString("url", url);
      editor.putString("name", name);
      editor.apply();

      failure_ttl = initial_failure_ttl;
   }

   private class NotifUpdate extends BroadcastReceiver {

      @Override
      public void onReceive(Context context, Intent intent) {

         String receiveIntent = intent.getAction();

         if (INTENT_STATE.equals(receiveIntent)) {

            String etat_lecture = intent.getStringExtra("state");

            // Traduction du texte
            String trad;
            if ("Play".equals(etat_lecture)) {
               trad = context.getResources().getString(R.string.play);
               Notification.updateNotification(context, name, trad, null);

            } else if ("Loading...".equals(etat_lecture)) {
               trad = context.getResources().getString(R.string.loading);
               Notification.updateNotification(context, name, trad, null);

            } else if ("Disconnected".equals(etat_lecture)){
               trad = context.getResources().getString(R.string.disconnected);
               Notification.updateNotification(context, name, trad, null);

            } else if ("Completed".equals(etat_lecture)){
               trad = context.getResources().getString(R.string.disconnected);
               Notification.updateNotification(context, name, trad, null);

            } else if ("Pause".equals(etat_lecture)){
               trad = etat_lecture;
               Notification.updateNotification(context, name, trad, null);

            } else if ("Stop".equals(etat_lecture)){
               trad = etat_lecture;
               Notification.updateNotification(context, name, trad, null);

            } else {
               trad = etat_lecture;
               Notification.updateNotification(context, name, trad, null);
            }

         }
      }
   }


   public boolean isNetworkUrl() {
      return isNetworkUrl(launch_url);
   }

   private boolean isNetworkUrl(String check_url) {
      return ( check_url != null && URLUtil.isNetworkUrl(check_url) );
   }

   @Override
   public void onAudioFocusChange(int change) {

      if ( mExoPlayer != null )
         switch (change) {

            case AudioManager.AUDIOFOCUS_GAIN:
               restart();
               break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS:
               pause();
               break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
               duck();
               break;

            default:
               break;
         }
   }

   @SuppressWarnings("UnusedReturnValue")
   private int duck() {

      if ( State.is(State.STATE_DUCK) || ! isPlaying() )
         return done();

      mExoPlayer.setVolume(0.1f);
      return done(State.STATE_DUCK);
   }

   private void setVolume(float vol) {
      mExoPlayer.setVolume(vol);
   }


   @Override
   public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

      switch (playbackState) {
         case STATE_BUFFERING:
            State.setState(context, State.STATE_BUFFER, isNetworkUrl());
            // The player is not able to immediately play from the current position.
            break;

         case STATE_ENDED:
            // The player has finished playing the media.
            break;

         case STATE_IDLE:
            // The player does not have a source to play, so it is neither buffering nor ready to play.
            break;

         case STATE_READY:
            // The player is able to immediately play from the current position.
            failure_ttl = initial_failure_ttl;
            State.setState(context, State.STATE_PLAY, isNetworkUrl());

            break;

         default:
            // default
            break;
      }
   }


   @Override
   public void onPlayerError(ExoPlaybackException error) {
      switch (error.type) {
         case TYPE_RENDERER:
            // error occurred in a Renderer. Playback state: ExoPlayer.STATE_IDLE
            Log.e(TAG, "An error occurred. Type RENDERER: " + error.getRendererException().toString());
            break;

         case TYPE_SOURCE:
            // error occurred loading data from a MediaSource. Playback state: ExoPlayer.STATE_IDLE
            Log.e(TAG, "An error occurred. Type SOURCE: " + error.getSourceException().toString());

            tryRecover();

            break;

         case TYPE_UNEXPECTED:
            // error was an unexpected RuntimeException. Playback state: ExoPlayer.STATE_IDLE
            Log.e(TAG, "An error occurred. Type UNEXPECTED: " + error.getUnexpectedException().toString());
            break;

         default:
            Log.w(TAG, "An error occurred. Type OTHER ERROR.");
            tryRecover();
            break;
      }
   }


   private void tryRecover() {

      stop_soon();

      if ( isNetworkUrl() && 0 < failure_ttl ) {
         failure_ttl -= 1;

         if ( Connectivity.isConnected(context) )
            startPlayback(url);
         else
            connectivity.dropped_connection();
      }
   }

   private void stop_soon() {

      if ( stopSoonTask != null )
         stopSoonTask.cancel(true);

      stopSoonTask = (Later) new Later(300) {

         @Override
         public void later() {
            stopSoonTask = null;
            stopPlayback();
         }
      }.start();
   }

   @Override
   public void onLoadingChanged(boolean isLoading) {

      String state;
      if (isLoading) {

         state = "Media source is currently being loaded.";
      } else {
         state = "Media source is currently not being loaded.";
      }
      Log.v(TAG, "State of loading has changed: " + state);
   }


   @Override
   public void onTimelineChanged(Timeline timeline, Object manifest) {}

   @Override
   public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {}

   @Override
   public void onPositionDiscontinuity() {}


   private void createExoPlayer() {

      if (mExoPlayer != null) {
         releaseExoPlayer();
      }

      TrackSelector trackSelector = new DefaultTrackSelector();

      LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE * 2));

      mExoPlayer = ExoPlayerFactory.newSimpleInstance(getApplicationContext(), trackSelector, loadControl);
   }

   private void prepareExoPLayer(boolean sourceIsHLS, String uriString) {

      TransferListener transferListener = new TransferListener() {
         @Override
         public void onTransferStart(Object source, DataSpec dataSpec) {
            Log.v(TAG, "onTransferStart\nSource: " + source.toString() + "\nDataSpec: " + dataSpec.toString());
         }

         @Override
         public void onBytesTransferred(Object source, int bytesTransferred) {

         }

         @Override
         public void onTransferEnd(Object source) {
            Log.v(TAG, "onTransferEnd\nSource: " + source.toString());
         }
      };

      DataSource.Factory dataSourceFactory = new CustomHttpDataSource(mUserAgent, transferListener);

      MediaSource mediaSource;

      if (sourceIsHLS) {
         mediaSource = new HlsMediaSource(Uri.parse(uriString),
                 dataSourceFactory, 32, null, null);
      } else {
         ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
         mediaSource = new ExtractorMediaSource(Uri.parse(url),
                 dataSourceFactory, extractorsFactory, 32, null, null, null); // todo attach listener here
      }

      mExoPlayer.prepare(mediaSource);
   }


   private void releaseExoPlayer() {
      mExoPlayer.release();
      mExoPlayer = null;
   }


   private void initializeExoPlayer() {
      PlayerService.InitializeExoPlayerHelper initializeExoPlayerHelper = new PlayerService.InitializeExoPlayerHelper();
      initializeExoPlayerHelper.execute();
   }


   private class InitializeExoPlayerHelper extends AsyncTask<Void, Void, Boolean> {

      @Override
      protected Boolean doInBackground(Void... voids) {
         String contentType;
         URLConnection connection;
         try {
            connection = new URL(url).openConnection();
            connection.connect();
            contentType = connection.getContentType();
            Log.v(TAG, "MIME type of stream: " + contentType);
            if (contentType.contains("application/vnd.apple.mpegurl") || contentType.contains("application/x-mpegurl")) {
               Log.v(TAG, "HTTP Live Streaming detected.");
               return true;
            } else {
               return false;
            }
         } catch (IOException e) {
            e.printStackTrace();
            return false;
         }
      }

      @Override
      protected void onPostExecute(Boolean sourceIsHLS) {

         String uriString = url;

         prepareExoPLayer(sourceIsHLS, uriString);
         mExoPlayer.addListener(PlayerService.this);
      }

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
   public IBinder onBind(Intent intent) {
      return null;
   }

}
