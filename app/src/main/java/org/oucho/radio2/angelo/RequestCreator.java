package org.oucho.radio2.angelo;

import android.app.Notification;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.RemoteViews;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.oucho.radio2.angelo.BitmapHunter.forRequest;
import static org.oucho.radio2.angelo.MemoryPolicy.shouldReadFromMemoryCache;
import static org.oucho.radio2.angelo.Angelo.LoadedFrom.MEMORY;
import static org.oucho.radio2.angelo.Angelo.Priority;
import static org.oucho.radio2.angelo.AngeloDrawable.setBitmap;
import static org.oucho.radio2.angelo.AngeloDrawable.setPlaceholder;
import static org.oucho.radio2.angelo.RemoteViewsAction.AppWidgetAction;
import static org.oucho.radio2.angelo.RemoteViewsAction.NotificationAction;
import static org.oucho.radio2.angelo.Utils.checkMain;
import static org.oucho.radio2.angelo.Utils.checkNotMain;
import static org.oucho.radio2.angelo.Utils.createKey;

/** Fluent API for building an image download request. */
@SuppressWarnings("UnusedDeclaration") // Public API.
public class RequestCreator {
    private static final AtomicInteger nextId = new AtomicInteger();

    private final Angelo angelo;
    private final Request.Builder data;

    private boolean noFade;
    private boolean deferred;
    private boolean setPlaceholder = true;
    private int placeholderResId;
    private int errorResId;
    private int memoryPolicy;
    private int networkPolicy;
    private Drawable placeholderDrawable;
    private Drawable errorDrawable;
    private Object tag;

    RequestCreator(Angelo angelo, Uri uri, int resourceId) {
        this.angelo = angelo;
        this.data = new Request.Builder(uri, resourceId);
    }

    @VisibleForTesting
    RequestCreator() {
        this.angelo = null;
        this.data = new Request.Builder(null, 0);
    }

    public RequestCreator noPlaceholder() {
        if (placeholderResId != 0) {
            throw new IllegalStateException("Placeholder resource already set.");
        }
        if (placeholderDrawable != null) {
            throw new IllegalStateException("Placeholder image already set.");
        }
        setPlaceholder = false;
        return this;
    }


    RequestCreator placeholder(@DrawableRes int placeholderResId) {
        if (!setPlaceholder) {
            throw new IllegalStateException("Already explicitly declared as no placeholder.");
        }
        if (placeholderResId == 0) {
            throw new IllegalArgumentException("Placeholder image resource invalid.");
        }
        if (placeholderDrawable != null) {
            throw new IllegalStateException("Placeholder image already set.");
        }
        this.placeholderResId = placeholderResId;
        return this;
    }


    RequestCreator placeholder(@NonNull Drawable placeholderDrawable) {
        if (!setPlaceholder) {
            throw new IllegalStateException("Already explicitly declared as no placeholder.");
        }
        if (placeholderResId != 0) {
            throw new IllegalStateException("Placeholder image already set.");
        }
        this.placeholderDrawable = placeholderDrawable;
        return this;
    }

    /** An error drawable to be used if the request image could not be loaded. */
    public RequestCreator error(@DrawableRes int errorResId) {
        if (errorResId == 0) {
            throw new IllegalArgumentException("Error image resource invalid.");
        }
        if (errorDrawable != null) {
            throw new IllegalStateException("Error image already set.");
        }
        this.errorResId = errorResId;
        return this;
    }

    /** An error drawable to be used if the request image could not be loaded. */
    public RequestCreator error(Drawable errorDrawable) {
        if (errorDrawable == null) {
            throw new IllegalArgumentException("Error image may not be null.");
        }
        if (errorResId != 0) {
            throw new IllegalStateException("Error image already set.");
        }
        this.errorDrawable = errorDrawable;
        return this;
    }


