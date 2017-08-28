package org.oucho.radio2.angelo;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;
import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static org.oucho.radio2.angelo.BitmapHunter.forRequest;
import static org.oucho.radio2.angelo.MemoryPolicy.shouldWriteToMemoryCache;
import static org.oucho.radio2.angelo.Utils.getService;
import static org.oucho.radio2.angelo.Utils.hasPermission;

class Dispatcher {
    private static final int RETRY_DELAY = 500;

    private static final int REQUEST_SUBMIT = 1;
    private static final int REQUEST_CANCEL = 2;
    static final int REQUEST_GCED = 3;
    private static final int HUNTER_COMPLETE = 4;
    private static final int HUNTER_RETRY = 5;
    private static final int HUNTER_DECODE_FAILED = 6;
    private static final int HUNTER_DELAY_NEXT_BATCH = 7;
    static final int HUNTER_BATCH_COMPLETE = 8;
    private static final int NETWORK_STATE_CHANGE = 9;
    private static final int TAG_PAUSE = 11;
    private static final int TAG_RESUME = 12;
    static final int REQUEST_BATCH_RESUME = 13;

    private static final String DISPATCHER_THREAD_NAME = "Dispatcher";
    private static final int BATCH_DELAY = 200; // ms

    private final Context context;
    private final ExecutorService service;
    final Downloader downloader;
    private final Map<String, BitmapHunter> hunterMap;
    private final Map<Object, Action> failedActions;
    private final Map<Object, Action> pausedActions;
    private final Set<Object> pausedTags;
    private final Handler handler;
    private final Handler mainThreadHandler;
    private final Cache cache;
    private final List<BitmapHunter> batch;
    private final boolean scansNetworkChanges;

