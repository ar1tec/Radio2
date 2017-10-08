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


public final class OkHttp3Downloader implements Downloader {

    @VisibleForTesting
    private final Call.Factory client;

    OkHttp3Downloader(final Context context) {
        this(Utils.createDefaultCacheDir(context));
    }

    private OkHttp3Downloader(final File cacheDir) {
        this(cacheDir, Utils.calculateDiskCacheSize(cacheDir));
    }

    private OkHttp3Downloader(final File cacheDir, final long maxSize) {
        this(new OkHttpClient.Builder().cache(new Cache(cacheDir, maxSize)).build());
    }

    private OkHttp3Downloader(OkHttpClient client) {
        this.client = client;
    }

    @NonNull
    @Override
    public Response load(@NonNull Request request) throws IOException {
        return client.newCall(request).execute();
    }

}
