package org.oucho.radio2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.oucho.radio2.xml.DatabaseSave;
import org.oucho.radio2.db.RadiosDatabase;
import org.oucho.radio2.xml.ReadXML;
import org.oucho.radio2.xml.XMLParser;
import org.oucho.radio2.xml.XmlValuesModel;
import org.oucho.radio2.filepicker.FilePicker;
import org.oucho.radio2.filepicker.FilePickerActivity;
import org.oucho.radio2.filepicker.FilePickerParcelObject;
import org.oucho.radio2.dialog.Permissions;
import org.oucho.radio2.images.ImageFactory;
import org.oucho.radio2.interfaces.ListsClickListener;
import org.oucho.radio2.interfaces.PlayableItem;
import org.oucho.radio2.db.Radio;
import org.oucho.radio2.gui.RadioAdapter;
import org.oucho.radio2.interfaces.RadioKeys;
import org.oucho.radio2.audio.VolumeTimer;
import org.oucho.radio2.update.CheckUpdate;
import org.oucho.radio2.dialog.About;
import org.oucho.radio2.audio.GetAudioFocusTask;
import org.oucho.radio2.gui.Notification;
import org.oucho.radio2.utils.SeekArc;
import org.oucho.radio2.utils.State;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class MainActivity extends AppCompatActivity
        implements
        RadioKeys,
        NavigationView.OnNavigationItemSelectedListener,
        View.OnClickListener {

    private static String nom_radio = "";
    private static String etat_lecture = "";
    private static String imp_exp = "null";
    private static String importType = "null";

    private boolean bitrate = false;
    private static boolean running;
    private static ScheduledFuture mTask;

    private View editView;
    private Handler handler;
    private Bitmap logoRadio;
    private Quit quitReceiver;
    private VolumeTimer volume;
    private TextView timeAfficheur;
    private RecyclerView radioView;
    private CountDownTimer timerEcran;
    private DrawerLayout mDrawerLayout;
    private MediaPlayer soundChargement;
    private SharedPreferences préférences;
    private Etat_player Etat_player_Receiver;

    private Context context;


    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        préférences = getSharedPreferences(PREF_FILE, MODE_PRIVATE);

        setStatusBar();
        setToolBarAndNavigationDrawer();


        Etat_player_Receiver = new Etat_player();
        IntentFilter filter = new IntentFilter(STATE);
        registerReceiver(Etat_player_Receiver, filter);

        quitReceiver = new Quit();
        IntentFilter filter2 = new IntentFilter(QUIT);
        registerReceiver(quitReceiver, filter2);

        volume = new VolumeTimer();
        Control_Volume niveau_Volume = new Control_Volume(this, new Handler());
        getApplicationContext().getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, niveau_Volume);

        radioView = (RecyclerView)findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        radioView.setLayoutManager(layoutManager);

        this.findViewById(R.id.add).setOnClickListener(this);
        this.findViewById(R.id.stop).setOnClickListener(this);
        this.findViewById(R.id.play).setOnClickListener(this);
        this.findViewById(R.id.pause).setOnClickListener(this);

        soundChargement = MediaPlayer.create(context, R.raw.connexion);
        soundChargement.setLooping(true);

        getBitRate();
        bitrate = true;

        volume();
        State.getState(context);
        CheckUpdate.onStart(this);
    }

    private void setStatusBar() {

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final int mUIFlag = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            getWindow().getDecorView().setSystemUiVisibility(mUIFlag);
            getWindow().setStatusBarColor(ContextCompat.getColor(context, R.color.white));
        }
    }

    private void setToolBarAndNavigationDrawer() {

        int couleurTitre = ContextCompat.getColor(context, R.color.colorAccent);
        int couleurFond = ContextCompat.getColor(context, R.color.colorPrimary);
        String titre = context.getString(R.string.app_name);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ColorDrawable colorDrawable = new ColorDrawable(couleurFond);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setBackgroundDrawable(colorDrawable);

        if (android.os.Build.VERSION.SDK_INT >= 24) {
            actionBar.setTitle(Html.fromHtml("<font color='" + couleurTitre + "'>" + titre + "</font>", Html.FROM_HTML_MODE_LEGACY));
        } else {
            actionBar.setTitle(Html.fromHtml("<font color='" + couleurTitre + "'>" + titre + "</font>"));
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        NavigationView mNavigationView = (NavigationView) findViewById(R.id.navigation_view);

        assert mNavigationView != null;
        mNavigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                toolbar,
                R.string.drawer_open,
                R.string.drawer_close) {};

        mDrawerToggle.syncState();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        final Drawable upArrow = ContextCompat.getDrawable(context, R.drawable.ic_menu_black_24dp);
        upArrow.setColorFilter(ContextCompat.getColor(context, R.color.controls_tint_light), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);
    }


   /* **********************************************************************************************
    * Pause / résume / etc.
    * *********************************************************************************************/

    @Override
    protected void onPause() {
        super.onPause();

        killNotif();

        if (bitrate) {
            stopBitrate();
            bitrate = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!bitrate) {
            getBitRate();
            bitrate = true;
        }

        if (State.isStopped()) {
            TextView status = (TextView) findViewById(R.id.etat);
            etat_lecture = "Stop";
            assert status != null;
            status.setText(etat_lecture);
        }

        if (running)
            showTimeEcran();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (bitrate)
            stopBitrate();

        killNotif();

        soundChargement.release();

        try {
            unregisterReceiver(Etat_player_Receiver);
            unregisterReceiver(quitReceiver);
        } catch (IllegalArgumentException ignore) {}
    }


   /* **********************************************************************************************
    *
    * Broadcast receiver
    *
    * *********************************************************************************************/

    private class Etat_player extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {


            String receiveIntent = intent.getAction();

            if (STATE.equals(receiveIntent)) {

                TextView status = (TextView) findViewById(R.id.etat);

                etat_lecture = intent.getStringExtra("state");
                nom_radio = intent.getStringExtra("name");


                // Traduction du texte
                String trad;
                if ("Play".equals(etat_lecture)) {
                    trad = context.getResources().getString(R.string.play);
                } else if ("Loading...".equals(etat_lecture)) {
                    trad = context.getResources().getString(R.string.loading);;
                } else {
                    trad = etat_lecture;
                }

                assert status != null;
                status.setText(trad);

                updateNomRadio();
                updateListView();
                updatePlayPause();

                if (etat_lecture.equals("Loading...")) {
                    soundChargement.start();
                } else if (soundChargement.isPlaying()) {
                    soundChargement.pause();
                }
            }
        }
    }

    private class Quit extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String receiveIntent = intent.getAction();

            if (QUIT.equals(receiveIntent))
                exit();
        }
    }


   /* *********************************
    * Affiche le nom de la radio active
    * *********************************/

    private void updateNomRadio() {

        TextView StationTextView = (TextView) findViewById(R.id.station);

        if (nom_radio == null)
            nom_radio = préférences.getString("name", "");

        assert StationTextView != null;
        StationTextView.setText(nom_radio);
    }


   /* ****************************
    * Changement d'état play/pause
    * ****************************/

    @SuppressWarnings("ConstantConditions")
    private void updatePlayPause() {
        ImageView equalizer = (ImageView) findViewById(R.id.icon_equalizer);

        if (State.isPlaying() || State.isPaused()) {
            equalizer.setBackground(getDrawable(R.drawable.ic_equalizer1));
        } else {
            equalizer.setBackground(getDrawable(R.drawable.ic_equalizer0));
        }
    }


   /* *********************************************************************************************
    * Menu
    * ********************************************************************************************/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.set_timer:
                if (!running) {
                    showDatePicker();
                } else {
                    showTimerInfo();
                }
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

   /* **********************************************************************************************
    * Navigation Drawer
    * *********************************************************************************************/

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        mDrawerLayout.closeDrawers();

        switch (menuItem.getItemId()) {
            case R.id.action_export:
                exporter();
                break;
            case R.id.action_import:
                importer();
                break;
            case R.id.nav_update:
                CheckUpdate.withInfo(this);
                break;
            case R.id.nav_help:
                about();
                break;
            case R.id.nav_exit:
                exit();
                break;
            default:
                break;
        }
        return true;
    }


   /* *******
    * Quitter
    * *******/


    private void exit() {
        soundChargement.release();

        if (bitrate)
            stopBitrate();

        stopTimer();

        Intent player = new Intent(this, PlayerService.class);
        player.putExtra("action", ACTION_STOP);
        startService(player);

        unregisterReceiver(Etat_player_Receiver);
        unregisterReceiver(quitReceiver);

        killNotif();
        finish();
    }


    /* **********************************************************************************************
    * Gestion des clicks sur l'interface
    * *********************************************************************************************/

    @Override
    public void onClick(View v) {

        Intent player = new Intent(this, PlayerService.class);

        switch (v.getId()) {
            case R.id.stop:
                player.putExtra("action", ACTION_STOP);
                startService(player);
                break;
            case R.id.play:
                switch (etat_lecture) {
                    case "Stop":
                        player.putExtra("action", ACTION_PLAY);
                        startService(player);
                        break;
                    case "Pause":
                        player.putExtra("action", ACTION_RESTART);
                        startService(player);
                        break;
                    default:
                        break;
                }
                break;

            case R.id.pause:

                switch (etat_lecture) {
                    case "Play":
                        player.putExtra("action", ACTION_PAUSE);
                        startService(player);
                        break;
                    case "Pause":
                        player.putExtra("action", ACTION_RESTART);
                        startService(player);
                        break;
                    default:
                        break;
                }
                break;

            case R.id.add:
                editRadio(null);
                break;
            default:
                break;
        }
    }



   /* *********************************************************************************************
    * Mise à jour de la vue de la liste des radios
    * ********************************************************************************************/

    private void updateListView() {
        ArrayList<Object> items = new ArrayList<>();
        items.addAll(Radio.getRadios(context));

        radioView.setAdapter(new RadioAdapter(this, items, nom_radio, clickListener));

        // pas fichu de trouver une façon d'extraire directement de l'Object
        List<String> lst = new ArrayList<>();
        lst.addAll(Radio.getListe(context));

        String url = PlayerService.getUrl();

        if (PlayerService.getUrl() == null) {
            url = préférences.getString("url", null);
        }

        for (int i = 0; i < items.size(); i++) {
            if (lst.get(i).equals(url))
                radioView.scrollToPosition( i );
        }
    }


   /* *********************************************************************************************
    * Click radio et menu radio
    * ********************************************************************************************/

    private final ListsClickListener clickListener = new ListsClickListener() {

        @Override
        public void onPlayableItemClick(PlayableItem item) {
            play((Radio)item);
        }

        @Override
        public void onPlayableItemMenuClick(PlayableItem item, int menuId) {
            switch(menuId) {
                case R.id.menu_edit:
                    editRadio((Radio)item);
                    break;
                case R.id.menu_delete:
                    deleteRadio((Radio)item);
                    break;
                default:
                    break;
            }
        }
    };

   /* **********************************************************************************************
    * Lecture de la radio
    * *********************************************************************************************/

    private void play(Radio radio) {

        String url = radio.getPlayableUri();
        String name = radio.getName();
        byte[] logo = radio.getLogo();

        SharedPreferences.Editor edit = préférences.edit();

        Intent player = new Intent(this, PlayerService.class);

        player.putExtra("action", ACTION_PLAY);
        player.putExtra("url", url);
        player.putExtra("name", name);
        startService(player);

        if (logo != null) {
            Bitmap logoBitmap = ImageFactory.getImage(radio.getLogo());
            Notification.updateNotification(context, nom_radio, "Play", logoBitmap);
            String encodedImage = Base64.encodeToString(logo, Base64.DEFAULT);
            edit.putString("image_data",encodedImage);
            edit.apply();
        } else {
            Notification.updateNotification(context, nom_radio, "Play",
                    BitmapFactory.decodeResource(context.getResources(),
            R.drawable.ic_radio_white_36dp));

            edit.remove("image_data");
            edit.apply();

        }

    }



   /* **********************************************************************************************
    * Suppression de la radio
    * *********************************************************************************************/

    private void deleteRadio(final Radio radio) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(R.string.deleteRadioConfirm));
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Radio.deleteRadio(context, radio);
                updateListView();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }




   /* **********************************************************************************************
    * Ajout ou édition d'une radio
    * *********************************************************************************************/

    private void editRadio(final Radio oldRadio) {

        logoRadio = null;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        int title = oldRadio == null ? R.string.addRadio : R.string.edit;

        builder.setTitle(getResources().getString(title));

        editView = getLayoutInflater().inflate(R.layout.layout_editwebradio, null);
        builder.setView(editView);

        final EditText editTextUrl = (EditText) editView.findViewById(R.id.editTextUrl);
        final EditText editTextName = (EditText) editView.findViewById(R.id.editTextName);
        final ImageView editLogo = (ImageView) editView.findViewById(R.id.logo);
        final TextView text = (TextView) editView.findViewById(R.id.texte);

        editLogo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addImg();
            }
        });

        if(oldRadio!=null) {
            editTextUrl.setText(oldRadio.getUrl());
            editTextName.setText(oldRadio.getName());

            if (oldRadio.getImg() != null ) {
                editLogo.setImageBitmap(logoRadio);
                editLogo.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
                text.setVisibility(View.INVISIBLE);
                editLogo.setImageBitmap(ImageFactory.getImage(oldRadio.getImg()));
                logoRadio = ImageFactory.getImage(oldRadio.getImg());
            }
        }

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String url = editTextUrl.getText().toString();
                String name = editTextName.getText().toString();
                byte[] img = null;

                if (logoRadio != null) {
                    img = ImageFactory.getBytes(logoRadio);
                }

                if("".equals(url) || "http://".equals(url)) {
                    Toast.makeText(context, R.string.errorInvalidURL, Toast.LENGTH_SHORT).show();
                    return;
                }

                if("".equals(name))
                    name = url;

                if(oldRadio != null) {
                    Radio.deleteRadio(context, oldRadio);
                }

                Radio newRadio = new Radio(url, name, img);
                Radio.addRadio(context, newRadio);
                updateListView();
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                updateListView();
            }
        });


        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }



   /* **********************************************************************************************
    * Fermeture notification
    * *********************************************************************************************/

    private void killNotif() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @SuppressLint("SetTextI18n")
            public void run() {

                if (!State.isPlaying())
                    Notification.removeNotification(context);

            }
        }, 500);
    }



   /* **********************************************************************************************
    * Volume observer
    * *********************************************************************************************/

    private class Control_Volume extends ContentObserver {
        private int previousVolume;
        private final Context context;

        private Control_Volume(Context c, Handler handler) {
            super(handler);
            context=c;

            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            previousVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            volume();

            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);

            int delta=previousVolume-currentVolume;

            if (delta >0) {
                previousVolume=currentVolume;
            }
            else if(delta<0) {
                previousVolume=currentVolume;
            }
        }
    }


    /* ******************************************
     * Gestion de l'affichage de l'icon de volume
     * ******************************************/

    private void volume() {

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        ImageView play = (ImageView) findViewById(R.id.icon_volume);

        if (currentVolume == 0) {
            assert play != null;
            play.setBackground(getDrawable(R.drawable.volume0));
        } else if (currentVolume < 4) {
            assert play != null;
            play.setBackground(getDrawable(R.drawable.volume1));
        } else if (currentVolume < 7) {
            assert play != null;
            play.setBackground(getDrawable(R.drawable.volume2));
        } else if (currentVolume < 10) {
            assert play != null;
            play.setBackground(getDrawable(R.drawable.volume3));
        } else if (currentVolume < 13) {
            assert play != null;
            play.setBackground(getDrawable(R.drawable.volume4));
        } else if (currentVolume < 16) {
            assert play != null;
            play.setBackground(getDrawable(R.drawable.volume5));
        }
    }



   /* **********************************************************************************************
    * Get bitrate
    * *********************************************************************************************/

    private void getBitRate() {
        handler = new Handler();
        handler.postDelayed(new Runnable() {

            public void run() {
                bitRate();
                handler.postDelayed(this, 2000);
            }
        }, 1);
    }


    private void bitRate() {
        final int uid = android.os.Process.myUid();
        final long received = TrafficStats.getUidRxBytes(uid) / 1024;

        handler = new Handler();
        handler.postDelayed(new Runnable() {

            @SuppressLint("SetTextI18n")
            public void run() {
                long current = TrafficStats.getUidRxBytes(uid) / 1024;
                long total = current - received;
                long ByteToBit = total * 8;
                TextView BitRate = (TextView) findViewById(R.id.bitrate);

                if (ByteToBit <= 1024 ) {
                    String bitrate = String.valueOf(ByteToBit);
                    assert BitRate != null;
                    BitRate.setText(bitrate + " Kb/s");
                } else {
                    long megaBit = ByteToBit / 1024;
                    String bitrate = String.valueOf(megaBit);
                    assert BitRate != null;
                    BitRate.setText(bitrate + " Mb/s");
                }
            }
        }, 1000);
    }


    private void stopBitrate() {

        if (bitrate) {
            handler.removeCallbacksAndMessages(null);
            bitrate = false;
        }
    }

   /* **********************************************************************************************
    * Sleep Timer
    * *********************************************************************************************/

    private void showDatePicker() {

        final String start = getString(R.string.start);
        final String cancel = getString(R.string.cancel);
        final SeekArc mSeekArc;
        final TextView mSeekArcProgress;

        @SuppressLint("InflateParams")
        View view = getLayoutInflater().inflate(R.layout.date_picker_dialog, null);
        mSeekArc = (SeekArc) view.findViewById(R.id.seekArc);
        mSeekArcProgress = (TextView) view.findViewById(R.id.seekArcProgress);

        mSeekArc.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {

            @Override
            public void onStopTrackingTouch() {
                // vide, obligatoire
            }

            @Override
            public void onStartTrackingTouch() {
                // vide, obligatoire
            }

            @Override
            public void onProgressChanged(int progress) {
                String minute;

                if (progress <= 1){
                    minute = "minute";
                } else {
                    minute = "minutes";
                }

                String temps = String.valueOf(progress) + " " + minute;

                mSeekArcProgress.setText(temps);
            }
        });



        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(start, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                int mins = mSeekArc.getProgress();
                startTimer(mins);
            }
        });

        builder.setNegativeButton(cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // This constructor is intentionally empty, pourquoi ? parce que !
            }
        });

        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();
    }


   /* ***********
    * Start timer
    * ***********/

    private void startTimer(final int minutes) {

        final String impossible = getString(R.string.impossible);
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final int delay = (minutes * 60) * 1000;

        if (delay == 0) {
            Toast.makeText(this, impossible, Toast.LENGTH_LONG).show();
            return;
        }

        mTask = scheduler.schedule(new GetAudioFocusTask(this), delay, TimeUnit.MILLISECONDS);

        Notification.setState(true);
        running = true;
        State.getState(context);
        showTimeEcran();
        volume.baisser(context, mTask, delay);
    }


   /* ***************************************
    * Afficher temps restant dans AlertDialog
    * ***************************************/

    private void showTimerInfo() {
        final String continuer = getString(R.string.continuer);
        final String cancelTimer = getString(R.string.cancel_timer);

        if (mTask.getDelay(TimeUnit.MILLISECONDS) < 0) {
            stopTimer();
            return;
        }

        @SuppressLint("InflateParams")
        View view = getLayoutInflater().inflate(R.layout.timer_info_dialog, null);

        final TextView timeLeft = ((TextView) view.findViewById(R.id.time_left));

        final AlertDialog dialog = new AlertDialog.Builder(this).setPositiveButton(continuer, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }

        }).setNegativeButton(cancelTimer, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                stopTimer();
            }
        }).setView(view).create();

        new CountDownTimer(mTask.getDelay(TimeUnit.MILLISECONDS), 1000) {
            @Override
            public void onTick(long seconds) {

                long secondes = seconds;
                secondes = secondes / 1000;

                String textTemps = String.format(getString(R.string.timer_info),  ((secondes % 3600) / 60), ((secondes % 3600) % 60));

                timeLeft.setText(textTemps);
            }

            @Override
            public void onFinish() {
                dialog.dismiss();
            }
        }.start();

        dialog.show();
    }


   /* ********************************
    * Afficher temps restant à l'écran
    * ********************************/

    private void showTimeEcran() {

        timeAfficheur = ((TextView) findViewById(R.id.time_ecran));

        assert timeAfficheur != null;
        timeAfficheur.setVisibility(View.VISIBLE);

        timerEcran = new CountDownTimer(mTask.getDelay(TimeUnit.MILLISECONDS), 1000) {
            @Override
            public void onTick(long seconds) {

                long secondes = seconds;

                secondes = secondes / 1000;

                String textTemps = "zZz " + String.format(getString(R.string.timer_info), ((secondes % 3600) / 60), ((secondes % 3600) % 60));

                timeAfficheur.setText(textTemps);
            }

            @Override
            public void onFinish() {
                timeAfficheur.setVisibility(View.INVISIBLE);
            }

        }.start();
    }


   /* ****************
    * Annuler le timer
    * ****************/

    private void stopTimer() {
        if (running) {
            mTask.cancel(true);
            timerEcran.cancel();

            volume.getMinuteur().cancel();
            volume.setVolume(context, 1.0f);
        }

        running = false;

        Notification.setState(false);
        State.getState(context);

        timeAfficheur = ((TextView) findViewById(R.id.time_ecran));
        assert timeAfficheur != null;
        timeAfficheur.setVisibility(View.INVISIBLE);
    }



    /* *********************************************************************************************
     *
     * Sauvegarde/restauration/importation des radios
     *
     * ********************************************************************************************/

    private static final int FILE_PICKER_RESULT = 0;

    private void exporter() {

        if (Build.VERSION.SDK_INT >= 23
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            checkWritePermission();
            imp_exp = "exporter";
        } else {

            RadiosDatabase radiosDatabase = new RadiosDatabase(context);

            String Destination = Environment.getExternalStorageDirectory().toString() + "/Radio";

            File newRep = new File(Destination);
            if (!newRep.exists()) {
                //noinspection ResultOfMethodCallIgnored
                newRep.mkdir();
            }

            String path = Destination + "/" + RadiosDatabase.DB_NAME + ".xml";

            DatabaseSave databaseSave = new DatabaseSave(radiosDatabase.getReadableDatabase(), path);
            databaseSave.exportData();

            Toast.makeText(context, getString(R.string.exporter), Toast.LENGTH_SHORT).show();


        }
    }

    private void importer() {

        if (Build.VERSION.SDK_INT >= 23
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            checkWritePermission();
            imp_exp = "importer";
        } else {
            importType = "fichier";
            String currentPath = Environment.getExternalStorageDirectory().toString() + "/Radio";

            Intent intent = new Intent(getApplicationContext(), FilePickerActivity.class);
            intent.putExtra(FilePicker.SET_ONLY_ONE_ITEM, true);
            intent.putExtra(FilePicker.DISABLE_NEW_FOLDER_BUTTON, true);
            intent.putExtra(FilePicker.DISABLE_SORT_BUTTON, true);
            intent.putExtra(FilePicker.ENABLE_QUIT_BUTTON, true);
            intent.putExtra(FilePicker.SET_CHOICE_TYPE, FilePicker.CHOICE_TYPE_FILES);
            intent.putExtra(FilePicker.SET_START_DIRECTORY, currentPath);
            startActivityForResult(intent, FILE_PICKER_RESULT);
        }
    }

    /* **********************************************************************************************
    * Changer le logo de la radio
    * *********************************************************************************************/

    private void addImg() {

        if (Build.VERSION.SDK_INT >= 23
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            checkWritePermission();
            imp_exp = "image";
        } else {
            importType = "image";
            String currentPath = Environment.getExternalStorageDirectory().toString() + "/";

            Intent intent = new Intent(getApplicationContext(), FilePickerActivity.class);
            intent.putExtra(FilePicker.SET_ONLY_ONE_ITEM, true);
            intent.putExtra(FilePicker.SET_FILTER_LISTED, new String[] { "bmp", "gif", "jpeg", "jpg", "png" });
            intent.putExtra(FilePicker.DISABLE_NEW_FOLDER_BUTTON, true);
            intent.putExtra(FilePicker.DISABLE_SORT_BUTTON, true);
            intent.putExtra(FilePicker.ENABLE_QUIT_BUTTON, true);
            intent.putExtra(FilePicker.SET_CHOICE_TYPE, FilePicker.CHOICE_TYPE_FILES);
            intent.putExtra(FilePicker.SET_START_DIRECTORY, currentPath);
            startActivityForResult(intent, FILE_PICKER_RESULT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == FILE_PICKER_RESULT && data != null) {
            FilePickerParcelObject object = data.getParcelableExtra(FilePickerParcelObject.class.getCanonicalName());
            StringBuilder buffer = new StringBuilder();

            if (object.count > 0) {
                for (int i = 0; i < object.count; i++) {
                    buffer.append(object.names.get(i));
                    if (i < object.count - 1) buffer.append(", ");
                }
            }

            if ( importType.equals("fichier") ) {

                ReadXML readXML = new ReadXML();

                String XMLdata = readXML.readFile(object.path + buffer.toString());

                readXML.read(context, XMLdata);


                updateListView();


            } else if (importType.equals("image")) {
                String pathImg = object.path + buffer.toString();
                File imgFile = new  File(pathImg);
                if(imgFile.exists()){
                    try {
                        logoRadio = ImageFactory.getResizedBitmap(context, BitmapFactory.decodeFile(imgFile.getAbsolutePath()));

                        final ImageView logo = (ImageView) editView.findViewById(R.id.logo);
                        final TextView text = (TextView) editView.findViewById(R.id.texte);

                        logo.setImageBitmap(logoRadio);
                        logo.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
                        text.setVisibility(View.INVISIBLE);
                    } catch (NullPointerException ignored) {}
                }
            }
        }
    }





    private String readFile(String fichier) {

        String ret = "";

        try {

            FileInputStream inputStream = new FileInputStream(new File(fichier));

            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString = "";
            StringBuilder stringBuilder = new StringBuilder();

            while ( (receiveString = bufferedReader.readLine()) != null ) {
                stringBuilder.append(receiveString);
            }

            inputStream.close();
            ret = stringBuilder.toString();

        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }


    private void readXML(String XMLData) {

        List<XmlValuesModel> myData = null;

        try {

            /************** Read XML *************/

            BufferedReader br = new BufferedReader(new StringReader(XMLData));
            InputSource is = new InputSource(br);

            /************  Parse XML **************/

            XMLParser parser=new XMLParser();
            SAXParserFactory factory=SAXParserFactory.newInstance();
            SAXParser sp=factory.newSAXParser();
            XMLReader reader=sp.getXMLReader();
            reader.setContentHandler(parser);
            reader.parse(is);

            /************* Get Parse data in a ArrayList **********/
            myData = parser.list;

            if (myData != null) {

                // String OutputData = "";

                /*************** Get Data From ArrayList *********/

                for (XmlValuesModel xmlRowData : myData) {

                    if (xmlRowData != null) {

                        String  url   = xmlRowData.getUrl();
                        String  name   = xmlRowData.getName();
                        String image = xmlRowData.getImage();
                        byte[] img = null;


                        if (image != null)
                            img = Base64.decode(image, Base64.DEFAULT);

                        Radio newRadio = new Radio(url, name, img);
                        Radio.addRadio(context, newRadio);

                    }
                }

            }

            updateListView();

            Toast.makeText(context, getString(R.string.importer), Toast.LENGTH_SHORT).show();

        } catch(Exception e) {
            Log.e("Jobs", "Exception parse xml :" + e);
            Toast.makeText(context, getString(R.string.importer_erreur), Toast.LENGTH_SHORT).show();
        }
    }


    /* *********************************************************************************************
     * Arrêt de la radio
     * ********************************************************************************************/
    public static void stop(Context context) {
        running = false;

        Intent player = new Intent(context, PlayerService.class);
        player.putExtra("action", "stop");
        context.startService(player);

        Notification.setState(false);
        State.getState(context);
    }


    /***********************************************************************************************
     * About dialog
     **********************************************************************************************/

    private void about() {
        About dialog = new About();
        dialog.show(getSupportFragmentManager(), "about");
    }


    /***********************************************************************************************
     * Touche retour
     **********************************************************************************************/

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert drawer != null;
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {

            moveTaskToBack(true);

        }

        return super.onKeyDown(keyCode, event);
    }


    private void checkWritePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            Permissions perm = new Permissions();
            perm.check(context, MainActivity.this);

        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull  String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {

            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResult == PackageManager.PERMISSION_GRANTED) {

                    switch (imp_exp) {
                        case "importer":
                            importer();
                            break;
                        case "exporter":
                            exporter();
                            break;
                        case "image":
                            addImg();
                            break;
                    }

                }
            }
        }
    }

}
