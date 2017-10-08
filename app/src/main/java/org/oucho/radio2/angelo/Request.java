package org.oucho.radio2.angelo;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.annotation.Px;

import org.oucho.radio2.angelo.Angelo.Priority;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public final class Request {

    public final Uri uri;

    final int resourceId;

    final String stableKey;

    final List<Transformation> transformations;

    final int targetWidth;

    final int targetHeight;

    final boolean centerCrop;

    final int centerCropGravity;

    final boolean centerInside;
    final boolean onlyScaleDown;

    final float rotationDegrees;

    final float rotationPivotX;

    final float rotationPivotY;

    final boolean hasRotationPivot;

    final boolean purgeable;

    final Bitmap.Config config;

    final Priority priority;

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

    @Override
    public String toString() {
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

    String getName() {
        if (uri != null) {
            return String.valueOf(uri.getPath());
        }
        return Integer.toHexString(resourceId);
    }

    boolean hasSize() {
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
        private final Uri uri;
        private final int resourceId;
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

        Builder(Uri uri, int resourceId) {
            this.uri = uri;
            this.resourceId = resourceId;
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

        void stableKey(@Nullable String stableKey) {
            this.stableKey = stableKey;
        }

        void resize(@Px int targetWidth, @Px int targetHeight) {
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
        }

        void centerCrop(int alignGravity) {
            if (centerInside) {
                throw new IllegalStateException("Center crop can not be used after calling centerInside");
            }
            centerCrop = true;
            centerCropGravity = alignGravity;
        }

        void centerInside() {
            if (centerCrop) {
                throw new IllegalStateException("Center inside can not be used after calling centerCrop");
            }
            centerInside = true;
        }

        void onlyScaleDown() {
            if (targetHeight == 0 && targetWidth == 0) {
                throw new IllegalStateException("onlyScaleDown can not be applied without resize");
            }
            onlyScaleDown = true;
        }

        /** Rotate the image by the specified degrees. */
        void rotate(float degrees) {
            rotationDegrees = degrees;
        }

        /** Rotate the image by the specified degrees around a pivot point. */
        void rotate(float degrees, float pivotX, float pivotY) {
            rotationDegrees = degrees;
            rotationPivotX = pivotX;
            rotationPivotY = pivotY;
            hasRotationPivot = true;
        }

        void purgeable() {
            purgeable = true;
        }

        void config(Bitmap.Config config) {
            if (config == null) {
                throw new IllegalArgumentException("config == null");
            }
            this.config = config;
        }

        void priority(Priority priority) {
            if (priority == null) {
                throw new IllegalArgumentException("Priority invalid.");
            }
            if (this.priority != null) {
                throw new IllegalStateException("Priority already set.");
            }
            this.priority = priority;
        }


        void transform(Transformation transformation) {
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
        }

        /**
         * Add a list of custom transformations to be applied to the image.
         * <p>
         * Custom transformations will always be run after the built-in transformations.
         */
        void transform(List<? extends Transformation> transformations) {
            if (transformations == null) {
                throw new IllegalArgumentException("Transformation list must not be null.");
            }
            for (int i = 0, size = transformations.size(); i < size; i++) {
                transform(transformations.get(i));
            }
        }

        /** Create the immutable {@link Request} object. */
        public Request build() {
            if (centerInside && centerCrop) {
                throw new IllegalStateException("Center crop and center inside can not be used together.");
            }
            if (centerCrop && (targetWidth == 0 && targetHeight == 0)) {
                throw new IllegalStateException("Center crop requires calling resize with positive width and height.");
            }
            if (centerInside && (targetWidth == 0 && targetHeight == 0)) {
                throw new IllegalStateException("Center inside requires calling resize with positive width and height.");
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
