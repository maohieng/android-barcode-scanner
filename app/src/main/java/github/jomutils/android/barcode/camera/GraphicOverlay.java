package github.jomutils.android.barcode.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.SizeF;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A view which renders a series of custom graphics to be overlaid on top of an associated preview
 * (i.e., the camera preview). The creator can add graphics objects, update the objects, and remove
 * them, triggering the appropriate drawing and invalidation within the view.
 * <p>
 * <p>
 * Supports scaling and mirroring of the graphics relative the camera's preview properties. The
 * idea is that detection items are expressed in terms of a preview size, but need to be scaled up
 * to the full view size, and also mirrored in the case of the front-facing camera.
 * <p>
 * <p>
 * Associated [Graphic] items should use [.translateX] and [ ][.translateY] to convert to view coordinate from the preview's coordinate.
 */
public class GraphicOverlay extends View {
    private final Object lock = new Object();
    private final List<Graphic> graphics = new ArrayList<>();

    private int previewWidth;
    private int previewHeight;
    // The factor of overlay View size to image size. Anything in the image coordinates need to be
    // scaled by this amount to fit with the area of overlay View.
    private float widthScaleFactor = 1.0f;
    private float heightScaleFactor = 1.0f;

    /**
     * Base class for a custom graphics object to be rendered within the graphic overlay. Subclass
     * this and implement the {@link Graphic#draw(Canvas)} method to define the graphics element. Add
     * instances to the overlay using {@link GraphicOverlay#add(Graphic)}.
     */
    public abstract static class Graphic {
        private final GraphicOverlay overlay;

        public Graphic(GraphicOverlay overlay) {
            this.overlay = overlay;
        }

        /**
         * Draws the graphic on the supplied canvas.
         */
        public abstract void draw(Canvas canvas);

        /**
         * Returns the application context of the app.
         */
        public Context getContext() {
            return overlay.getContext();
        }

        public GraphicOverlay getOverlay() {
            return overlay;
        }
    }

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Removes all graphics from the overlay.
     */
    public void clear() {
        synchronized (lock) {
            graphics.clear();
        }
        postInvalidate();
    }

    /**
     * Adds a graphic to the overlay.
     */
    public void add(Graphic graphic) {
        synchronized (lock) {
            graphics.add(graphic);
        }
    }

    public static void checkState(boolean expression, @Nullable String message) {
        if (!expression) {
            throw new IllegalStateException(message);
        }
    }

    public boolean isPortraitMode() {
        return getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    /**
     * Sets the source information of the image being processed by detectors, including size and
     * whether it is flipped, which informs how to transform image coordinates later.
     *
     * @param imageWidth the width of the image sent to ML Kit detectors
     * @param imageHeight the height of the image sent to ML Kit detectors
     * @param isFlipped whether the image is flipped. Should set it to true when the image is from the
     *     front camera.
     */
//    public void setImageSourceInfo(int imageWidth, int imageHeight, boolean isFlipped) {
//        checkState(imageWidth > 0, "image width must be positive");
//        checkState(imageHeight > 0, "image height must be positive");
//        synchronized (lock) {
//            this.previewWidth = imageWidth;
//            this.previewHeight = imageHeight;
//            this.isImageFlipped = isFlipped;
//            needUpdateTransformation = true;
//        }
//        postInvalidate();
//    }

    /**
     * Sets the camera attributes for size and facing direction, which informs how to transform image
     * coordinates later.
     */
    public void setPreviewSize(@NonNull SizeF previewSize) {
        if (isPortraitMode()) {
            // Swap width and height when in portrait, since camera's natural orientation is landscape.
            previewWidth = (int) (previewSize.getHeight() / heightScaleFactor);
            previewHeight = (int) (previewSize.getWidth() / widthScaleFactor);
        } else {
            previewWidth = (int) (previewSize.getWidth() / widthScaleFactor);
            previewHeight = (int) (previewSize.getHeight() / heightScaleFactor);
        }
    }
//
//    private void updateTransformationIfNeeded() {
//        if (!needUpdateTransformation || previewWidth <= 0 || previewHeight <= 0) {
//            return;
//        }
//        float viewAspectRatio = (float) getWidth() / getHeight();
//        float imageAspectRatio = (float)  previewWidth /  previewHeight;
//        postScaleWidthOffset = 0;
//        postScaleHeightOffset = 0;
//        if (viewAspectRatio > imageAspectRatio) {
//            // The image needs to be vertically cropped to be displayed in this view.
//            widthScaleFactor = (float) getWidth() /  previewWidth;
//            postScaleHeightOffset = ((float) getWidth() / imageAspectRatio - getHeight()) / 2;
//        } else {
//            // The image needs to be horizontally cropped to be displayed in this view.
//            heightScaleFactor = (float) getHeight() /  previewHeight;
//            postScaleWidthOffset = ((float) getHeight() * imageAspectRatio - getWidth()) / 2;
//        }
//
//        transformationMatrix.reset();
//        transformationMatrix.setScale(widthScaleFactor, heightScaleFactor);
//        transformationMatrix.postTranslate(-postScaleWidthOffset, -postScaleHeightOffset);
//
//        if (isImageFlipped) {
//            transformationMatrix.postScale(-1f, 1f, getWidth() / 2f, getHeight() / 2f);
//        }
//
//        needUpdateTransformation = false;
//    }

    /**
     * Adjusts the `rect`'s coordinate from the preview's coordinate system to the view
     * coordinate system.
     */
    public RectF translateRect(Rect rect) {
        return new RectF(
                translateX((float) rect.left),
                translateY((float) rect.top),
                translateX((float) rect.right),
                translateY((float) rect.bottom)
        );
    }

    /**
     * Adjusts the supplied value from the image scale to the view scale.
     */
    public float scaleX(float x) {
        return x * widthScaleFactor;
    }

    /** Adjusts the supplied value from the image scale to the view scale. */
//    public float scaleY(float y) {
//        return y * heightScaleFactor;
//    }

    /**
     * Adjusts the x coordinate from the image's coordinate system to the view coordinate system.
     */
    public float translateX(float x) {
//        if (isImageFlipped) {
//            return getWidth() - (scaleX(x) - postScaleWidthOffset);
//        } else {
//            return scaleX(x) - postScaleWidthOffset;
//        }
//
//
        return x * widthScaleFactor;
    }

    /**
     * Adjusts the y coordinate from the image's coordinate system to the view coordinate system.
     */
    public float translateY(float y) {
//        return scaleY(y) - postScaleHeightOffset;
        return y * heightScaleFactor;
    }

    /**
     * Draws the overlay with its associated graphic objects.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (previewWidth > 0 && previewHeight > 0) {
            widthScaleFactor = ((float) getWidth()) / previewWidth;
            heightScaleFactor = ((float) getHeight()) / previewHeight;
        }

        synchronized (lock) {
//            updateTransformationIfNeeded();
            for (Graphic graphic : graphics) {
                graphic.draw(canvas);
            }
        }
    }

}