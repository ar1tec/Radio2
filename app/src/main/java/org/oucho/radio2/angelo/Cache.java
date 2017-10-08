package org.oucho.radio2.angelo;

import android.graphics.Bitmap;


interface Cache {

    Bitmap get(String key);

    void set(String key, Bitmap bitmap);

}
