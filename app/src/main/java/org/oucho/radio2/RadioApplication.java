package org.oucho.radio2;

import android.app.Application;

public class RadioApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

/*        Intent intent = getIntent();
        String action = intent != null ? intent.getAction(): null;

        if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {

            intent.setDataAndType(intent.getData(), intent.getType());
            Log.w("PWET", intent.getData() + " " + intent.getType());


            Radio newRadio = new Radio(intent.getData().toString(), intent.getData().toString() , null);
            Radio.addRadio(getApplicationContext(), newRadio);

        }*/

/*        startActivity(intent.setClass(this, MainActivity.class));

        Intent intent1 = new Intent();
        intent1.putExtra("android.intent.action.MAIN", true);
        startActivity(intent1);*/

        // MediaUtils.openMediaNoUi(intent.getData());
        //finish();
    }
}
