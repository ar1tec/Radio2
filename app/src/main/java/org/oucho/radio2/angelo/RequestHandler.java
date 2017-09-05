package org.oucho.radio2.angelo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.io.IOException;
import okio.Source;

import static org.oucho.radio2.angelo.Utils.checkNotNull;


public abstract class RequestHandler {

    public static final class Result {
        private final Angelo.LoadedFrom loadedFrom;
        private final Bitmap bitmap;
        private final Source source;

        Result(@NonNull Bitmap bitmap) {
            this(checkNotNull(bitmap, "bitmap == null"), null, Angelo.LoadedFrom.DISK);
        }

        Result(@NonNull Source source, @NonNull Angelo.LoadedFrom loadedFrom) {
            this(null, checkNotNull(source, "source == null"), loadedFrom);
        }

        Result(@Nullable Bitmap bitmap, @Nullable Source source, @NonNull Angelo.LoadedFrom loadedFrom) {
            if ((bitmap != null) == (source != null)) {
                throw new AssertionError();
            }
            this.bitmap = bitmap;
            this.source = source;
            this.loadedFrom = checkNotNull(loadedFrom, "loadedFrom == null");
        }

        /** The loaded {@link Bitmap}. Mutually exclusive with {@link #getSource()}. */
        @Nullable
        public Bitmap getBitmap() {
            return bitmap;
        }

        /** A stream of image data. Mutually exclusive with {@link #getBitmap()}. */
        @Nullable
        public Source getSource() {
            return source;
        }

        /**
         * Returns the resulting {@link Angelo.LoadedFrom} generated from a
         * {@link #load(Request, int)} call.
         */
        @NonNull
        Angelo.LoadedFrom getLoadedFrom() {
            return loadedFrom;
        }

    }

    /**
     * Whether or not this {@link RequestHandler} can handle a request with the given {@link Request}.
     */
    public abstract boolean canHandleRequest(Request data);

    /**
     * Loads an image for the given {@link Request}.
     *
     * @param request the data from which the image should be resolved.
     * @param networkPolicy the {@link NetworkPolicy} for this request.
     */
    @Nullable
    public abstract Result load(Request request, int networkPolicy) throws IOException;

    int getRetryCount() {
        return 0;
    }

    boolean shouldRetry(NetworkInfo info) {
        return false;
    }

    boolean supportsReplay() {
        return false;
    }

    /**
     * Lazily create {@link BitmapFactory.Options} based in given
     * {@link Request}, only instantiating them if needed.
     */
    static BitmapFactory.Options createBitmapOptions(Request data) {
        final boolean justBounds = data.hasSize();
        final boolean hasConfig = data.config != null;
        BitmapFactory.Options options = null;
        if (justBounds || hasConfig || data.purgeable) {
            options = new BitmapFactory.Options();
            options.inJustDecodeBounds = justBounds;
            //options.inInputShareable = data.purgeable;
            //options.inPurgeable = data.purgeable;
            if (hasConfig) {
                options.inPreferredConfig = data.config;
            }
        }
        return options;
    }

    static boolean requiresInSampleSize(BitmapFactory.Options options) {
        return options != null && options.inJustDecodeBounds;
    }

    static void calculateInSampleSize(int reqWidth, int reqHeight, BitmapFactory.Options options, Request request) {
        calculateInSampleSize(reqWidth, reqHeight, options.outWidth, options.outHeight, options,
                request);
    }

    static void calculateInSampleSize(int reqWidth, int reqHeight, int width, int height, BitmapFactory.Options options, Request request) {
        int sampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int heightRatio;
            final int widthRatio;
            if (reqHeight == 0) {
                sampleSize = (int) Math.floor((float) width / (float) reqWidth);
            } else if (reqWidth == 0) {
                sampleSize = (int) Math.floor((float) height / (float) reqHeight);
            } else {
                heightRatio = (int) Math.floor((float) height / (float) reqHeight);
                widthRatio = (int) Math.floor((float) width / (float) reqWidth);
                sampleSize = request.centerInside ? Math.max(heightRatio, widthRatio) : Math.min(heightRatio, widthRatio);
            }
        }
        options.inSampleSize = sampleSize;
        options.inJustDecodeBounds = false;
    }
}