    public RequestCreator tag(Object tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag invalid.");
        }
        if (this.tag != null) {
            throw new IllegalStateException("Tag already set.");
        }
        this.tag = tag;
        return this;
    }


    public RequestCreator fit() {
        deferred = true;
        return this;
    }

    /** Internal use only. Used by {@link DeferredRequestCreator}. */
    RequestCreator unfit() {
        deferred = false;
        return this;
    }

    /** Internal use only. Used by {@link DeferredRequestCreator}. */
    void clearTag() {
        this.tag = null;
    }

    /** Internal use only. Used by {@link DeferredRequestCreator}. */
    Object getTag() {
        return tag;
    }

    /** Resize the image to the specified dimension size. */
    public RequestCreator resizeDimen(int targetWidthResId, int targetHeightResId) {
        assert angelo != null;
        Resources resources = angelo.context.getResources();
        int targetWidth = resources.getDimensionPixelSize(targetWidthResId);
        int targetHeight = resources.getDimensionPixelSize(targetHeightResId);
        return resize(targetWidth, targetHeight);
    }

    /** Resize the image to the specified size in pixels. */
    public RequestCreator resize(int targetWidth, int targetHeight) {
        data.resize(targetWidth, targetHeight);
        return this;
    }


    public RequestCreator centerCrop() {
        data.centerCrop(Gravity.CENTER);
        return this;
    }


    public RequestCreator centerCrop(int alignGravity) {
        data.centerCrop(alignGravity);
        return this;
    }


    public RequestCreator centerInside() {
        data.centerInside();
        return this;
    }


    public RequestCreator onlyScaleDown() {
        data.onlyScaleDown();
        return this;
    }

    /** Rotate the image by the specified degrees. */
    public RequestCreator rotate(float degrees) {
        data.rotate(degrees);
        return this;
    }

    /** Rotate the image by the specified degrees around a pivot point. */
    public RequestCreator rotate(float degrees, float pivotX, float pivotY) {
        data.rotate(degrees, pivotX, pivotY);
        return this;
    }


    public RequestCreator config(@NonNull Bitmap.Config config) {
        data.config(config);
        return this;
    }


    public RequestCreator stableKey(@NonNull String stableKey) {
        data.stableKey(stableKey);
        return this;
    }


    public RequestCreator priority(@NonNull Priority priority) {
        data.priority(priority);
        return this;
    }

    public RequestCreator transform(@NonNull Transformation transformation) {
        data.transform(transformation);
        return this;
    }

    public RequestCreator transform(@NonNull List<? extends Transformation> transformations) {
        data.transform(transformations);
        return this;
    }


    public RequestCreator memoryPolicy(@NonNull MemoryPolicy policy, @NonNull MemoryPolicy... additional) {
        this.memoryPolicy |= policy.index;
        if (additional.length > 0) {
            for (MemoryPolicy memoryPolicy : additional) {
                if (memoryPolicy == null) {
                    throw new IllegalArgumentException("Memory policy cannot be null.");
                }
                this.memoryPolicy |= memoryPolicy.index;
            }
        }
        return this;
    }


    public RequestCreator networkPolicy(@NonNull NetworkPolicy policy, @NonNull NetworkPolicy... additional) {
        this.networkPolicy |= policy.index;
        if (additional.length > 0) {
            for (NetworkPolicy networkPolicy : additional) {
                if (networkPolicy == null) {
                    throw new IllegalArgumentException("Network policy cannot be null.");
                }
                this.networkPolicy |= networkPolicy.index;
            }
        }
        return this;
    }


    public RequestCreator purgeable() {
        data.purgeable();
        return this;
    }

    /** Disable brief fade in of images loaded from the disk cache or network. */
    public RequestCreator noFade() {
        noFade = true;
        return this;
    }


    public Bitmap get() throws IOException {
        checkNotMain();

        if (deferred) {
            throw new IllegalStateException("Fit cannot be used with get.");
        }
        if (!data.hasImage()) {
            return null;
        }

        Request finalData = createRequest();
        String key = createKey(finalData, new StringBuilder());

        Action action = new GetAction(angelo, finalData, memoryPolicy, networkPolicy, tag, key);
        assert angelo != null;
        return forRequest(angelo, angelo.dispatcher, angelo.cache, action).hunt();
    }

    public void fetch() {
        fetch(null);
    }


    private void fetch(@Nullable Callback callback) {

        if (deferred) {
            throw new IllegalStateException("Fit cannot be used with fetch.");
        }
        if (data.hasImage()) {
            // Fetch requests have lower priority by default.
            if (!data.hasPriority()) {
                data.priority(Priority.LOW);
            }

            Request request = createRequest();
            String key = createKey(request, new StringBuilder());

            if (shouldReadFromMemoryCache(memoryPolicy)) {
                assert angelo != null;
                Bitmap bitmap = angelo.quickMemoryCacheCheck(key);
                if (bitmap != null) {

                    if (callback != null) {
                        callback.onSuccess();
                    }
                    return;
                }
            }

            Action action =
                    new FetchAction(angelo, request, memoryPolicy, networkPolicy, tag, key, callback);
            assert angelo != null;
            angelo.submit(action);
        }
    }


    public void into(Target target) {
        checkMain();

        if (target == null) {
            throw new IllegalArgumentException("Target must not be null.");
        }
        if (deferred) {
            throw new IllegalStateException("Fit cannot be used with a Target.");
        }

        if (!data.hasImage()) {
            assert angelo != null;
            angelo.cancelRequest(target);
            target.onPrepareLoad(setPlaceholder ? getPlaceholderDrawable() : null);
            return;
        }

        Request request = createRequest();
        String requestKey = createKey(request);

        if (shouldReadFromMemoryCache(memoryPolicy)) {
            assert angelo != null;
            Bitmap bitmap = angelo.quickMemoryCacheCheck(requestKey);
            if (bitmap != null) {
                angelo.cancelRequest(target);
                target.onBitmapLoaded(bitmap, MEMORY);
                return;
            }
        }

        target.onPrepareLoad(setPlaceholder ? getPlaceholderDrawable() : null);

        Action action =
                new TargetAction(angelo, target, request, memoryPolicy, networkPolicy, errorDrawable,
                        requestKey, tag, errorResId);
        assert angelo != null;
        angelo.enqueueAndSubmit(action);
    }

    /**
     * Asynchronously fulfills the request into the specified {@link RemoteViews} object with the
     * given {@code viewId}. This is used for loading bitmaps into a {@link Notification}.
     */
    public void into(@NonNull RemoteViews remoteViews, @IdRes int viewId, int notificationId, @NonNull Notification notification) {
        into(remoteViews, viewId, notificationId, notification, null);
    }

    /**
     * Asynchronously fulfills the request into the specified {@link RemoteViews} object with the
     * given {@code viewId}. This is used for loading bitmaps into a {@link Notification}.
     */
    private void into(@NonNull RemoteViews remoteViews, @IdRes int viewId, int notificationId, @NonNull Notification notification, @Nullable String notificationTag) {
        into(remoteViews, viewId, notificationId, notification, notificationTag, null);
    }

    /**
     * Asynchronously fulfills the request into the specified {@link RemoteViews} object with the
     * given {@code viewId}. This is used for loading bitmaps into a {@link Notification}.
     */
    private void into(RemoteViews remoteViews, @IdRes int viewId, int notificationId, Notification notification, @Nullable String notificationTag, Callback callback) {

        if (remoteViews == null) {
            throw new IllegalArgumentException("RemoteViews must not be null.");
        }
        if (notification == null) {
            throw new IllegalArgumentException("Notification must not be null.");
        }
        if (deferred) {
            throw new IllegalStateException("Fit cannot be used with RemoteViews.");
        }
        if (placeholderDrawable != null || placeholderResId != 0 || errorDrawable != null) {
            throw new IllegalArgumentException(
                    "Cannot use placeholder or error drawables with remote views.");
        }

        Request request = createRequest();
        String key = createKey(request, new StringBuilder()); // Non-main thread needs own builder.

        RemoteViewsAction action =
                new NotificationAction(angelo, request, remoteViews, viewId, notificationId, notification,
                        notificationTag, memoryPolicy, networkPolicy, key, tag, errorResId, callback);

        performRemoteViewInto(action);
    }

    /**
     * Asynchronously fulfills the request into the specified {@link RemoteViews} object with the
     * given {@code viewId}. This is used for loading bitmaps into all instances of a widget.
     */
    public void into(@NonNull RemoteViews remoteViews, @IdRes int viewId, @NonNull int[] appWidgetIds) {
        into(remoteViews, viewId, appWidgetIds, null);
    }


    private void into(RemoteViews remoteViews, @IdRes int viewId, int[] appWidgetIds, Callback callback) {

        if (remoteViews == null) {
            throw new IllegalArgumentException("remoteViews must not be null.");
        }
        if (appWidgetIds == null) {
            throw new IllegalArgumentException("appWidgetIds must not be null.");
        }
        if (deferred) {
            throw new IllegalStateException("Fit cannot be used with remote views.");
        }
        if (placeholderDrawable != null || placeholderResId != 0 || errorDrawable != null) {
            throw new IllegalArgumentException(
                    "Cannot use placeholder or error drawables with remote views.");
        }

        Request request = createRequest();
        String key = createKey(request, new StringBuilder()); // Non-main thread needs own builder.

        RemoteViewsAction action =
                new AppWidgetAction(angelo, request, remoteViews, viewId, appWidgetIds, memoryPolicy,
                        networkPolicy, key, tag, errorResId, callback);

        performRemoteViewInto(action);
    }


    public void into(ImageView target) {
        into(target, null);
    }


    void into(ImageView target, Callback callback) {

        checkMain();

        if (target == null) {
            throw new IllegalArgumentException("Target must not be null.");
        }

        if (!data.hasImage()) {
            assert angelo != null;
            angelo.cancelRequest(target);
            if (setPlaceholder) {
                setPlaceholder(target, getPlaceholderDrawable());
            }
            return;
        }

        if (deferred) {
            if (data.hasSize()) {
                throw new IllegalStateException("Fit cannot be used with resize.");
            }
            int width = target.getWidth();
            int height = target.getHeight();
            if (width == 0 || height == 0 || target.isLayoutRequested()) {
                if (setPlaceholder) {
                    setPlaceholder(target, getPlaceholderDrawable());
                }
                assert angelo != null;
                angelo.defer(target, new DeferredRequestCreator(this, target, callback));
                return;
            }
            data.resize(width, height);
        }

        Request request = createRequest();
        String requestKey = createKey(request);

        if (shouldReadFromMemoryCache(memoryPolicy)) {
            assert angelo != null;
            Bitmap bitmap = angelo.quickMemoryCacheCheck(requestKey);
            if (bitmap != null) {
                angelo.cancelRequest(target);
                setBitmap(target, angelo.context, bitmap, MEMORY, noFade);
                if (callback != null) {
                    callback.onSuccess();
                }
                return;
            }
        }

        if (setPlaceholder) {
            setPlaceholder(target, getPlaceholderDrawable());
        }

        Action action =
                new ImageViewAction(angelo, target, request, memoryPolicy, networkPolicy, errorResId,
                        errorDrawable, requestKey, tag, callback, noFade);

        assert angelo != null;
        angelo.enqueueAndSubmit(action);
    }

    private Drawable getPlaceholderDrawable() {
        if (placeholderResId != 0) {
            assert angelo != null;
            return angelo.context.getResources().getDrawable(placeholderResId);
        } else {
            return placeholderDrawable; // This may be null which is expected and desired behavior.
        }
    }

    private Request createRequest() {
        Request request = data.build();
        assert angelo != null;
        return angelo.transformRequest(request);
    }

    private void performRemoteViewInto(RemoteViewsAction action) {
        if (shouldReadFromMemoryCache(memoryPolicy)) {
            assert angelo != null;
            Bitmap bitmap = angelo.quickMemoryCacheCheck(action.getKey());
            if (bitmap != null) {
                action.complete(bitmap, MEMORY);
                return;
            }
        }

        if (placeholderResId != 0) {
            action.setImageResource(placeholderResId);
        }

        assert angelo != null;
        angelo.enqueueAndSubmit(action);
    }
}
