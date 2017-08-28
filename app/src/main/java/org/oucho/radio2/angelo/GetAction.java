package org.oucho.radio2.angelo;

import android.graphics.Bitmap;

class GetAction extends Action<Void> {
    GetAction(Angelo angelo, Request data, int memoryPolicy, int networkPolicy, Object tag,
              String key) {
        super(angelo, null, data, memoryPolicy, networkPolicy, 0, null, key, tag, false);
    }

    @Override void complete(Bitmap result, Angelo.LoadedFrom from) {
    }

    @Override public void error(Exception e) {
    }
}
