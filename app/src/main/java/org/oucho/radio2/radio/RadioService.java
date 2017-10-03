package org.oucho.radio2.radio;


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.RemoteViews;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player.EventListener;
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

import org.oucho.radio2.MainActivity;
import org.oucho.radio2.R;
import org.oucho.radio2.utils.ImageFactory;
import org.oucho.radio2.net.Connectivity;
import org.oucho.radio2.net.CustomHttpDataSource;
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
import static com.google.android.exoplayer2.Player.STATE_BUFFERING;
import static com.google.android.exoplayer2.Player.STATE_ENDED;
import static com.google.android.exoplayer2.Player.STATE_IDLE;
import static com.google.android.exoplayer2.Player.STATE_READY;
import static org.oucho.radio2.utils.State.isPaused;
import static org.oucho.radio2.utils.State.isPlaying;
import static org.oucho.radio2.utils.State.isWantPlaying;

public class RadioService extends Service implements RadioKeys, EventListener, OnAudioFocusChangeListener {

    private Context context = null;
    private String mUserAgent;
    private String launch_url = null;

    private static String url = null;
    private static String name = null;
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

    private NotificationUpdate notificationUpdateReceiver;

    private static boolean sIsServiceForeground = false;
    private static final int NOTIFY_ID = 32;
    private static boolean timer = false;

    @Override
    public void onCreate() {

        context = getApplicationContext();
        préférences = getSharedPreferences(PREF_FILE, MODE_PRIVATE);

        setUrl(préférences.getString("url", default_url));
        setName(préférences.getString("name", default_name));

        notificationUpdateReceiver = new NotificationUpdate();
        IntentFilter filter = new IntentFilter(INTENT_STATE);
        registerReceiver(notificationUpdateReceiver, filter);

        audio_manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        connectivity = new Connectivity(context,this);

        mUserAgent = Util.getUserAgent(this, APPLICATION_NAME);

        createExoPlayer();
    }


