package org.oucho.radio2.angelo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.NetworkInfo;
import android.view.Gravity;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;


import static org.oucho.radio2.angelo.MemoryPolicy.shouldReadFromMemoryCache;
import static org.oucho.radio2.angelo.Angelo.LoadedFrom.MEMORY;
import static org.oucho.radio2.angelo.Angelo.Priority;
import static org.oucho.radio2.angelo.Angelo.Priority.LOW;

class BitmapHunter implements Runnable {

    private static final Object DECODE_LOCK = new Object();

    private static final ThreadLocal<StringBuilder> NAME_BUILDER = new ThreadLocal<StringBuilder>() {
        @Override protected StringBuilder initialValue() {
            return new StringBuilder(Utils.THREAD_PREFIX);
        }
    };

    private static final AtomicInteger SEQUENCE_GENERATOR = new AtomicInteger();

    private static final RequestHandler ERRORING_HANDLER = new RequestHandler() {
        @Override public boolean canHandleRequest(Request data) {
            return true;
        }

        @Override public Result load(Request request, int networkPolicy) throws IOException {
            throw new IllegalStateException("Unrecognized type of request: " + request);
        }
    };

    final int sequence;
    final Angelo angelo;
    private final Dispatcher dispatcher;
    private final Cache cache;
    private final String key;
    private final Request data;
    private final int memoryPolicy;
    int networkPolicy;
    private final RequestHandler requestHandler;

    private Action action;
    private List<Action> actions;
    Bitmap result;
    Future<?> future;
    private Angelo.LoadedFrom loadedFrom;
    private Exception exception;
    private int retryCount;
    private Priority priority;

    private BitmapHunter(Angelo angelo, Dispatcher dispatcher, Cache cache, Action action, RequestHandler requestHandler) {
        this.sequence = SEQUENCE_GENERATOR.incrementAndGet();
        this.angelo = angelo;
        this.dispatcher = dispatcher;
        this.cache = cache;
        this.action = action;
        this.key = action.getKey();
        this.data = action.getRequest();
        this.priority = action.getPriority();
        this.memoryPolicy = action.getMemoryPolicy();
        this.networkPolicy = action.getNetworkPolicy();
        this.requestHandler = requestHandler;
        this.retryCount = requestHandler.getRetryCount();
    }


