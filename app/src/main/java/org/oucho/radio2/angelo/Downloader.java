package org.oucho.radio2.angelo;

import android.support.annotation.NonNull;
import java.io.IOException;
import okhttp3.Response;

interface Downloader {

    @NonNull Response load(@NonNull okhttp3.Request request) throws IOException;


    void shutdown();
}
