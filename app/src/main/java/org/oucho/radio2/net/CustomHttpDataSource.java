package org.oucho.radio2.net;


import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;

public class CustomHttpDataSource implements DataSource.Factory {

    // static pour GC
    private static String userAgent;
    private static TransferListener<? super DataSource> listener;
    private static int connectTimeoutMillis;
    private static int readTimeoutMillis;
    private static boolean allowCrossProtocolRedirects;



    public CustomHttpDataSource(String userAgent, TransferListener<? super DataSource> listener) {
        this(userAgent,
             listener,
             DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
             DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
             false);
    }


    private CustomHttpDataSource(String userAgent, TransferListener<? super DataSource> listener,
                                 int connectTimeoutMillis,
                                 int readTimeoutMillis,
                                 boolean allowCrossProtocolRedirects) {
        this.userAgent = userAgent;
        this.listener = listener;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.allowCrossProtocolRedirects = allowCrossProtocolRedirects;
    }


    @Override
    public DefaultHttpDataSource createDataSource() {

            return new DefaultHttpDataSource(userAgent, null, listener, connectTimeoutMillis,
                    readTimeoutMillis, allowCrossProtocolRedirects, null);

    }
}