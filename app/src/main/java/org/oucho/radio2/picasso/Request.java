package org.oucho.radio2.picasso;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;

import org.oucho.radio2.picasso.Picasso.Priority;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.unmodifiableList;

public final class Request {
  private static final long TOO_LONG_LOG = TimeUnit.SECONDS.toNanos(5);

  int id;
  long started;
  int networkPolicy;


  public final Uri uri;

  public final int resourceId;

  public final String stableKey;

  public final List<Transformation> transformations;

  public final int targetWidth;

  public final int targetHeight;

  public final boolean centerCrop;

  public final int centerCropGravity;

  public final boolean centerInside;
  public final boolean onlyScaleDown;

  public final float rotationDegrees;

  public final float rotationPivotX;

  public final float rotationPivotY;

  public final boolean hasRotationPivot;

  public final boolean purgeable;

  public final Bitmap.Config config;

  public final Priority priority;

  private Request(Uri uri, int resourceId, String stableKey, List<Transformation> transformations,
      int targetWidth, int targetHeight, boolean centerCrop, boolean centerInside,
      int centerCropGravity, boolean onlyScaleDown, float rotationDegrees,
      float rotationPivotX, float rotationPivotY, boolean hasRotationPivot,
      boolean purgeable, Bitmap.Config config, Priority priority) {
    this.uri = uri;
    this.resourceId = resourceId;
    this.stableKey = stableKey;
    if (transformations == null) {
      this.transformations = null;
    } else {
      this.transformations = unmodifiableList(transformations);
    }
    this.targetWidth = targetWidth;
    this.targetHeight = targetHeight;
    this.centerCrop = centerCrop;
    this.centerInside = centerInside;
    this.centerCropGravity = centerCropGravity;
    this.onlyScaleDown = onlyScaleDown;
    this.rotationDegrees = rotationDegrees;
    this.rotationPivotX = rotationPivotX;
    this.rotationPivotY = rotationPivotY;
    this.hasRotationPivot = hasRotationPivot;
    this.purgeable = purgeable;
    this.config = config;
    this.priority = priority;
  }

  @Override public String toString() {
    final StringBuilder builder = new StringBuilder("Request{");
    if (resourceId > 0) {
      builder.append(resourceId);
    } else {
      builder.append(uri);
    }
    if (transformations != null && !transformations.isEmpty()) {
      for (Transformation transformation : transformations) {
        builder.append(' ').append(transformation.key());
      }
    }
    if (stableKey != null) {
      builder.append(" stableKey(").append(stableKey).append(')');
    }
    if (targetWidth > 0) {
      builder.append(" resize(").append(targetWidth).append(',').append(targetHeight).append(')');
    }
    if (centerCrop) {
      builder.append(" centerCrop");
    }
    if (centerInside) {
      builder.append(" centerInside");
    }
    if (rotationDegrees != 0) {
      builder.append(" rotation(").append(rotationDegrees);
      if (hasRotationPivot) {
        builder.append(" @ ").append(rotationPivotX).append(',').append(rotationPivotY);
      }
      builder.append(')');
    }
    if (purgeable) {
      builder.append(" purgeable");
    }
    if (config != null) {
      builder.append(' ').append(config);
    }
    builder.append('}');

    return builder.toString();
  }

  String logId() {
    long delta = System.nanoTime() - started;
    if (delta > TOO_LONG_LOG) {
      return plainId() + '+' + TimeUnit.NANOSECONDS.toSeconds(delta) + 's';
    }
    return plainId() + '+' + TimeUnit.NANOSECONDS.toMillis(delta) + "ms";
  }

  String plainId() {
    return "[R" + id + ']';
  }

  String getName() {
    if (uri != null) {
      return String.valueOf(uri.getPath());
    }
    return Integer.toHexString(resourceId);
  }

  public boolean hasSize() {
    return targetWidth != 0 || targetHeight != 0;
  }

  boolean needsTransformation() {
    return needsMatrixTransform() || hasCustomTransformations();
  }

  boolean needsMatrixTransform() {
    return hasSize() || rotationDegrees != 0;
  }

  boolean hasCustomTransformations() {
    return transformations != null;
  }

  /** Builder for creating {@link Request} instances. */
  public static final class Builder {
    private Uri uri;
    private int resourceId;
    private String stableKey;
    private int targetWidth;
    private int targetHeight;
    private boolean centerCrop;
    private int centerCropGravity;
    private boolean centerInside;
    private boolean onlyScaleDown;
    private float rotationDegrees;
    private float rotationPivotX;
    private float rotationPivotY;
    private boolean hasRotationPivot;
    private boolean purgeable;
    private List<Transformation> transformations;
    private Bitmap.Config config;
    private Priority priority;

    Builder(Uri uri, int resourceId, Bitmap.Config bitmapConfig) {
      this.uri = uri;
      this.resourceId = resourceId;
      this.config = bitmapConfig;
    }

    boolean hasImage() {
      return uri != null || resourceId != 0;
    }

    boolean hasSize() {
      return targetWidth != 0 || targetHeight != 0;
    }