    private static Bitmap decodeStream(Source source, Request request) throws IOException {
        BufferedSource bufferedSource = Okio.buffer(source);

        boolean isWebPFile = Utils.isWebPFile(bufferedSource);
        BitmapFactory.Options options = RequestHandler.createBitmapOptions(request);
        boolean calculateSize = RequestHandler.requiresInSampleSize(options);


        if (isWebPFile) {
            byte[] bytes = bufferedSource.readByteArray();
            if (calculateSize) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                RequestHandler.calculateInSampleSize(request.targetWidth, request.targetHeight, options, request);
            }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        } else {
            InputStream stream = bufferedSource.inputStream();
            if (calculateSize) {
                // TODO use an InputStream that buffers with Okio...
                MarkableInputStream markStream = new MarkableInputStream(stream);
                stream = markStream;
                markStream.allowMarksToExpire(false);
                long mark = markStream.savePosition(1024);
                BitmapFactory.decodeStream(stream, null, options);
                RequestHandler.calculateInSampleSize(request.targetWidth, request.targetHeight, options, request);
                markStream.reset(mark);
                markStream.allowMarksToExpire(true);
            }
            Bitmap bitmap = BitmapFactory.decodeStream(stream, null, options);
            if (bitmap == null) {
                // Treat null as an IO exception, we will eventually retry.
                throw new IOException("Failed to decode stream.");
            }
            return bitmap;
        }
    }

    @Override public void run() {
        try {
            updateThreadName(data);

            result = hunt();

            if (result == null) {
                dispatcher.dispatchFailed(this);
            } else {
                dispatcher.dispatchComplete(this);
            }
        } catch (NetworkRequestHandler.ResponseException e) {
            if (e.code != 504) {
                exception = e;
            }
            dispatcher.dispatchFailed(this);
        } catch (IOException e) {
            exception = e;
            dispatcher.dispatchRetry(this);
        } catch (OutOfMemoryError e) {
            StringWriter writer = new StringWriter();
            exception = new RuntimeException(writer.toString(), e);
            dispatcher.dispatchFailed(this);
        } catch (Exception e) {
            exception = e;
            dispatcher.dispatchFailed(this);
        } finally {
            Thread.currentThread().setName(Utils.THREAD_IDLE_NAME);
        }
    }

    Bitmap hunt() throws IOException {
        Bitmap bitmap = null;

        if (shouldReadFromMemoryCache(memoryPolicy)) {
            bitmap = cache.get(key);
            if (bitmap != null) {
                loadedFrom = MEMORY;
                return bitmap;
            }
        }

        networkPolicy = retryCount == 0 ? NetworkPolicy.OFFLINE.index : networkPolicy;
        RequestHandler.Result result = requestHandler.load(data, networkPolicy);
        if (result != null) {
            loadedFrom = result.getLoadedFrom();
            bitmap = result.getBitmap();

            // If there was no Bitmap then we need to decode it from the stream.
            if (bitmap == null) {
                Source source = result.getSource();
                try {
                    bitmap = decodeStream(source, data);
                } finally {
                    try {
                        //noinspection ConstantConditions If bitmap is null then source is guranteed non-null.
                        source.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        if (bitmap != null) {

            if (data.needsTransformation()) {
                synchronized (DECODE_LOCK) {
                    if (data.needsMatrixTransform()) {
                        bitmap = transformResult(data, bitmap);

                    }
                    if (data.hasCustomTransformations()) {
                        bitmap = applyCustomTransformations(data.transformations, bitmap);
                    }
                }

            }
        }

        return bitmap;
    }

    void attach(Action action) {

        if (this.action == null) {
            this.action = action;

            return;
        }

        if (actions == null) {
            actions = new ArrayList<>(3);
        }

        actions.add(action);

        Priority actionPriority = action.getPriority();
        if (actionPriority.ordinal() > priority.ordinal()) {
            priority = actionPriority;
        }
    }

    void detach(Action action) {
        boolean detached = false;
        if (this.action == action) {
            this.action = null;
            detached = true;
        } else if (actions != null) {
            detached = actions.remove(action);
        }

        // The action being detached had the highest priority. Update this
        // hunter's priority with the remaining actions.
        if (detached && action.getPriority() == priority) {
            priority = computeNewPriority();
        }
    }

    private Priority computeNewPriority() {
        Priority newPriority = LOW;

        boolean hasMultiple = actions != null && !actions.isEmpty();
        boolean hasAny = action != null || hasMultiple;

        // Hunter has no requests, low priority.
        if (!hasAny) {
            return newPriority;
        }

        if (action != null) {
            newPriority = action.getPriority();
        }

        if (hasMultiple) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, n = actions.size(); i < n; i++) {
                Priority actionPriority = actions.get(i).getPriority();
                if (actionPriority.ordinal() > newPriority.ordinal()) {
                    newPriority = actionPriority;
                }
            }
        }

        return newPriority;
    }

    boolean cancel() {
        return action == null && (actions == null || actions.isEmpty()) && future != null && future.cancel(false);
    }

    boolean isCancelled() {
        return future != null && future.isCancelled();
    }

    boolean shouldRetry(NetworkInfo info) {
        boolean hasRetries = retryCount > 0;
        if (!hasRetries) {
            return false;
        }
        retryCount--;
        return requestHandler.shouldRetry(info);
    }

    boolean supportsReplay() {
        return requestHandler.supportsReplay();
    }

    Bitmap getResult() {
        return result;
    }

    String getKey() {
        return key;
    }

    int getMemoryPolicy() {
        return memoryPolicy;
    }

    Action getAction() {
        return action;
    }

    List<Action> getActions() {
        return actions;
    }

    Exception getException() {
        return exception;
    }

    Angelo.LoadedFrom getLoadedFrom() {
        return loadedFrom;
    }

    Priority getPriority() {
        return priority;
    }

    private static void updateThreadName(Request data) {
        String name = data.getName();

        StringBuilder builder = NAME_BUILDER.get();
        builder.ensureCapacity(Utils.THREAD_PREFIX.length() + name.length());
        builder.replace(Utils.THREAD_PREFIX.length(), builder.length(), name);

        Thread.currentThread().setName(builder.toString());
    }

    static BitmapHunter forRequest(Angelo angelo, Dispatcher dispatcher, Cache cache, Action action) {
        Request request = action.getRequest();
        List<RequestHandler> requestHandlers = angelo.getRequestHandlers();

        // Index-based loop to avoid allocating an iterator.
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, count = requestHandlers.size(); i < count; i++) {
            RequestHandler requestHandler = requestHandlers.get(i);
            if (requestHandler.canHandleRequest(request)) {
                return new BitmapHunter(angelo, dispatcher, cache, action, requestHandler);
            }
        }

        return new BitmapHunter(angelo, dispatcher, cache, action, ERRORING_HANDLER);
    }

    private static Bitmap applyCustomTransformations(List<Transformation> transformations, Bitmap result) {

        for (int i = 0, count = transformations.size(); i < count; i++) {

            final Transformation transformation = transformations.get(i);
            Bitmap newResult;

            try {
                newResult = transformation.transform(result);
            } catch (final RuntimeException e) {
                Angelo.HANDLER.post(() -> {
                    throw new RuntimeException("Transformation " + transformation.key() + " crashed with exception.", e);
                });
                return null;
            }

            if (newResult == null) {
                final StringBuilder builder = new StringBuilder() //
                        .append("Transformation ")
                        .append(transformation.key())
                        .append(" returned null after ")
                        .append(i)
                        .append(" previous transformation(s).\n\nTransformation list:\n");

                for (Transformation t : transformations) {
                    builder.append(t.key()).append('\n');
                }

                Angelo.HANDLER.post(() -> {
                    throw new NullPointerException(builder.toString());
                });

                return null;
            }

            if (newResult == result && result.isRecycled()) {
                Angelo.HANDLER.post(() -> {
                    throw new IllegalStateException("Transformation " + transformation.key() + " returned input Bitmap but recycled it.");
                });
                return null;
            }

            // If the transformation returned a new bitmap ensure they recycled the original.
            if (newResult != result && !result.isRecycled()) {
                Angelo.HANDLER.post(() -> {
                    throw new IllegalStateException("Transformation " + transformation.key() + " mutated input Bitmap but failed to recycle the original.");
                });
                return null;
            }

            result = newResult;
        }
        return result;
    }

    private static Bitmap transformResult(Request data, Bitmap result) {
        int inWidth = result.getWidth();
        int inHeight = result.getHeight();
        boolean onlyScaleDown = data.onlyScaleDown;

        int drawX = 0;
        int drawY = 0;
        int drawWidth = inWidth;
        int drawHeight = inHeight;

        Matrix matrix = new Matrix();

        if (data.needsMatrixTransform()) {
            int targetWidth = data.targetWidth;
            int targetHeight = data.targetHeight;

            float targetRotation = data.rotationDegrees;

            if (targetRotation != 0) {

                double cosR = Math.cos(Math.toRadians(targetRotation));
                double sinR = Math.sin(Math.toRadians(targetRotation));

                if (data.hasRotationPivot) {
                    matrix.setRotate(targetRotation, data.rotationPivotX, data.rotationPivotY);
                    // Recalculate dimensions after rotation around pivot point
                    double x1T = data.rotationPivotX * (1.0 - cosR) + (data.rotationPivotY * sinR);
                    double y1T = data.rotationPivotY * (1.0 - cosR) - (data.rotationPivotX * sinR);
                    double x2T = x1T + (data.targetWidth * cosR);
                    double y2T = y1T + (data.targetWidth * sinR);
                    double x3T = x1T + (data.targetWidth * cosR) - (data.targetHeight * sinR);
                    double y3T = y1T + (data.targetWidth * sinR) + (data.targetHeight * cosR);
                    double x4T = x1T - (data.targetHeight * sinR);
                    double y4T = y1T + (data.targetHeight * cosR);

                    double maxX = Math.max(x4T, Math.max(x3T, Math.max(x1T, x2T)));
                    double minX = Math.min(x4T, Math.min(x3T, Math.min(x1T, x2T)));
                    double maxY = Math.max(y4T, Math.max(y3T, Math.max(y1T, y2T)));
                    double minY = Math.min(y4T, Math.min(y3T, Math.min(y1T, y2T)));
                    targetWidth = (int) Math.floor(maxX - minX);
                    targetHeight  = (int) Math.floor(maxY - minY);

                } else {
                    matrix.setRotate(targetRotation);
                    // Recalculate dimensions after rotation (around origin)
                    double x1T = 0.0;
                    double y1T = 0.0;
                    double x2T = (data.targetWidth * cosR);
                    double y2T = (data.targetWidth * sinR);
                    double x3T = (data.targetWidth * cosR) - (data.targetHeight * sinR);
                    double y3T = (data.targetWidth * sinR) + (data.targetHeight * cosR);
                    double x4T = -(data.targetHeight * sinR);
                    double y4T = (data.targetHeight * cosR);

                    double maxX = Math.max(x4T, Math.max(x3T, Math.max(x1T, x2T)));
                    double minX = Math.min(x4T, Math.min(x3T, Math.min(x1T, x2T)));
                    double maxY = Math.max(y4T, Math.max(y3T, Math.max(y1T, y2T)));
                    double minY = Math.min(y4T, Math.min(y3T, Math.min(y1T, y2T)));
                    targetWidth = (int) Math.floor(maxX - minX);
                    targetHeight  = (int) Math.floor(maxY - minY);
                }
            }


            if (data.centerCrop) {
                // Keep aspect ratio if one dimension is set to 0
                float widthRatio = targetWidth != 0 ? targetWidth / (float) inWidth : targetHeight / (float) inHeight;
                float heightRatio = targetHeight != 0 ? targetHeight / (float) inHeight : targetWidth / (float) inWidth;
                float scaleX, scaleY;

                if (widthRatio > heightRatio) {

                    int newSize = (int) Math.ceil(inHeight * (heightRatio / widthRatio));

                    if ((data.centerCropGravity & Gravity.TOP) == Gravity.TOP) {
                        drawY = 0;
                    } else if ((data.centerCropGravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
                        drawY = inHeight - newSize;
                    } else {
                        drawY = (inHeight - newSize) / 2;
                    }

                    drawHeight = newSize;
                    scaleX = widthRatio;
                    scaleY = targetHeight / (float) drawHeight;

                } else if (widthRatio < heightRatio) {

                    int newSize = (int) Math.ceil(inWidth * (widthRatio / heightRatio));

                    if ((data.centerCropGravity & Gravity.LEFT) == Gravity.LEFT) {
                        drawX = 0;
                    } else if ((data.centerCropGravity & Gravity.RIGHT) == Gravity.RIGHT) {
                        drawX = inWidth - newSize;
                    } else {
                        drawX = (inWidth - newSize) / 2;
                    }

                    drawWidth = newSize;
                    scaleX = targetWidth / (float) drawWidth;
                    scaleY = heightRatio;

                } else {
                    drawX = 0;
                    drawWidth = inWidth;
                    scaleX = scaleY = heightRatio;
                }

                if (shouldResize(onlyScaleDown, inWidth, inHeight, targetWidth, targetHeight)) {
                    matrix.preScale(scaleX, scaleY);
                }

            } else if (data.centerInside) {
                // Keep aspect ratio if one dimension is set to 0
                float widthRatio = targetWidth != 0 ? targetWidth / (float) inWidth : targetHeight / (float) inHeight;
                float heightRatio = targetHeight != 0 ? targetHeight / (float) inHeight : targetWidth / (float) inWidth;
                float scale = widthRatio < heightRatio ? widthRatio : heightRatio;

                if (shouldResize(onlyScaleDown, inWidth, inHeight, targetWidth, targetHeight)) {
                    matrix.preScale(scale, scale);
                }

            } else if ((targetWidth != 0 || targetHeight != 0) && (targetWidth != inWidth || targetHeight != inHeight)) {
                // If an explicit target size has been specified and they do not match the results bounds,
                // pre-scale the existing matrix appropriately.
                // Keep aspect ratio if one dimension is set to 0.
                float sx = targetWidth != 0 ? targetWidth / (float) inWidth : targetHeight / (float) inHeight;
                float sy = targetHeight != 0 ? targetHeight / (float) inHeight : targetWidth / (float) inWidth;

                if (shouldResize(onlyScaleDown, inWidth, inHeight, targetWidth, targetHeight)) {
                    matrix.preScale(sx, sy);
                }
            }
        }

        Bitmap newResult = Bitmap.createBitmap(result, drawX, drawY, drawWidth, drawHeight, matrix, true);
        if (newResult != result) {
            result.recycle();
            result = newResult;
        }

        return result;
    }

    private static boolean shouldResize(boolean onlyScaleDown, int inWidth, int inHeight, int targetWidth, int targetHeight) {
        return !onlyScaleDown || (targetWidth != 0 && inWidth > targetWidth) || (targetHeight != 0 && inHeight > targetHeight);
    }

}

