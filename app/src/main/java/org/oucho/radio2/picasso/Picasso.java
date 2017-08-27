package org.oucho.radio2.picasso;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
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
import static org.oucho.radio2.picasso.Action.RequestWeakReference;
import static org.oucho.radio2.picasso.Dispatcher.HUNTER_BATCH_COMPLETE;
import static org.oucho.radio2.picasso.Dispatcher.REQUEST_BATCH_RESUME;
import static org.oucho.radio2.picasso.Dispatcher.REQUEST_GCED;
import static org.oucho.radio2.picasso.MemoryPolicy.shouldReadFromMemoryCache;
import static org.oucho.radio2.picasso.Picasso.LoadedFrom.MEMORY;
import static org.oucho.radio2.picasso.Utils.OWNER_MAIN;
import static org.oucho.radio2.picasso.Utils.THREAD_LEAK_CLEANING_MS;
import static org.oucho.radio2.picasso.Utils.THREAD_PREFIX;
import static org.oucho.radio2.picasso.Utils.VERB_CANCELED;
import static org.oucho.radio2.picasso.Utils.VERB_COMPLETED;
import static org.oucho.radio2.picasso.Utils.VERB_ERRORED;
import static org.oucho.radio2.picasso.Utils.VERB_RESUMED;
import static org.oucho.radio2.picasso.Utils.checkMain;
import static org.oucho.radio2.picasso.Utils.log;


public class Picasso {

  /** Callbacks for Picasso events. */
  public interface Listener {
    /**
     * Invoked when an image has failed to load. This is useful for reporting image failures to a
     * remote analytics service, for example.
     */
    void onImageLoadFailed(Picasso picasso, Uri uri, Exception exception);
  }

  /**
   * A transformer that is called immediately before every request is submitted. This can be used to
   * modify any information about a request.
   * <p>
   * For example, if you use a CDN you can change the hostname for the image based on the current
   * location of the user in order to get faster download speeds.
   * <p>
   * <b>NOTE:</b> This is a beta feature. The API is subject to change in a backwards incompatible
   * way at any time.
   */
  public interface RequestTransformer {
    /**
     * Transform a request before it is submitted to be processed.
     *
     * @return The original request or a new request to replace it. Must not be null.
     */
    Request transformRequest(Request request);

    /** A {@link RequestTransformer} which returns the original request. */
    RequestTransformer IDENTITY = request -> request;
  }

  /**
   * The priority of a request.
   *
   * @see RequestCreator#priority(Priority)
   */
  public enum Priority {
    LOW,
    NORMAL
  }

  static final String TAG = "Picasso";
  static final Handler HANDLER = new Handler(Looper.getMainLooper()) {
    @Override public void handleMessage(Message msg) {
      switch (msg.what) {
        case HUNTER_BATCH_COMPLETE: {
          @SuppressWarnings("unchecked") List<BitmapHunter> batch = (List<BitmapHunter>) msg.obj;
          //noinspection ForLoopReplaceableByForEach
          for (int i = 0, n = batch.size(); i < n; i++) {
            BitmapHunter hunter = batch.get(i);
            hunter.picasso.complete(hunter);
          }
          break;
        }
        case REQUEST_GCED: {
          Action action = (Action) msg.obj;
          if (action.getPicasso().loggingEnabled) {
            log(OWNER_MAIN, VERB_CANCELED, action.request.logId(), "target got garbage collected");
          }
          action.picasso.cancelExistingRequest(action.getTarget());
          break;
        }
        case REQUEST_BATCH_RESUME:
          @SuppressWarnings("unchecked") List<Action> batch = (List<Action>) msg.obj;
          //noinspection ForLoopReplaceableByForEach
          for (int i = 0, n = batch.size(); i < n; i++) {
            Action action = batch.get(i);
            action.picasso.resumeAction(action);
          }
          break;
        default:
          throw new AssertionError("Unknown handler message received: " + msg.what);
      }
    }
  };

  private static volatile Picasso singleton = null;

