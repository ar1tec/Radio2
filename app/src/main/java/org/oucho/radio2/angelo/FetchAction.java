package org.oucho.radio2.angelo;

import android.graphics.Bitmap;

class FetchAction extends Action<Object> {

    private final Object target;
    private Callback callback;

    FetchAction(Angelo angelo, Request data, int memoryPolicy, int networkPolicy, Object tag,
                String key, Callback callback) {
        super(angelo, null, data, memoryPolicy, networkPolicy, 0, null, key, tag, false);
        this.target = new Object();
        this.callback = callback;
    }

    @Override void complete(Bitmap result, Angelo.LoadedFrom from) {
        if (callback != null) {
            callback.onSuccess();
        }
    }

    @Override void error(Exception e) {
        if (callback != null) {
            callback.onError(e);
        }
    }

    @Override void cancel() {
        super.cancel();
        callback = null;
    }

    @Override Object getTarget() {
        return target;
    }
}
