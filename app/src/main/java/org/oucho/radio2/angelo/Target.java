package org.oucho.radio2.angelo;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import static org.oucho.radio2.angelo.Angelo.LoadedFrom;


interface Target {

    void onBitmapLoaded(Bitmap bitmap, LoadedFrom from);

    void onBitmapFailed(Exception e, Drawable errorDrawable);

    void onPrepareLoad(Drawable placeHolderDrawable);
}