  private final Listener listener;
  private final RequestTransformer requestTransformer;
  private final List<RequestHandler> requestHandlers;

  final Context context;
  final Dispatcher dispatcher;
  final Cache cache;
  private final Map<Object, Action> targetToAction;
  private final Map<ImageView, DeferredRequestCreator> targetToDeferredRequestCreator;
  final ReferenceQueue<Object> referenceQueue;
  final Bitmap.Config defaultBitmapConfig;

  boolean indicatorsEnabled;
  volatile boolean loggingEnabled;

  boolean shutdown;

  private Picasso(Context context, Dispatcher dispatcher, Cache cache, Listener listener,
                  RequestTransformer requestTransformer, List<RequestHandler> extraRequestHandlers,
                  Bitmap.Config defaultBitmapConfig, boolean indicatorsEnabled, boolean loggingEnabled) {
    this.context = context;
    this.dispatcher = dispatcher;
    this.cache = cache;
    this.listener = listener;
    this.requestTransformer = requestTransformer;
    this.defaultBitmapConfig = defaultBitmapConfig;

    int builtInHandlers = 7; // Adjust this as internal handlers are added or removed.
    int extraCount = (extraRequestHandlers != null ? extraRequestHandlers.size() : 0);
    List<RequestHandler> allRequestHandlers = new ArrayList<>(builtInHandlers + extraCount);

    // ResourceRequestHandler needs to be the first in the list to avoid
    // forcing other RequestHandlers to perform null checks on request.uri
    // to cover the (request.resourceId != 0) case.
    allRequestHandlers.add(new ResourceRequestHandler(context));
    if (extraRequestHandlers != null) {
      allRequestHandlers.addAll(extraRequestHandlers);
    }
    allRequestHandlers.add(new ContactsPhotoRequestHandler(context));
    allRequestHandlers.add(new MediaStoreRequestHandler(context));
    allRequestHandlers.add(new ContentStreamRequestHandler(context));
    allRequestHandlers.add(new AssetRequestHandler(context));
    allRequestHandlers.add(new FileRequestHandler(context));
    allRequestHandlers.add(new NetworkRequestHandler(dispatcher.downloader));
    requestHandlers = Collections.unmodifiableList(allRequestHandlers);

    this.targetToAction = new WeakHashMap<>();
    this.targetToDeferredRequestCreator = new WeakHashMap<>();
    this.indicatorsEnabled = indicatorsEnabled;
    this.loggingEnabled = loggingEnabled;
    this.referenceQueue = new ReferenceQueue<>();
    CleanupThread cleanupThread = new CleanupThread(referenceQueue, HANDLER);
    cleanupThread.start();
  }

  /** Cancel any existing requests for the specified target {@link ImageView}. */
  public void cancelRequest(ImageView view) {
    // checkMain() is called from cancelExistingRequest()
    if (view == null) {
      throw new IllegalArgumentException("view cannot be null.");
    }
    cancelExistingRequest(view);
  }

