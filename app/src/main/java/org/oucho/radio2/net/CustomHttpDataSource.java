package org.oucho.radio2.net;


import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;

public class CustomHttpDataSource implements DataSource.Factory {

    // static for GarbageCollector
    private static String userAgent;
    private static TransferListener<? super DataSource> listener;
    private static int connectTimeoutMillis;
    private static int readTimeoutMillis;
    private static boolean allowCrossProtocolRedirects;

    private static void setUserAgent(String value) {
        userAgent = value;
    }

    private void setListener(TransferListener<? super DataSource> value) {
        listener = value;
    }

    private void setConnectTimeoutMillis() {
        connectTimeoutMillis = DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS;
    }

    private void setReadTimeoutMillis() {
        readTimeoutMillis = DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS;
    }

    private void setAllowCrossProtocolRedirects() {
        allowCrossProtocolRedirects = false;
    }

    public CustomHttpDataSource(String userAgent, TransferListener<? super DataSource> listener) {

        setUserAgent(userAgent);
        setListener(listener);
        setConnectTimeoutMillis();
        setReadTimeoutMillis();
        setAllowCrossProtocolRedirects();
    }

    @Override
    public DefaultHttpDataSource createDataSource() {

        return new DefaultHttpDataSource(userAgent, null, listener, connectTimeoutMillis, readTimeoutMillis, allowCrossProtocolRedirects, null);
    }


}