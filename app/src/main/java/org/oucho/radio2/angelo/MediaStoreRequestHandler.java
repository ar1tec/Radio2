package org.oucho.radio2.angelo;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.IOException;
import okio.Okio;
import okio.Source;

import static android.content.ContentResolver.SCHEME_CONTENT;
import static android.content.ContentUris.parseId;
import static android.provider.MediaStore.Images;
import static android.provider.MediaStore.Video;
import static android.provider.MediaStore.Images.Thumbnails.FULL_SCREEN_KIND;
import static android.provider.MediaStore.Images.Thumbnails.MICRO_KIND;
import static android.provider.MediaStore.Images.Thumbnails.MINI_KIND;
import static org.oucho.radio2.angelo.MediaStoreRequestHandler.AngeloKind.FULL;
import static org.oucho.radio2.angelo.MediaStoreRequestHandler.AngeloKind.MICRO;
import static org.oucho.radio2.angelo.MediaStoreRequestHandler.AngeloKind.MINI;
import static org.oucho.radio2.angelo.Angelo.LoadedFrom.DISK;

class MediaStoreRequestHandler extends ContentStreamRequestHandler {

    MediaStoreRequestHandler(Context context) {
        super(context);
    }

    @Override
    public boolean canHandleRequest(Request data) {
        final Uri uri = data.uri;
        return (SCHEME_CONTENT.equals(uri.getScheme())
                && MediaStore.AUTHORITY.equals(uri.getAuthority()));
    }

    @Override
    public Result load(Request request, int networkPolicy) throws IOException {
        ContentResolver contentResolver = context.getContentResolver();

        String mimeType = contentResolver.getType(request.uri);
        boolean isVideo = mimeType != null && mimeType.startsWith("video/");

        if (request.hasSize()) {
            AngeloKind angeloKind = getAngeloKind(request.targetWidth, request.targetHeight);
            if (!isVideo && angeloKind == FULL) {
                Source source = Okio.source(getInputStream(request));
                return new Result(null, source, DISK);
            }

            long id = parseId(request.uri);

            BitmapFactory.Options options = createBitmapOptions(request);
            options.inJustDecodeBounds = true;

            calculateInSampleSize(request.targetWidth, request.targetHeight, angeloKind.width,
                    angeloKind.height, options, request);

            Bitmap bitmap;

            if (isVideo) {
                // Since MediaStore doesn't provide the full screen kind thumbnail, we use the mini kind
                // instead which is the largest thumbnail size can be fetched from MediaStore.
                int kind = (angeloKind == FULL) ? Video.Thumbnails.MINI_KIND : angeloKind.androidKind;
                bitmap = Video.Thumbnails.getThumbnail(contentResolver, id, kind, options);
            } else {
                bitmap =
                        Images.Thumbnails.getThumbnail(contentResolver, id, angeloKind.androidKind, options);
            }

            if (bitmap != null) {
                return new Result(bitmap, null, DISK);
            }
        }

        Source source = Okio.source(getInputStream(request));
        return new Result(null, source, DISK);
    }

    private static AngeloKind getAngeloKind(int targetWidth, int targetHeight) {
        if (targetWidth <= MICRO.width && targetHeight <= MICRO.height) {
            return MICRO;
        } else if (targetWidth <= MINI.width && targetHeight <= MINI.height) {
            return MINI;
        }
        return FULL;
    }

    enum AngeloKind {
        MICRO(MICRO_KIND, 96, 96),
        MINI(MINI_KIND, 512, 384),
        FULL(FULL_SCREEN_KIND, -1, -1);

        final int androidKind;
        final int width;
        final int height;

        AngeloKind(int androidKind, int width, int height) {
            this.androidKind = androidKind;
            this.width = width;
            this.height = height;
        }
    }
}