  /** Cancel any existing requests for the specified {@link Target} instance. */
  public void cancelRequest(Target target) {
    // checkMain() is called from cancelExistingRequest()
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

  /** Toggle whether to display debug indicators on images. */
  @SuppressWarnings("UnusedDeclaration") public void setIndicatorsEnabled(boolean enabled) {
    indicatorsEnabled = enabled;
  }

  /** {@code true} if debug indicators should are displayed on images. */
  @SuppressWarnings("UnusedDeclaration") public boolean areIndicatorsEnabled() {
    return indicatorsEnabled;
  }

  /**
   * Toggle whether debug logging is enabled.
   * <p>
   * <b>WARNING:</b> Enabling this will result in excessive object allocation. This should be only
   * be used for debugging Picasso behavior. Do NOT pass {@code BuildConfig.DEBUG}.
   */
  @SuppressWarnings("UnusedDeclaration") // Public API.
  public void setLoggingEnabled(boolean enabled) {
    loggingEnabled = enabled;
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
    // If there is already a deferred request, cancel it.
    if (targetToDeferredRequestCreator.containsKey(view)) {
      cancelExistingRequest(view);
    }
    targetToDeferredRequestCreator.put(view, request);
  }

  void enqueueAndSubmit(Action action) {
    Object target = action.getTarget();
    if (target != null && targetToAction.get(target) != action) {
      // This will also check we are on the main thread.
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

    Uri uri = hunter.getData().uri;
    Exception exception = hunter.getException();
    Bitmap result = hunter.getResult();
    LoadedFrom from = hunter.getLoadedFrom();

    if (single != null) {
      deliverAction(result, from, single, exception);
    }

    if (hasMultiple) {
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0, n = joined.size(); i < n; i++) {
        Action join = joined.get(i);
        deliverAction(result, from, join, exception);
      }
    }

    if (listener != null && exception != null) {
      listener.onImageLoadFailed(this, uri, exception);
    }
  }

  private void resumeAction(Action action) {
    Bitmap bitmap = null;
    if (shouldReadFromMemoryCache(action.memoryPolicy)) {
      bitmap = quickMemoryCacheCheck(action.getKey());
    }

    if (bitmap != null) {
      // Resumed action is cached, complete immediately.
      deliverAction(bitmap, MEMORY, action, null);
      if (loggingEnabled) {
        log(OWNER_MAIN, VERB_COMPLETED, action.request.logId(), "from " + MEMORY);
      }
    } else {
      // Re-submit the action to the executor.
      enqueueAndSubmit(action);
      if (loggingEnabled) {
        log(OWNER_MAIN, VERB_RESUMED, action.request.logId());
      }
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
      if (loggingEnabled) {
        log(OWNER_MAIN, VERB_COMPLETED, action.request.logId(), "from " + from);
      }
    } else {
      action.error(e);
      if (loggingEnabled) {
        log(OWNER_MAIN, VERB_ERRORED, action.request.logId(), e.getMessage());
      }
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

  /**
   * When the target of an action is weakly reachable but the request hasn't been canceled, it
   * gets added to the reference queue. This thread empties the reference queue and cancels the
   * request.
   */
  private static class CleanupThread extends Thread {
    private final ReferenceQueue<Object> referenceQueue;
    private final Handler handler;

    CleanupThread(ReferenceQueue<Object> referenceQueue, Handler handler) {
      this.referenceQueue = referenceQueue;
      this.handler = handler;
      setDaemon(true);
      setName(THREAD_PREFIX + "refQueue");
    }

    @Override public void run() {
      Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);
      while (true) {
        try {
          // Prior to Android 5.0, even when there is no local variable, the result from
          // remove() & obtainMessage() is kept as a stack local variable.
          // We're forcing this reference to be cleared and replaced by looping every second
          // when there is nothing to do.
          // This behavior has been tested and reproduced with heap dumps.
          RequestWeakReference<?> remove =
              (RequestWeakReference<?>) referenceQueue.remove(THREAD_LEAK_CLEANING_MS);
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


  public static Picasso with(Context context) {
    if (singleton == null) {
      synchronized (Picasso.class) {
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

  /** Fluent API for creating {@link Picasso} instances. */
  @SuppressWarnings("UnusedDeclaration") // Public API.
  public static class Builder {
    private final Context context;
    private Downloader downloader;
    private ExecutorService service;
    private Cache cache;
    private Listener listener;
    private RequestTransformer transformer;
    private List<RequestHandler> requestHandlers;
    private Bitmap.Config defaultBitmapConfig;

    private boolean indicatorsEnabled;
    private boolean loggingEnabled;

    /** Start building a new {@link Picasso} instance. */
    public Builder(Context context) {
      if (context == null) {
        throw new IllegalArgumentException("Context must not be null.");
      }
      this.context = context.getApplicationContext();
    }

    /**
     * Specify the default {@link Bitmap.Config} used when decoding images. This can be overridden
     * on a per-request basis using {@link RequestCreator#config(Bitmap.Config) config(..)}.
     */
    public Builder defaultBitmapConfig(Bitmap.Config bitmapConfig) {
      if (bitmapConfig == null) {
        throw new IllegalArgumentException("Bitmap config must not be null.");
      }
      this.defaultBitmapConfig = bitmapConfig;
      return this;
    }

    /** Specify the {@link Downloader} that will be used for downloading images. */
    public Builder downloader(Downloader downloader) {
      if (downloader == null) {
        throw new IllegalArgumentException("Downloader must not be null.");
      }
      if (this.downloader != null) {
        throw new IllegalStateException("Downloader already set.");
      }
      this.downloader = downloader;
      return this;
    }


    public Builder executor(ExecutorService executorService) {
      if (executorService == null) {
        throw new IllegalArgumentException("Executor service must not be null.");
      }
      if (this.service != null) {
        throw new IllegalStateException("Executor service already set.");
      }
      this.service = executorService;
      return this;
    }

    /** Specify the memory cache used for the most recent images. */
    public Builder memoryCache(Cache memoryCache) {
      if (memoryCache == null) {
        throw new IllegalArgumentException("Memory cache must not be null.");
      }
      if (this.cache != null) {
        throw new IllegalStateException("Memory cache already set.");
      }
      this.cache = memoryCache;
      return this;
    }

    /** Specify a listener for interesting events. */
    public Builder listener(Listener listener) {
      if (listener == null) {
        throw new IllegalArgumentException("Listener must not be null.");
      }
      if (this.listener != null) {
        throw new IllegalStateException("Listener already set.");
      }
      this.listener = listener;
      return this;
    }

    /**
     * Specify a transformer for all incoming requests.
     * <p>
     * <b>NOTE:</b> This is a beta feature. The API is subject to change in a backwards incompatible
     * way at any time.
     */
    public Builder requestTransformer(RequestTransformer transformer) {
      if (transformer == null) {
        throw new IllegalArgumentException("Transformer must not be null.");
      }
      if (this.transformer != null) {
        throw new IllegalStateException("Transformer already set.");
      }
      this.transformer = transformer;
      return this;
    }

    /** Register a {@link RequestHandler}. */
    public Builder addRequestHandler(RequestHandler requestHandler) {
      if (requestHandler == null) {
        throw new IllegalArgumentException("RequestHandler must not be null.");
      }
      if (requestHandlers == null) {
        requestHandlers = new ArrayList<>();
      }
      if (requestHandlers.contains(requestHandler)) {
        throw new IllegalStateException("RequestHandler already registered.");
      }
      requestHandlers.add(requestHandler);
      return this;
    }

    /** Toggle whether to display debug indicators on images. */
    public Builder indicatorsEnabled(boolean enabled) {
      this.indicatorsEnabled = enabled;
      return this;
    }

    /**
     * Toggle whether debug logging is enabled.
     * <p>
     * <b>WARNING:</b> Enabling this will result in excessive object allocation. This should be only
     * be used for debugging purposes. Do NOT pass {@code BuildConfig.DEBUG}.
     */
    public Builder loggingEnabled(boolean enabled) {
      this.loggingEnabled = enabled;
      return this;
    }

    /** Create the {@link Picasso} instance. */
    public Picasso build() {
      Context context = this.context;

      if (downloader == null) {
        downloader = new OkHttp3Downloader(context);
      }
      if (cache == null) {
        cache = new LruCache(context);
      }
      if (service == null) {
        service = new PicassoExecutorService();
      }
      if (transformer == null) {
        transformer = RequestTransformer.IDENTITY;
      }


      Dispatcher dispatcher = new Dispatcher(context, service, HANDLER, downloader, cache);

      return new Picasso(context, dispatcher, cache, listener, transformer, requestHandlers, defaultBitmapConfig, indicatorsEnabled, loggingEnabled);
    }
  }

  /** Describes where the image was loaded from. */
  public enum LoadedFrom {
    MEMORY(Color.GREEN),
    DISK(Color.BLUE),
    NETWORK(Color.RED);

    final int debugColor;

    LoadedFrom(int debugColor) {
      this.debugColor = debugColor;
    }
  }
}