    boolean hasPriority() {
      return priority != null;
    }

    /**
     * Set the stable key to be used instead of the URI or resource ID when caching.
     * Two requests with the same value are considered to be for the same resource.
     */
    public Builder stableKey(@Nullable String stableKey) {
      this.stableKey = stableKey;
      return this;
    }

    /**
     * Resize the image to the specified size in pixels.
     * Use 0 as desired dimension to resize keeping aspect ratio.
     */
    public Builder resize(@Px int targetWidth, @Px int targetHeight) {
      if (targetWidth < 0) {
        throw new IllegalArgumentException("Width must be positive number or 0.");
      }
      if (targetHeight < 0) {
        throw new IllegalArgumentException("Height must be positive number or 0.");
      }
      if (targetHeight == 0 && targetWidth == 0) {
        throw new IllegalArgumentException("At least one dimension has to be positive number.");
      }
      this.targetWidth = targetWidth;
      this.targetHeight = targetHeight;
      return this;
    }

    /**
     * Crops an image inside of the bounds specified by {@link #resize(int, int)} rather than
     * distorting the aspect ratio. This cropping technique scales the image so that it fills the
     * requested bounds, aligns it using provided gravity parameter and then crops the extra.
     */
    public Builder centerCrop(int alignGravity) {
      if (centerInside) {
        throw new IllegalStateException("Center crop can not be used after calling centerInside");
      }
      centerCrop = true;
      centerCropGravity = alignGravity;
      return this;
    }

    /**
     * Centers an image inside of the bounds specified by {@link #resize(int, int)}. This scales
     * the image so that both dimensions are equal to or less than the requested bounds.
     */
    public Builder centerInside() {
      if (centerCrop) {
        throw new IllegalStateException("Center inside can not be used after calling centerCrop");
      }
      centerInside = true;
      return this;
    }

    /**
     * Only resize an image if the original image size is bigger than the target size
     * specified by {@link #resize(int, int)}.
     */
    public Builder onlyScaleDown() {
      if (targetHeight == 0 && targetWidth == 0) {
        throw new IllegalStateException("onlyScaleDown can not be applied without resize");
      }
      onlyScaleDown = true;
      return this;
    }

    /** Rotate the image by the specified degrees. */
    public Builder rotate(float degrees) {
      rotationDegrees = degrees;
      return this;
    }

    /** Rotate the image by the specified degrees around a pivot point. */
    public Builder rotate(float degrees, float pivotX, float pivotY) {
      rotationDegrees = degrees;
      rotationPivotX = pivotX;
      rotationPivotY = pivotY;
      hasRotationPivot = true;
      return this;
    }

    public Builder purgeable() {
      purgeable = true;
      return this;
    }

    /** Decode the image using the specified config. */
    public Builder config(Bitmap.Config config) {
      if (config == null) {
        throw new IllegalArgumentException("config == null");
      }
      this.config = config;
      return this;
    }

    /** Execute request using the specified priority. */
    public Builder priority(Priority priority) {
      if (priority == null) {
        throw new IllegalArgumentException("Priority invalid.");
      }
      if (this.priority != null) {
        throw new IllegalStateException("Priority already set.");
      }
      this.priority = priority;
      return this;
    }

    /**
     * Add a custom transformation to be applied to the image.
     * <p>
     * Custom transformations will always be run after the built-in transformations.
     */
    public Builder transform(Transformation transformation) {
      if (transformation == null) {
        throw new IllegalArgumentException("Transformation must not be null.");
      }
      if (transformation.key() == null) {
        throw new IllegalArgumentException("Transformation key must not be null.");
      }
      if (transformations == null) {
        transformations = new ArrayList<>(2);
      }
      transformations.add(transformation);
      return this;
    }

    /**
     * Add a list of custom transformations to be applied to the image.
     * <p>
     * Custom transformations will always be run after the built-in transformations.
     */
    public Builder transform(List<? extends Transformation> transformations) {
      if (transformations == null) {
        throw new IllegalArgumentException("Transformation list must not be null.");
      }
      for (int i = 0, size = transformations.size(); i < size; i++) {
        transform(transformations.get(i));
      }
      return this;
    }

    /** Create the immutable {@link Request} object. */
    public Request build() {
      if (centerInside && centerCrop) {
        throw new IllegalStateException("Center crop and center inside can not be used together.");
      }
      if (centerCrop && (targetWidth == 0 && targetHeight == 0)) {
        throw new IllegalStateException(
            "Center crop requires calling resize with positive width and height.");
      }
      if (centerInside && (targetWidth == 0 && targetHeight == 0)) {
        throw new IllegalStateException(
            "Center inside requires calling resize with positive width and height.");
      }
      if (priority == null) {
        priority = Priority.NORMAL;
      }
      return new Request(uri, resourceId, stableKey, transformations, targetWidth, targetHeight,
          centerCrop, centerInside, centerCropGravity, onlyScaleDown, rotationDegrees,
          rotationPivotX, rotationPivotY, hasRotationPivot, purgeable, config, priority);
    }
  }
}