    Dispatcher(Context context, ExecutorService service, Handler mainThreadHandler, Downloader downloader, Cache cache) {
        DispatcherThread dispatcherThread = new DispatcherThread();
        dispatcherThread.start();
        Utils.flushStackLocalLeaks(dispatcherThread.getLooper());
        this.context = context;
        this.service = service;
        this.hunterMap = new LinkedHashMap<>();
        this.failedActions = new WeakHashMap<>();
        this.pausedActions = new WeakHashMap<>();
        this.pausedTags = new HashSet<>();
        this.handler = new DispatcherHandler(dispatcherThread.getLooper(), this);
        this.downloader = downloader;
        this.mainThreadHandler = mainThreadHandler;
        this.cache = cache;
        this.batch = new ArrayList<>(4);
        this.scansNetworkChanges = hasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE);
        NetworkBroadcastReceiver receiver = new NetworkBroadcastReceiver(this);
        receiver.register();
    }

    void dispatchSubmit(Action action) {
        handler.sendMessage(handler.obtainMessage(REQUEST_SUBMIT, action));
    }

    void dispatchCancel(Action action) {
        handler.sendMessage(handler.obtainMessage(REQUEST_CANCEL, action));
    }

    void dispatchComplete(BitmapHunter hunter) {
        handler.sendMessage(handler.obtainMessage(HUNTER_COMPLETE, hunter));
    }

    void dispatchRetry(BitmapHunter hunter) {
        handler.sendMessageDelayed(handler.obtainMessage(HUNTER_RETRY, hunter), RETRY_DELAY);
    }

    void dispatchFailed(BitmapHunter hunter) {
        handler.sendMessage(handler.obtainMessage(HUNTER_DECODE_FAILED, hunter));
    }

    private void dispatchNetworkStateChange(NetworkInfo info) {
        handler.sendMessage(handler.obtainMessage(NETWORK_STATE_CHANGE, info));
    }

    private void performSubmit(Action action) {
        performSubmit(action, true);
    }

    private void performSubmit(Action action, boolean dismissFailed) {
        if (pausedTags.contains(action.getTag())) {
            pausedActions.put(action.getTarget(), action);
            return;
        }

        BitmapHunter hunter = hunterMap.get(action.getKey());
        if (hunter != null) {
            hunter.attach(action);
            return;
        }

        if (service.isShutdown()) {
            return;
        }

        hunter = forRequest(action.getAngelo(), this, cache, action);
        hunter.future = service.submit(hunter);
        hunterMap.put(action.getKey(), hunter);
        if (dismissFailed) {
            failedActions.remove(action.getTarget());
        }
    }

    private void performCancel(Action action) {
        String key = action.getKey();
        BitmapHunter hunter = hunterMap.get(key);
        if (hunter != null) {
            hunter.detach(action);
            if (hunter.cancel()) {
                hunterMap.remove(key);
            }
        }

        if (pausedTags.contains(action.getTag())) {
            pausedActions.remove(action.getTarget());
        }
    }


    private void performPauseTag(Object tag) {
        // Trying to pause a tag that is already paused.
        if (!pausedTags.add(tag)) {
            return;
        }

        // Go through all active hunters and detach/pause the requests
        // that have the paused tag.
        for (Iterator<BitmapHunter> it = hunterMap.values().iterator(); it.hasNext();) {
            BitmapHunter hunter = it.next();

            Action single = hunter.getAction();
            List<Action> joined = hunter.getActions();
            boolean hasMultiple = joined != null && !joined.isEmpty();

            // Hunter has no requests, bail early.
            if (single == null && !hasMultiple) {
                continue;
            }

            if (single != null && single.getTag().equals(tag)) {
                hunter.detach(single);
                pausedActions.put(single.getTarget(), single);
            }

            if (hasMultiple) {
                for (int i = joined.size() - 1; i >= 0; i--) {
                    Action action = joined.get(i);
                    if (!action.getTag().equals(tag)) {
                        continue;
                    }
                    hunter.detach(action);
                    pausedActions.put(action.getTarget(), action);
                }
            }

            // Check if the hunter can be cancelled in case all its requests
            // had the tag being paused here.
            if (hunter.cancel()) {
                it.remove();
            }
        }
    }

    private void performResumeTag(Object tag) {
        // Trying to resume a tag that is not paused.
        if (!pausedTags.remove(tag)) {
            return;
        }

        List<Action> batch = null;
        for (Iterator<Action> i = pausedActions.values().iterator(); i.hasNext();) {
            Action action = i.next();
            if (action.getTag().equals(tag)) {
                if (batch == null) {
                    batch = new ArrayList<>();
                }
                batch.add(action);
                i.remove();
            }
        }

        if (batch != null) {
            mainThreadHandler.sendMessage(mainThreadHandler.obtainMessage(REQUEST_BATCH_RESUME, batch));
        }
    }

    private void performRetry(BitmapHunter hunter) {
        if (hunter.isCancelled()) return;

        if (service.isShutdown()) {
            performError(hunter);
            return;
        }

        NetworkInfo networkInfo = null;
        if (scansNetworkChanges) {
            ConnectivityManager connectivityManager = getService(context, CONNECTIVITY_SERVICE);
            networkInfo = connectivityManager.getActiveNetworkInfo();
        }

        if (hunter.shouldRetry(networkInfo)) {

            if (hunter.getException() instanceof NetworkRequestHandler.ContentLengthException) {
                hunter.networkPolicy |= NetworkPolicy.NO_CACHE.index;
            }
            hunter.future = service.submit(hunter);
        } else {
            // Mark for replay only if we observe network info changes and support replay.
            boolean willReplay = scansNetworkChanges && hunter.supportsReplay();
            performError(hunter);
            if (willReplay) {
                markForReplay(hunter);
            }
        }
    }

    private void performComplete(BitmapHunter hunter) {
        if (shouldWriteToMemoryCache(hunter.getMemoryPolicy())) {
            cache.set(hunter.getKey(), hunter.getResult());
        }
        hunterMap.remove(hunter.getKey());
        batch(hunter);
    }

    private void performBatchComplete() {
        List<BitmapHunter> copy = new ArrayList<>(batch);
        batch.clear();
        mainThreadHandler.sendMessage(mainThreadHandler.obtainMessage(HUNTER_BATCH_COMPLETE, copy));
    }

    private void performError(BitmapHunter hunter) {
        hunterMap.remove(hunter.getKey());
        batch(hunter);
    }


    private void performNetworkStateChange(NetworkInfo info) {
        if (service instanceof AngeloExecutorService) {
            ((AngeloExecutorService) service).adjustThreadCount(info);
        }
        // Intentionally check only if isConnected() here before we flush out failed actions.
        if (info != null && info.isConnected()) {
            flushFailedActions();
        }
    }

    private void flushFailedActions() {
        if (!failedActions.isEmpty()) {
            Iterator<Action> iterator = failedActions.values().iterator();
            while (iterator.hasNext()) {
                Action action = iterator.next();
                iterator.remove();
                performSubmit(action, false);
            }
        }
    }

    private void markForReplay(BitmapHunter hunter) {
        Action action = hunter.getAction();
        if (action != null) {
            markForReplay(action);
        }
        List<Action> joined = hunter.getActions();
        if (joined != null) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, n = joined.size(); i < n; i++) {
                Action join = joined.get(i);
                markForReplay(join);
            }
        }
    }

    private void markForReplay(Action action) {
        Object target = action.getTarget();
        if (target != null) {
            action.willReplay = true;
            failedActions.put(target, action);
        }
    }

    private void batch(BitmapHunter hunter) {
        if (hunter.isCancelled()) {
            return;
        }
        if (hunter.result != null) {
            hunter.result.prepareToDraw();
        }
        batch.add(hunter);
        if (!handler.hasMessages(HUNTER_DELAY_NEXT_BATCH)) {
            handler.sendEmptyMessageDelayed(HUNTER_DELAY_NEXT_BATCH, BATCH_DELAY);
        }
    }


    private static class DispatcherHandler extends Handler {
        private final Dispatcher dispatcher;

        DispatcherHandler(Looper looper, Dispatcher dispatcher) {
            super(looper);
            this.dispatcher = dispatcher;
        }

        @Override public void handleMessage(final Message msg) {
            switch (msg.what) {
                case REQUEST_SUBMIT: {
                    Action action = (Action) msg.obj;
                    dispatcher.performSubmit(action);
                    break;
                }
                case REQUEST_CANCEL: {
                    Action action = (Action) msg.obj;
                    dispatcher.performCancel(action);
                    break;
                }
                case TAG_PAUSE: {
                    Object tag = msg.obj;
                    dispatcher.performPauseTag(tag);
                    break;
                }
                case TAG_RESUME: {
                    Object tag = msg.obj;
                    dispatcher.performResumeTag(tag);
                    break;
                }
                case HUNTER_COMPLETE: {
                    BitmapHunter hunter = (BitmapHunter) msg.obj;
                    dispatcher.performComplete(hunter);
                    break;
                }
                case HUNTER_RETRY: {
                    BitmapHunter hunter = (BitmapHunter) msg.obj;
                    dispatcher.performRetry(hunter);
                    break;
                }
                case HUNTER_DECODE_FAILED: {
                    BitmapHunter hunter = (BitmapHunter) msg.obj;
                    dispatcher.performError(hunter);
                    break;
                }
                case HUNTER_DELAY_NEXT_BATCH: {
                    dispatcher.performBatchComplete();
                    break;
                }
                case NETWORK_STATE_CHANGE: {
                    NetworkInfo info = (NetworkInfo) msg.obj;
                    dispatcher.performNetworkStateChange(info);
                    break;
                }
                default:
                    Angelo.HANDLER.post(() -> {
                        throw new AssertionError("Unknown handler message received: " + msg.what);
                    });
            }
        }
    }

    static class DispatcherThread extends HandlerThread {
        DispatcherThread() {
            super(Utils.THREAD_PREFIX + DISPATCHER_THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
        }
    }

    static class NetworkBroadcastReceiver extends BroadcastReceiver {

        private final Dispatcher dispatcher;

        NetworkBroadcastReceiver(Dispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        void register() {
            IntentFilter filter = new IntentFilter();

            if (dispatcher.scansNetworkChanges) {
                filter.addAction(CONNECTIVITY_ACTION);
            }
            dispatcher.context.registerReceiver(this, filter);
        }

        @Override public void onReceive(Context context, Intent intent) {
            // On some versions of Android this may be called with a null Intent,
            // also without extras (getExtras() == null), in such case we use defaults.
            if (intent == null) {
                return;
            }
            final String action = intent.getAction();

            if (CONNECTIVITY_ACTION.equals(action)) {
                ConnectivityManager connectivityManager = getService(context, CONNECTIVITY_SERVICE);
                dispatcher.dispatchNetworkStateChange(connectivityManager.getActiveNetworkInfo());
            }
        }
    }
}
