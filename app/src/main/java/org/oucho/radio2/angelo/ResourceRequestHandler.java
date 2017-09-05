package org.oucho.radio2.angelo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.IOException;

import static android.content.ContentResolver.SCHEME_ANDROID_RESOURCE;

class ResourceRequestHandler extends RequestHandler {
    private final Context context;

    ResourceRequestHandler(Context context) {
        this.context = context;
    }

    @Override public boolean canHandleRequest(Request data) {
        return data.resourceId != 0 || SCHEME_ANDROID_RESOURCE.equals(data.uri.getScheme());

    }

    @Override public Result load(Request request, int networkPolicy) throws IOException {
        Resources res = Utils.getResources(context, request);
        int id = Utils.getResourceId(res, request);
        return new Result(decodeResource(res, id, request));
    }

    private static Bitmap decodeResource(Resources resources, int id, Request data) {
        final BitmapFactory.Options options = createBitmapOptions(data);
        if (requiresInSampleSize(options)) {
            BitmapFactory.decodeResource(resources, id, options);
            calculateInSampleSize(data.targetWidth, data.targetHeight, options, data);
        }
        return BitmapFactory.decodeResource(resources, id, options);
    }
}
