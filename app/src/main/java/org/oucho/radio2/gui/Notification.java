package org.oucho.radio2.gui;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.NotificationCompat;
import android.util.Base64;
import android.widget.RemoteViews;

import org.oucho.radio2.MainActivity;
import org.oucho.radio2.R;
import org.oucho.radio2.interfaces.RadioKeys;

import static android.content.Context.MODE_PRIVATE;


public class Notification implements RadioKeys{

    private static final int NOTIFY_ID = 32;

    private static boolean timer = false;

    public static void setState(boolean onOff){
        timer = onOff;
    }

    public static void updateNotification(Context context, String nom_radio, String action, Bitmap logo) {

        SharedPreferences préférences = context.getSharedPreferences(PREF_FILE, MODE_PRIVATE);

        String img = préférences.getString("image_data", "");

        if( logo == null && !img.equalsIgnoreCase("") ){
            byte[] b = Base64.decode(img, Base64.DEFAULT);

            logo = BitmapFactory.decodeByteArray(b, 0, b.length);

        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        Intent i = new Intent(context, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(intent);

        if (!timer) {
            builder.setSmallIcon(R.drawable.notification);
        } else {
            builder.setSmallIcon(R.drawable.notification_sleeptimer);
        }

        builder.setOngoing(true);

        Boolean unlock;
        unlock = "Lecture".equals(action);
        builder.setOngoing(unlock);

        android.app.Notification notification = builder.build();
        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification);

        contentView.setTextViewText(R.id.notif_name, nom_radio);
        contentView.setTextViewText(R.id.notif_text, action);

        if (logo != null)
            contentView.setImageViewBitmap(R.id.notif_ombre, logo);

/*        } else {
            contentView.setImageViewBitmap(R.id.notif_ombre,
                    BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.ic_radio_white_36dp));
        }*/



        notification.contentView = contentView;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFY_ID, notification);

    }


    public static void removeNotification(Context context) {

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFY_ID);

    }


}
