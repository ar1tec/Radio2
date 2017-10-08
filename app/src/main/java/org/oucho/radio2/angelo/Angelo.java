package org.oucho.radio2.angelo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static org.oucho.radio2.angelo.Action.RequestWeakReference;
import static org.oucho.radio2.angelo.Dispatcher.HUNTER_BATCH_COMPLETE;
import static org.oucho.radio2.angelo.Dispatcher.REQUEST_BATCH_RESUME;
import static org.oucho.radio2.angelo.Dispatcher.REQUEST_GCED;
import static org.oucho.radio2.angelo.MemoryPolicy.shouldReadFromMemoryCache;
import static org.oucho.radio2.angelo.Angelo.LoadedFrom.MEMORY;
import static org.oucho.radio2.angelo.Utils.THREAD_LEAK_CLEANING_MS;
import static org.oucho.radio2.angelo.Utils.THREAD_PREFIX;
import static org.oucho.radio2.angelo.Utils.checkMain;


public class Angelo {

    public interface RequestTransformer {
        Request transformRequest(Request request);

        RequestTransformer IDENTITY = request -> request;
    }

    public enum Priority {
        LOW,
        NORMAL
    }

    static final Handler HANDLER = new Handler(Looper.getMainLooper()) {

        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case HUNTER_BATCH_COMPLETE: {

                    List<BitmapHunter> batch = (List<BitmapHunter>) msg.obj;

                    for (int i = 0, n = batch.size(); i < n; i++) {
                        BitmapHunter hunter = batch.get(i);
                        hunter.angelo.complete(hunter);
                    }
                    break;
                }
                case REQUEST_GCED: {
                    Action action = (Action) msg.obj;
                    action.angelo.cancelExistingRequest(action.getTarget());
                    break;
                }
                case REQUEST_BATCH_RESUME:
                    List<Action> batch = (List<Action>) msg.obj;

                    for (int i = 0, n = batch.size(); i < n; i++) {
                        Action action = batch.get(i);
                        action.angelo.resumeAction(action);
                    }
                    break;
                default:
                    throw new AssertionError("Unknown handler message received: " + msg.what);
            }
        }
    };

    @SuppressLint("StaticFieldLeak")
    private static volatile Angelo singleton = null;

    private final RequestTransformer requestTransformer;
    private final List<RequestHandler> requestHandlers;

    final Context context;
    final Dispatcher dispatcher;
    final Cache cache;
    private final Map<Object, Action> targetToAction;
    private final Map<ImageView, DeferredRequestCreator> targetToDeferredRequestCreator;
    final ReferenceQueue<Object> referenceQueue;

    private Angelo(Context context, Dispatcher dispatcher, Cache cache, RequestTransformer requestTransformer) {

        this.context = context;
        this.dispatcher = dispatcher;
        this.cache = cache;
        this.requestTransformer = requestTransformer;

        int builtInHandlers = 7; // Adjust this as internal handlers are added or removed.
        List<RequestHandler> allRequestHandlers = new ArrayList<>(builtInHandlers);

        allRequestHandlers.add(new ResourceRequestHandler(context));
        allRequestHandlers.add(new MediaStoreRequestHandler(context));
        allRequestHandlers.add(new ContentStreamRequestHandler(context));
        allRequestHandlers.add(new AssetRequestHandler(context));
        allRequestHandlers.add(new FileRequestHandler(context));
        allRequestHandlers.add(new NetworkRequestHandler(dispatcher.downloader));
        requestHandlers = Collections.unmodifiableList(allRequestHandlers);

        this.targetToAction = new WeakHashMap<>();
        this.targetToDeferredRequestCreator = new WeakHashMap<>();
        this.referenceQueue = new ReferenceQueue<>();
        CleanupThread cleanupThread = new CleanupThread(referenceQueue);
        cleanupThread.start();
    }

    void cancelRequest(ImageView view) {

        if (view == null) {
            throw new IllegalArgumentException("view cannot be null.");
        }
        cancelExistingRequest(view);
    }

    void cancelRequest(Target target) {

        if (target == null) {
            throw new IllegalArgumentException("target cannot be null.");
        }
        cancelExistingRequest(target);
    }

    private RequestCreator load(@Nullable Uri uri) {
        return new RequestCreator(this, uri, 0);
    }


    public RequestCreator load(@Nullable String path) {
        if (path == null) {
            return new RequestCreator(this, null, 0);
        }
        if (path.trim().length() == 0) {
            throw new IllegalArgumentException("Path must not be empty.");
        }
        return load(Uri.parse(path));
    }

    List<RequestHandler> getRequestHandlers() {
        return requestHandlers;
    }

    Request transformRequest(Request request) {
        Request transformed = requestTransformer.transformRequest(request);
        if (transformed == null) {
            throw new IllegalStateException("Request transformer "
                    + requestTransformer.getClass().getCanonicalName()
                    + " returned null for "
                    + request);
        }
        return transformed;
    }

    void defer(ImageView view, DeferredRequestCreator request) {

        if (targetToDeferredRequestCreator.containsKey(view)) {
            cancelExistingRequest(view);
        }
        targetToDeferredRequestCreator.put(view, request);
    }

    void enqueueAndSubmit(Action action) {
        Object target = action.getTarget();
        if (target != null && targetToAction.get(target) != action) {
            cancelExistingRequest(target);
            targetToAction.put(target, action);
        }
        submit(action);
    }

    void submit(Action action) {
        dispatcher.dispatchSubmit(action);
    }

    Bitmap quickMemoryCacheCheck(String key) {
        return cache.get(key);
    }

    private void complete(BitmapHunter hunter) {
        Action single = hunter.getAction();
        List<Action> joined = hunter.getActions();

        boolean hasMultiple = joined != null && !joined.isEmpty();
        boolean shouldDeliver = single != null || hasMultiple;

        if (!shouldDeliver) {
            return;
        }

        Exception exception = hunter.getException();
        Bitmap result = hunter.getResult();
        LoadedFrom from = hunter.getLoadedFrom();

        if (single != null) {
            deliverAction(result, from, single, exception);
        }

        if (hasMultiple) {

            for (int i = 0, n = joined.size(); i < n; i++) {
                Action join = joined.get(i);
                deliverAction(result, from, join, exception);
            }
        }
    }

    private void resumeAction(Action action) {
        Bitmap bitmap = null;
        if (shouldReadFromMemoryCache(action.memoryPolicy)) {
            bitmap = quickMemoryCacheCheck(action.getKey());
        }

        if (bitmap != null) {
            deliverAction(bitmap, MEMORY, action, null);
        } else {
            enqueueAndSubmit(action);
        }
    }

    private void deliverAction(Bitmap result, LoadedFrom from, Action action, Exception e) {
        if (action.isCancelled()) {
            return;
        }
        if (!action.willReplay()) {
            targetToAction.remove(action.getTarget());
        }
        if (result != null) {
            if (from == null) {
                throw new AssertionError("LoadedFrom cannot be null.");
            }
            action.complete(result, from);
        } else {
            action.error(e);
        }
    }

    private void cancelExistingRequest(Object target) {
        checkMain();
        Action action = targetToAction.remove(target);
        if (action != null) {
            action.cancel();
            dispatcher.dispatchCancel(action);
        }
        if (target instanceof ImageView) {
            ImageView targetImageView = (ImageView) target;
            DeferredRequestCreator deferredRequestCreator =
                    targetToDeferredRequestCreator.remove(targetImageView);
            if (deferredRequestCreator != null) {
                deferredRequestCreator.cancel();
            }
        }
    }

    private static class CleanupThread extends Thread {
        private final ReferenceQueue<Object> referenceQueue;
        private final Handler handler;

        CleanupThread(ReferenceQueue<Object> referenceQueue) {
            this.referenceQueue = referenceQueue;
            this.handler = Angelo.HANDLER;
            setDaemon(true);
            setName(THREAD_PREFIX + "refQueue");
        }

        @Override
        public void run() {
            Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);
            while (true) {
                try {
                    RequestWeakReference<?> remove = (RequestWeakReference<?>) referenceQueue.remove(THREAD_LEAK_CLEANING_MS);
                    Message message = handler.obtainMessage();
                    if (remove != null) {
                        message.what = REQUEST_GCED;
                        message.obj = remove.action;
                        handler.sendMessage(message);
                    } else {
                        message.recycle();
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (final Exception e) {
                    handler.post(() -> {
                        throw new RuntimeException(e);
                    });
                    break;
                }
            }
        }
    }

    public static Angelo with(Context context) {
        if (singleton == null) {
            synchronized (Angelo.class) {
                if (singleton == null) {
                    if (context == null) {
                        throw new IllegalStateException("context == null");
                    }
                    singleton = new Builder(context).build();
                }
            }
        }
        return singleton;
    }

    public static class Builder {
        private final Context context;
        private Downloader downloader;
        private ExecutorService service;
        private Cache cache;
        private RequestTransformer transformer;

        public Builder(Context context) {
            if (context == null) {
                throw new IllegalArgumentException("Context must not be null.");
            }
            this.context = context.getApplicationContext();
        }

        public Angelo build() {
            Context context = this.context;

            if (downloader == null) {
                downloader = new OkHttp3Downloader(context);
            }
            if (cache == null) {
                cache = new LruCache(context);
            }
            if (service == null) {
                service = new AngeloExecutorService();
            }
            if (transformer == null) {
                transformer = RequestTransformer.IDENTITY;
            }

            Dispatcher dispatcher = new Dispatcher(context, service, downloader, cache);

            return new Angelo(context, dispatcher, cache, transformer);
        }
    }

    public enum LoadedFrom {
        MEMORY(),
        DISK(),
        NETWORK();

        LoadedFrom() {
        }
    }
}
