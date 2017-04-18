package org.oucho.radio2.audio;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;

import org.oucho.radio2.PlayerService;
import org.oucho.radio2.interfaces.RadioKeys;
import org.oucho.radio2.utils.State;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class VolumeTimer implements RadioKeys {

    private CountDownTimer minuteurVolume;

    public void setVolume(Context contex, float volume) {

        if (State.isPlaying() || State.isPaused()) {
            Intent niveau = new Intent(contex, PlayerService.class);
            niveau.putExtra("voldown", volume);
            contex.startService(niveau);
        }
    }

    public void baisser(final Context context, final ScheduledFuture task, final int delay) {

            // définir si le delay est supérieur ou inférieur à 10mn

            final short minutes = (short) ( ( (delay / 1000) % 3600) / 60);

            final boolean tempsMinuterie = minutes > 10;

            int cycle;

            if (tempsMinuterie) {
                cycle = 60000;
            } else {
                cycle = 1000;
            }


        minuteurVolume = new CountDownTimer(delay, cycle) {
                @Override
                public void onTick(long mseconds) {

                    long temps1 = ((task.getDelay(TimeUnit.MILLISECONDS) / 1000) % 3600) / 60 ;

                    long temps2 = task.getDelay(TimeUnit.MILLISECONDS) / 1000;

                    if (tempsMinuterie) {

                        if (temps1 < 1) {
                            setVolume(context, 0.1f);
                        } else if (temps1 < 2) {
                            setVolume(context, 0.2f);
                        } else if (temps1 < 3) {
                            setVolume(context, 0.3f);
                        } else if (temps1 < 4) {
                            setVolume(context, 0.4f);
                        } else if (temps1 < 5) {
                            setVolume(context, 0.5f);
                        } else if (temps1 < 6) {
                            setVolume(context, 0.6f);
                        } else if (temps1 < 7) {
                            setVolume(context, 0.7f);
                        } else if (temps1 < 8) {
                            setVolume(context, 0.8f);
                        } else if (temps1 < 9) {
                            setVolume(context, 0.9f);
                        } else if (temps1 < 10) {
                            setVolume(context, 1.0f);
                        }

                    } else {

                        if (temps2 < 6) {
                            setVolume(context, 0.1f);
                        } else if (temps2 < 12) {
                            setVolume(context, 0.2f);
                        } else if (temps2 < 18) {
                            setVolume(context, 0.3f);
                        } else if (temps2 < 24) {
                            setVolume(context, 0.4f);
                        } else if (temps2 < 30) {
                            setVolume(context, 0.5f);
                        } else if (temps2 < 36) {
                            setVolume(context, 0.6f);
                        } else if (temps2 < 42) {
                            setVolume(context, 0.7f);
                        } else if (temps2 < 48) {
                            setVolume(context, 0.8f);
                        } else if (temps2 < 54) {
                            setVolume(context, 0.9f);
                        } else if (temps2 < 60) {
                            setVolume(context, 1.0f);
                        }
                    }
                }

                @Override
                public void onFinish() {

                    Intent intent = new Intent();
                    intent.setAction(INTENT_STATE);
                    intent.putExtra(ACTION_QUIT, true);
                    context.sendBroadcast(intent);
                }
            }.start();

    }

    public CountDownTimer getMinuteur() {
        return minuteurVolume;
    }
}