    public void onDestroy() {
        super.onDestroy();

        stopPlayback();

        if ( mExoPlayer != null ) {
            releaseExoPlayer();
        }

        if ( connectivity != null ) {
            connectivity.destroy();
            connectivity = null;
        }

        if ( notificationUpdateReceiver != null )
            unregisterReceiver(notificationUpdateReceiver);

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
            if (isPlaying() || isPaused() || isWantPlaying()) {

                Counter.timePasses();
                launch_url = null;
                audio_manager.abandonAudioFocus(this);

                mExoPlayer.stop();

                if ( playlist_task != null ) {
                    playlist_task.cancel(true);
                    playlist_task = null;
                }

                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    intentPlay(intent);
                    startPlayback(getUrl());

                }, 200);

            } else {
                intentPlay(intent); // récupère les infos pour les variables url, name etc.
                startPlayback(getUrl());
            }
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


    private void startPlayback(String url) {

        stopPlayback(false);

        if ( ! URLUtil.isValidUrl(url) )
            stopPlayback();

        if ( isNetworkUrl(url) && ! Connectivity.isConnected(context) ) {
            connectivity.dropped_connection();
            done();
        }

        int focus = audio_manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if ( focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED )
            stopPlayback();

        if ( isNetworkUrl(url) )
            WifiLocker.lock(context);


        playlist_task = new Playlist(this, url).start();

        Intent music = new Intent();
        music.setAction("org.oucho.musicplayer.STOP");
        music.putExtra("halt", "stop");
        sendBroadcast(music);

        done(State.STATE_BUFFER);
    }


    /***********************************************************************************************
     * Play/Pause/restart...
     **********************************************************************************************/

    @SuppressWarnings("UnusedReturnValue")
    public int playLaunch(String surl) {

        setUrl(surl);

        launch_url = null;

        if ( ! URLUtil.isValidUrl(surl) )
            return stopPlayback();

        launch_url = surl;

        try {

            mExoPlayer.setVolume(1.0f);

            if (mExoPlayer.getPlayWhenReady()) {
                mExoPlayer.setPlayWhenReady(false);
                mExoPlayer.stop();
            }

            if (surl != null) {
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

        Counter.timePasses();
        launch_url = null;
        audio_manager.abandonAudioFocus(this);
        WifiLocker.unlock();

        mExoPlayer.stop();


        if ( playlist_task != null ) {
            playlist_task.cancel(true);
            playlist_task = null;
        }

        Handler handler = new Handler();
        handler.postDelayed(() -> removeNotification(context), 500);



        if ( update_state ) {
            return done(State.STATE_STOP);
        } else {
            return done();
        }

    }

    @SuppressLint("StaticFieldLeak")
    private void pause() {

        if ( mExoPlayer == null || State.is(State.STATE_PAUSE) || ! isPlaying() )
            done();

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

        done(State.STATE_PAUSE);
    }


    private void restart() {

        if ( mExoPlayer == null || State.isStopped() )
            startPlayback(getUrl());

        mExoPlayer.setVolume(1.0f);

        if ( State.is(State.STATE_PLAY) || State.is(State.STATE_BUFFER) )
            done();

        if ( State.is(State.STATE_DUCK) )
            done(State.STATE_PLAY);

        int focus = audio_manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if ( focus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED )
            done();


        if ( pause_task != null )
            pause_task.cancel(true); pause_task = null;

        mExoPlayer.setPlayWhenReady(true);

        done(State.STATE_PLAY);
    }


    @SuppressWarnings("UnusedReturnValue")
    private void intentPlay(Intent intent) {

        if ( intent.hasExtra("url") )
            setUrl(intent.getStringExtra("url"));

        if ( intent.hasExtra("name") )
            setName(intent.getStringExtra("name"));

        Editor editor = préférences.edit();
        editor.putString("url", getUrl());
        editor.putString("name", getName());
        editor.apply();

        failure_ttl = initial_failure_ttl;
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
                startPlayback(getUrl());
            else
                connectivity.dropped_connection();
        }
    }

    @SuppressLint("StaticFieldLeak")
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

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {}


    private void createExoPlayer() {

        if (mExoPlayer != null) {
            releaseExoPlayer();
        }

        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this, null, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);

        TrackSelector trackSelector = new DefaultTrackSelector();

        LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE * 2));

        mExoPlayer = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector, loadControl);
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

        @SuppressWarnings("unchecked")
        DataSource.Factory dataSourceFactory = new CustomHttpDataSource(mUserAgent, transferListener);

        MediaSource mediaSource;

        if (sourceIsHLS) {
            mediaSource = new HlsMediaSource(Uri.parse(uriString), dataSourceFactory, 32, null, null);
        } else {
            ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
            mediaSource = new ExtractorMediaSource(Uri.parse(getUrl()), dataSourceFactory, extractorsFactory, null, null);
        }

        mExoPlayer.prepare(mediaSource);
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {}

    private void releaseExoPlayer() {
        mExoPlayer.release();
        mExoPlayer = null;
    }


    private void initializeExoPlayer() {
        RadioService.InitializeExoPlayerHelper initializeExoPlayerHelper = new RadioService.InitializeExoPlayerHelper();
        initializeExoPlayerHelper.execute();
    }


    @SuppressLint("StaticFieldLeak")
    private class InitializeExoPlayerHelper extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            String contentType;
            URLConnection connection;
            try {
                connection = new URL(getUrl()).openConnection();
                connection.connect();
                contentType = connection.getContentType();
                Log.v(TAG, "MIME type of stream: " + contentType);
                if (contentType != null && (contentType.contains("application/vnd.apple.mpegurl") || contentType.contains("application/x-mpegurl"))) {
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
            prepareExoPLayer(sourceIsHLS, getUrl());

            mExoPlayer.addListener(RadioService.this);
        }

    }


    private int done(String state) {

        if ( state != null )
            State.setState(context, state, isNetworkUrl());

        return done();
    }

    @SuppressWarnings("SameReturnValue")
    private int done() {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }




    private class NotificationUpdate extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String receiveIntent = intent.getAction();

            if (INTENT_UPDATENOTIF.equals(receiveIntent)) {

                String etat = intent.getStringExtra("state");
                String nom = intent.getStringExtra("name");
                String logo = intent.getStringExtra("logo");
                Bitmap logoBitmap = null;

                if (logo != null) {

                    Log.d(TAG, "if (logo != null)");

                    logoBitmap = ImageFactory.stringToBitmap(logo);
                }

                updateNotification(nom, etat, logoBitmap);
            }

            if (INTENT_STATE.equals(receiveIntent)) {

                String etat_lecture = intent.getStringExtra("state");

                // Traduction du texte
                String trad;
                if ("Play".equals(etat_lecture)) {
                    trad = context.getResources().getString(R.string.play);
                    updateNotification(getName(), trad, null);

                } else if ("Loading...".equals(etat_lecture)) {
                    trad = context.getResources().getString(R.string.loading);
                    updateNotification(getName(), trad, null);

                } else if ("Disconnected".equals(etat_lecture)){
                    trad = context.getResources().getString(R.string.disconnected);
                    updateNotification(getName(), trad, null);

                } else if ("Completed".equals(etat_lecture)){
                    trad = context.getResources().getString(R.string.disconnected);
                    updateNotification(getName(), trad, null);

                } else if ("Pause".equals(etat_lecture)){
                    trad = context.getResources().getString(R.string.pause);
                    updateNotification(getName(), trad, null);

                } else if ("Stop".equals(etat_lecture)){
                    trad = context.getResources().getString(R.string.stop);
                    updateNotification(getName(), trad, null);

                } else {
                    trad = etat_lecture;
                    updateNotification(getName(), trad, null);
                }
            }
        }
    }


    private void updateNotification(String radio_name, String action, Bitmap logo_radio) {

        SharedPreferences préférences = context.getSharedPreferences(PREF_FILE, MODE_PRIVATE);

        String img = préférences.getString("image_data", "");

        if( logo_radio == null && !img.equalsIgnoreCase("") ){

            byte[] img_byte_array = Base64.decode(img, Base64.DEFAULT);
            logo_radio = BitmapFactory.decodeByteArray(img_byte_array, 0, img_byte_array.length);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, "notif");

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentIntent(pendingIntent);

        if (!timer) {
            notificationBuilder.setSmallIcon(R.drawable.notification);
        } else {
            notificationBuilder.setSmallIcon(R.drawable.notification_sleeptimer);
        }

        notificationBuilder.setOngoing(true);

        Boolean unlockNotification;
        unlockNotification = "Play".equals(action);
        notificationBuilder.setOngoing(unlockNotification);

        Notification notification = notificationBuilder.build();

        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification);

        String locale_string;
        if ("Play".equals(action)) {
            locale_string = context.getResources().getString(R.string.play);
        } else if ("Loading...".equals(action)) {
            locale_string = context.getResources().getString(R.string.loading);
        } else {
            locale_string = action;
        }

        contentView.setTextViewText(R.id.notif_name, radio_name);
        contentView.setTextViewText(R.id.notif_text, locale_string);

        if (logo_radio != null)
            contentView.setImageViewBitmap(R.id.notif_ombre, logo_radio);

        notification.contentView = contentView;

        boolean startForeground = isPlaying();

        if (startForeground) {

            startForeground(NOTIFY_ID, notification);

        } else {

            if (sIsServiceForeground)
                stopForeground(false);


            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            assert notificationManager != null;
            notificationManager.notify(NOTIFY_ID, notification);
        }

        sIsServiceForeground = startForeground;
    }


    private void removeNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.cancel(NOTIFY_ID);
    }

    public static String getUrl() {
        return url;
    }

    private static void setUrl(String value) {
        url = value;
    }

    public static String getName() {
        return name;
    }

    private static void setName(String value) {
        name = value;
    }

    public static void timerOnOff(boolean onOff){
        timer = onOff;
    }

}
