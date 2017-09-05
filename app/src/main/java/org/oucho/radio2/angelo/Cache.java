package org.oucho.radio2.angelo;

import android.graphics.Bitmap;


interface Cache {
    /** Retrieve an image for the specified {@code key} or {@code null}. */
    Bitmap get(String key);

    /** Store an image in the cache for the specified {@code key}. */
    void set(String key, Bitmap bitmap);

}
