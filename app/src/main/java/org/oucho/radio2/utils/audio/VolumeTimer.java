package org.oucho.radio2.utils.audio;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;

import org.oucho.radio2.radio.PlayerService;
import org.oucho.radio2.radio.RadioKeys;
import org.oucho.radio2.utils.State;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class VolumeTimer implements RadioKeys {

    private CountDownTimer volumeTimer;

    public void setVolume(Context contex, float volume) {

        if (State.isPlaying() || State.isPaused()) {
            Intent niveau = new Intent(contex, PlayerService.class);
            niveau.putExtra("voldown", volume);
            contex.startService(niveau);
        }
    }

    public void volumeDown(final Context context, final ScheduledFuture task, final int delay) {

        final short minutes = (short) ( ( (delay / 1000) % 3600) / 60);

        // delay is greater or less than 10mn
        final boolean tempsMinuterie = minutes > 10;

            int cycle;

            if (tempsMinuterie) {
                cycle = 60000;
            } else {
                cycle = 1000;
            }


        volumeTimer = new CountDownTimer(delay, cycle) {
                @Override
                public void onTick(long mseconds) {

                    // for long timer > 10mn
                    long minutesTimer = ((task.getDelay(TimeUnit.MILLISECONDS) / 1000) % 3600) / 60 ;

                    // for short timer < 10mn
                    long secondesTimer = task.getDelay(TimeUnit.MILLISECONDS) / 1000;

                    if (tempsMinuterie) {

                        if (minutesTimer < 1) {
                            setVolume(context, 0.1f);
                        } else if (minutesTimer < 2) {
                            setVolume(context, 0.2f);
                        } else if (minutesTimer < 3) {
                            setVolume(context, 0.3f);
                        } else if (minutesTimer < 4) {
                            setVolume(context, 0.4f);
                        } else if (minutesTimer < 5) {
                            setVolume(context, 0.5f);
                        } else if (minutesTimer < 6) {
                            setVolume(context, 0.6f);
                        } else if (minutesTimer < 7) {
                            setVolume(context, 0.7f);
                        } else if (minutesTimer < 8) {
                            setVolume(context, 0.8f);
                        } else if (minutesTimer < 9) {
                            setVolume(context, 0.9f);
                        } else if (minutesTimer < 10) {
                            setVolume(context, 1.0f);
                        }

                    } else {

                        if (secondesTimer < 6) {
                            setVolume(context, 0.1f);
                        } else if (secondesTimer < 12) {
                            setVolume(context, 0.2f);
                        } else if (secondesTimer < 18) {
                            setVolume(context, 0.3f);
                        } else if (secondesTimer < 24) {
                            setVolume(context, 0.4f);
                        } else if (secondesTimer < 30) {
                            setVolume(context, 0.5f);
                        } else if (secondesTimer < 36) {
                            setVolume(context, 0.6f);
                        } else if (secondesTimer < 42) {
                            setVolume(context, 0.7f);
                        } else if (secondesTimer < 48) {
                            setVolume(context, 0.8f);
                        } else if (secondesTimer < 54) {
                            setVolume(context, 0.9f);
                        } else if (secondesTimer < 60) {
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

    public CountDownTimer getVolumeTimer() {
        return volumeTimer;
    }
}
