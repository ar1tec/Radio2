package org.oucho.radio2.filepicker;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

public class ImageCache {

    private static ImageCache instance;

    private final LruCache<String, Bitmap> mMemoryCache;


    private ImageCache() {

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        final int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {

            @Override
            protected int sizeOf(String key, Bitmap bitmap) {


                return bitmap.getRowBytes() * bitmap.getHeight();
                //return bitmap.getByteCount() / 1024;
            }
        };
    }

    public static ImageCache getInstance() {

        if (instance == null) {

            instance = new ImageCache();
        }

        return instance;
    }

    public void put(String key, Bitmap bitmap) {
        if (get(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap get(String key) {

        return mMemoryCache.get(key);
    }

    public void remove(String key) {
        mMemoryCache.remove(key);
    }

    public synchronized void clear() {
        mMemoryCache.evictAll();
    }

}