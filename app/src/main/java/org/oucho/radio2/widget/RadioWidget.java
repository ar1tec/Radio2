package org.oucho.radio2.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

import org.oucho.radio2.MainActivity;
import org.oucho.radio2.R;
import org.oucho.radio2.radio.RadioApplication;
import org.oucho.radio2.radio.RadioService;
import org.oucho.radio2.utils.ImageFactory;

import static android.content.Context.MODE_PRIVATE;
import static org.oucho.radio2.radio.RadioKeys.INTENT_CONTROL_PAUSE;
import static org.oucho.radio2.radio.RadioKeys.INTENT_CONTROL_RESTART;
import static org.oucho.radio2.radio.RadioKeys.INTENT_CONTROL_STOP;
import static org.oucho.radio2.radio.RadioKeys.PREF_FILE;
import static org.oucho.radio2.utils.State.isPlaying;


public class RadioWidget extends AppWidgetProvider {

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.radio_widget);

        SharedPreferences preferences = RadioApplication.getInstance().getSharedPreferences(PREF_FILE, MODE_PRIVATE);

        Intent player = new Intent(context, WidgetService.class);
        context.startService(player);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

        Intent playpause = new Intent();
        if (isPlaying()) {
            playpause.setAction(INTENT_CONTROL_PAUSE);
        } else {
            playpause.setAction(INTENT_CONTROL_RESTART);
        }
        PendingIntent togglePlayIntent = PendingIntent.getBroadcast(context, 0, playpause, 0);


        Intent stop = new Intent();
        stop.setAction(INTENT_CONTROL_STOP);
        PendingIntent stopIntent = PendingIntent.getBroadcast(context, 0, stop, 0);

        if (isPlaying()) {
            views.setImageViewResource(R.id.playpause, R.drawable.ic_pause_circle_filled_grey_800_36dp);
        } else {
            views.setImageViewResource(R.id.playpause, R.drawable.ic_play_circle_filled_grey_800_36dp);
        }

        views.setOnClickPendingIntent(R.id.stop, stopIntent);
        views.setOnClickPendingIntent(R.id.playpause, togglePlayIntent);

        String name = RadioService.getName();
        String state = RadioService.getState();
        String img;
        Bitmap logo = RadioService.getLogo();

        if (name == null) {
            name = preferences.getString("name", "Radio");
        }

        if (state == null) {
            state = "Stop";
        }

        if (logo == null) {
            img = preferences.getString("image_data", null);

            if (img != null) {
                logo = ImageFactory.stringToBitmap(img);
            }
        }

        views.setImageViewBitmap(R.id.logo, logo);

        views.setTextViewText(R.id.radio, name);
        views.setTextViewText(R.id.state, state);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {}

    @Override
    public void onDisabled(Context context) {}
}

