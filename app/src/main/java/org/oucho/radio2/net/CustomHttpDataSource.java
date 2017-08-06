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

    private void setUserAgent(String value) {
        userAgent = value;
    }

    private void setListener(TransferListener<? super DataSource> value) {
        listener = value;
    }

    private void setConnectTimeoutMillis(int value) {
        connectTimeoutMillis = value;
    }

    private void setReadTimeoutMillis(int value) {
        readTimeoutMillis = value;
    }

    private void setAllowCrossProtocolRedirects(boolean value) {
        allowCrossProtocolRedirects = value;
    }
    public CustomHttpDataSource(String userAgent, TransferListener<? super DataSource> listener) {

        setUserAgent(userAgent);
        setListener(listener);
        setConnectTimeoutMillis(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS);
        setReadTimeoutMillis(DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS);
        setAllowCrossProtocolRedirects(false);

        //this.userAgent = userAgent;
       // this.listener = listener;
       // this.connectTimeoutMillis = DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS;
       // this.readTimeoutMillis = DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS;
       // this.allowCrossProtocolRedirects = false;
    }


    @Override
    public DefaultHttpDataSource createDataSource() {

        return new DefaultHttpDataSource(userAgent, null, listener, connectTimeoutMillis, readTimeoutMillis, allowCrossProtocolRedirects, null);
    }
}