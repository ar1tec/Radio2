package org.oucho.radio2.sound;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;

import org.oucho.radio2.PlayerService;
import org.oucho.radio2.utils.State;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class VolumeTimer {

    private CountDownTimer minuteurVolume;

    public void setVolume(Context contex, String volume) {

        if (State.isPlaying() || State.isPaused()) {
            Intent niveau = new Intent(contex, PlayerService.class);
            niveau.putExtra("action", volume);
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
                            setVolume(context, "vol1");
                        } else if (temps1 < 2) {
                            setVolume(context, "vol2");
                        } else if (temps1 < 3) {
                            setVolume(context, "vol3");
                        } else if (temps1 < 4) {
                            setVolume(context, "vol4");
                        } else if (temps1 < 5) {
                            setVolume(context, "vol5");
                        } else if (temps1 < 6) {
                            setVolume(context, "vol6");
                        } else if (temps1 < 7) {
                            setVolume(context, "vol7");
                        } else if (temps1 < 8) {
                            setVolume(context, "vol8");
                        } else if (temps1 < 9) {
                            setVolume(context, "vol9");
                        } else if (temps1 < 10) {
                            setVolume(context, "vol10");
                        }

                    } else {

                        if (temps2 < 6) {
                            setVolume(context, "vol1");
                        } else if (temps2 < 12) {
                            setVolume(context, "vol2");
                        } else if (temps2 < 18) {
                            setVolume(context, "vol3");
                        } else if (temps2 < 24) {
                            setVolume(context, "vol4");
                        } else if (temps2 < 30) {
                            setVolume(context, "vol5");
                        } else if (temps2 < 36) {
                            setVolume(context, "vol6");
                        } else if (temps2 < 42) {
                            setVolume(context, "vol7");
                        } else if (temps2 < 48) {
                            setVolume(context, "vol8");
                        } else if (temps2 < 54) {
                            setVolume(context, "vol9");
                        } else if (temps2 < 60) {
                            setVolume(context, "vol10");
                        }
                    }
                }

                @Override
                public void onFinish() {

                    //exit();

                }

            }.start();

    }

    public CountDownTimer getMinuteur() {
        return minuteurVolume;
    }
}
