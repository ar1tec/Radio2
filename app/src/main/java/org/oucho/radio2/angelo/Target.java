package org.oucho.radio2.angelo;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import static org.oucho.radio2.angelo.Angelo.LoadedFrom;

/**
 * Represents an arbitrary listener for image loading.
 * <p>
 * Objects implementing this class <strong>must</strong> have a working implementation of
 * {@link Object#equals(Object)} and {@link Object#hashCode()} for proper storage internally.
 * Instances of this interface will also be compared to determine if view recycling is occurring.
 * It is recommended that you add this interface directly on to a custom view type when using in an
 * adapter to ensure correct recycling behavior.
 */
interface Target {
    /**
     * Callback when an image has been successfully loaded.
     * <p>
     * <strong>Note:</strong> You must not recycle the bitmap.
     */
    void onBitmapLoaded(Bitmap bitmap, LoadedFrom from);

    /**
     * Callback indicating the image could not be successfully loaded.
     * <p>
     * <strong>Note:</strong> The passed {@link Drawable} may be {@code null} if none has been
     * specified via {@link RequestCreator#error(android.graphics.drawable.Drawable)}
     * or {@link RequestCreator#error(int)}.
     */
    void onBitmapFailed(Exception e, Drawable errorDrawable);

    /**
     * Callback invoked right before your request is submitted.
     * <p>
     * <strong>Note:</strong> The passed {@link Drawable} may be {@code null} if none has been
     * specified via {@link RequestCreator#placeholder(android.graphics.drawable.Drawable)}
     * or {@link RequestCreator#placeholder(int)}.
     */
    void onPrepareLoad(Drawable placeHolderDrawable);
}
