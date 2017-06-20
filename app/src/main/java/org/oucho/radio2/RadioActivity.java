package org.oucho.radio2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.util.Log;

import org.oucho.radio2.db.Radio;


public class RadioActivity extends Activity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent != null ? intent.getAction(): null;

        if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {

            intent.setDataAndType(intent.getData(), intent.getType());
            Log.w("PWET", intent.getData() + " " + intent.getType());


            Radio newRadio = new Radio(intent.getData().toString(), intent.getData().toString() , null);
            Radio.addRadio(getApplicationContext(), newRadio);

        }

        startActivity(intent.setClass(this, MainActivity.class));
/*        startActivity(intent.setClass(this, MainActivity.class));

        Intent intent1 = new Intent();
        intent1.putExtra("android.intent.action.MAIN", true);
        startActivity(intent1);*/

        // MediaUtils.openMediaNoUi(intent.getData());
        finish();
        return;
    }
}
