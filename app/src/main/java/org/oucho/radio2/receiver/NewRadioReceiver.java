package org.oucho.radio2.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.oucho.radio2.db.Radio;


public class NewRadioReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String etat = intent.getAction();

        Log.w("RadioReceiver", etat);

        if ( "org.oucho.radio2.ADD_RADIO".equals(etat) ) {

            String name = intent.getStringExtra("name");
            String url = intent.getStringExtra("url");

            Radio newRadio = new Radio(url, name, null);
            Radio.addNewRadio(context, newRadio);
        }
    }
}
