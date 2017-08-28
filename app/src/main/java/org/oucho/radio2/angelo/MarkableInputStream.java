package org.oucho.radio2.angelo;

import android.support.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;


final class MarkableInputStream extends InputStream {
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    private static final int DEFAULT_LIMIT_INCREMENT = 1024;
    private final InputStream in;

    private long offset;
    private long reset;
    private long limit;
    private long defaultMark = -1;
    private boolean allowExpire = true;
    private int limitIncrement = -1;

    MarkableInputStream(InputStream in) {
        this(in, DEFAULT_BUFFER_SIZE);
    }

    private MarkableInputStream(InputStream in, int size) {
        this(in, size, DEFAULT_LIMIT_INCREMENT);
    }

    private MarkableInputStream(InputStream in, int size, int limitIncrement) {
        if (!in.markSupported()) {
            in = new BufferedInputStream(in, size);
        }
        this.in = in;
        this.limitIncrement = limitIncrement;
    }

    /** Marks this place in the stream so we can reset back to it later. */
    @Override
    public void mark(int readLimit) {
        defaultMark = savePosition(readLimit);
    }


    long savePosition(int readLimit) {
        long offsetLimit = offset + readLimit;
        if (limit < offsetLimit) {
            setLimit(offsetLimit);
        }
        return offset;
    }

    void allowMarksToExpire(boolean allowExpire) {
        this.allowExpire = allowExpire;
    }

    private void setLimit(long limit) {
        try {
            if (reset < offset && offset <= this.limit) {
                in.reset();
                in.mark((int) (limit - reset));
                skip(reset, offset);
            } else {
                reset = offset;
                in.mark((int) (limit - offset));
            }
            this.limit = limit;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to mark: " + e);
        }
    }

    /** Resets the stream to the most recent {@link #mark mark}. */
    @Override
    public void reset() throws IOException {
        reset(defaultMark);
    }

    /** Resets the stream to the position recorded by {@code token}. */
    void reset(long token) throws IOException {
        if (offset > limit || token < reset) {
            throw new IOException("Cannot reset");
        }
        in.reset();
        skip(reset, token);
        offset = token;
    }

    /** Skips {@code target - current} bytes and returns. */
    private void skip(long current, long target) throws IOException {
        while (current < target) {
            long skipped = in.skip(target - current);
            if (skipped == 0) {
                if (read() == -1) {
                    break; // EOF
                } else {
                    skipped = 1;
                }
            }
            current += skipped;
        }
    }

    @Override
    public int read() throws IOException {
        if (!allowExpire && (offset + 1 > limit)) {
            setLimit(limit + limitIncrement);
        }
        int result = in.read();
        if (result != -1) {
            offset++;
        }
        return result;
    }

    @Override
    public int read(@NonNull byte[] buffer) throws IOException {
        if (!allowExpire && (offset + buffer.length > limit)) {
            setLimit(offset + buffer.length + limitIncrement);
        }
        int count = in.read(buffer);
        if (count != -1) {
            offset += count;
        }
        return count;
    }

    @Override
    public int read(@NonNull byte[] buffer, int offset, int length) throws IOException {
        if (!allowExpire && (this.offset + length > limit)) {
            setLimit(this.offset + length + limitIncrement);
        }
        int count = in.read(buffer, offset, length);
        if (count != -1) {
            this.offset += count;
        }
        return count;
    }

    @Override public long skip(long byteCount) throws IOException {
        if (!allowExpire && (offset + byteCount > limit)) {
            setLimit(offset + byteCount + limitIncrement);
        }
        long skipped = in.skip(byteCount);
        offset += skipped;
        return skipped;
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }
}
