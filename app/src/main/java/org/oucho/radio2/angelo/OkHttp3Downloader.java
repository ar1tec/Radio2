package org.oucho.radio2.angelo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** A {@link Downloader} which uses OkHttp to download images. */
public final class OkHttp3Downloader implements Downloader {
    @VisibleForTesting
    private final Call.Factory client;
    private final Cache cache;
    private boolean sharedClient = true;

    OkHttp3Downloader(final Context context) {
        this(Utils.createDefaultCacheDir(context));
    }


    private OkHttp3Downloader(final File cacheDir) {
        this(cacheDir, Utils.calculateDiskCacheSize(cacheDir));
    }


    private OkHttp3Downloader(final File cacheDir, final long maxSize) {
        this(new OkHttpClient.Builder().cache(new Cache(cacheDir, maxSize)).build());
        sharedClient = false;
    }


    private OkHttp3Downloader(OkHttpClient client) {
        this.client = client;
        this.cache = client.cache();
    }

    @NonNull @Override public Response load(@NonNull Request request) throws IOException {
        return client.newCall(request).execute();
    }

    @Override public void shutdown() {
        if (!sharedClient && cache != null) {
            try {
                cache.close();
            } catch (IOException ignored) {
            }
        }
    }
}
