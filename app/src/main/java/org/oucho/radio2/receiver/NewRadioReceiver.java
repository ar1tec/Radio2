package org.oucho.radio2.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.oucho.radio2.db.Radio;
import org.oucho.radio2.utils.ImageFactory;


public class NewRadioReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String etat = intent.getAction();

        Log.w("RadioReceiver", etat);

        if ( "org.oucho.radio2.ADD_RADIO".equals(etat) ) {

            String name = intent.getStringExtra("name");
            String url = intent.getStringExtra("url");

            String image = intent.getStringExtra("image");

            byte[] img = null;
            if (image != null)
                img = ImageFactory.stringToByte(image);

            Radio newRadio = new Radio(url, name, img);
            Radio.addNewRadio(context, newRadio);
        }
    }
}
