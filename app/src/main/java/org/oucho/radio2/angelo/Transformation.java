package org.oucho.radio2.angelo;

import android.graphics.Bitmap;

interface Transformation {

    Bitmap transform(Bitmap source);

    String key();
}
