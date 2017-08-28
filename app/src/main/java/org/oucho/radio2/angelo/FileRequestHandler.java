package org.oucho.radio2.angelo;

import android.content.Context;

import java.io.IOException;
import okio.Okio;
import okio.Source;

import static android.content.ContentResolver.SCHEME_FILE;
import static org.oucho.radio2.angelo.Angelo.LoadedFrom.DISK;


class FileRequestHandler extends ContentStreamRequestHandler {

    FileRequestHandler(Context context) {
        super(context);
    }

    @Override public boolean canHandleRequest(Request data) {
        return SCHEME_FILE.equals(data.uri.getScheme());
    }

    @Override public Result load(Request request, int networkPolicy) throws IOException {
        Source source = Okio.source(getInputStream(request));
        return new Result(null, source, DISK);
    }

}
