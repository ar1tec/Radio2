package org.oucho.radio2.angelo;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class LruCache implements Cache {
    private final LinkedHashMap<String, Bitmap> map;
    private final int maxSize;

    private int size;


    /** Create a cache using an appropriate portion of the available RAM as the maximum size. */
    public LruCache(@NonNull Context context) {
        this(Utils.calculateMemoryCacheSize(context));
    }

    /** Create a cache with a given maximum size in bytes. */
    private LruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Max size must be positive.");
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<>(0, 0.75f, true);
    }

    @Override public Bitmap get(String key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        Bitmap mapValue;
        synchronized (this) {
            mapValue = map.get(key);
            if (mapValue != null) {
                return mapValue;
            }
        }

        return null;
    }

    @Override public void set(String key, Bitmap bitmap) {
        if (key == null || bitmap == null) {
            throw new NullPointerException("key == null || bitmap == null");
        }

        int addedSize = Utils.getBitmapBytes(bitmap);
        if (addedSize > maxSize) {
            return;
        }

        synchronized (this) {
            size += addedSize;
            Bitmap previous = map.put(key, bitmap);
            if (previous != null) {
                size -= Utils.getBitmapBytes(previous);
            }
        }

        trimToSize(maxSize);
    }

    private void trimToSize(int maxSize) {
        while (true) {
            String key;
            Bitmap value;
            synchronized (this) {
                if (size < 0 || (map.isEmpty() && size != 0)) {
                    throw new IllegalStateException(
                            getClass().getName() + ".sizeOf() is reporting inconsistent results!");
                }

                if (size <= maxSize || map.isEmpty()) {
                    break;
                }

                Map.Entry<String, Bitmap> toEvict = map.entrySet().iterator().next();
                key = toEvict.getKey();
                value = toEvict.getValue();
                map.remove(key);
                size -= Utils.getBitmapBytes(value);
            }
        }
    }

    @Override
    public final synchronized int size() {
        return size;
    }

}
