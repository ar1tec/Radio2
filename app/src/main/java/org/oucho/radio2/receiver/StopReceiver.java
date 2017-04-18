package org.oucho.radio2.receiver;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;

import org.oucho.radio2.PlayerService;
import org.oucho.radio2.interfaces.RadioKeys;
import org.oucho.radio2.gui.Notification;
import org.oucho.radio2.utils.State;

public class StopReceiver extends BroadcastReceiver implements RadioKeys {

    @Override
    public void onReceive(Context context, Intent intent) {


        String etat = intent.getAction();

        if (INTENT_STATE.equals(etat)) {

            String nom_radio = intent.getStringExtra("name");


            if (nom_radio == null) {

                SharedPreferences préférences = context.getSharedPreferences(PREF_FILE, 0);

                nom_radio = préférences.getString("name", "");
            }

            String action_lecteur = intent.getStringExtra("state");

            Notification.updateNotification(context, nom_radio, action_lecteur, null);
        }


        if ( "org.oucho.radio2.STOP".equals(etat) && ( State.isPlaying() || State.isPaused() ) ) {

            String halt = intent.getStringExtra("halt");
            Intent player = new Intent(context, PlayerService.class);
            player.putExtra("action", halt);
            context.startService(player);

            final Context ctx = context;
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {

                @SuppressLint("SetTextI18n")
                public void run() {

                    if (!State.isPlaying())
                        Notification.removeNotification(ctx);

                }
            }, 500);
        }
    }
}
