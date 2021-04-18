package github.jomutils.android.barcode.widget;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;

import com.google.mlkit.vision.barcode.Barcode;

import github.jomutils.android.barcode.camera.GraphicOverlay;
import github.jomutils.android.barcode.settings.PreferenceUtils;

/**
 * Guides user to move camera closer to confirm the detected barcode.
 */
public class BarcodeConfirmingGraphic extends BarcodeGraphicBase {

    private final Barcode barcode;

    public BarcodeConfirmingGraphic(GraphicOverlay overlay, Barcode barcode) {
        super(overlay);
        this.barcode = barcode;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        final RectF boxRect = getBoxRect();
        // Draws a highlighted path to indicate the current progress to meet size requirement.
        float sizeProgress = PreferenceUtils.getProgressToMeetBarcodeSizeRequirement(getOverlay(), barcode);
        Path path = new Path();
        if (sizeProgress > 0.95f) {
            // To have a completed path with all corners rounded.
            path.moveTo(boxRect.left, boxRect.top);
            path.lineTo(boxRect.right, boxRect.top);
            path.lineTo(boxRect.right, boxRect.bottom);
            path.lineTo(boxRect.left, boxRect.bottom);
            path.close();
        } else {
            path.moveTo(boxRect.left, boxRect.top + boxRect.height() * sizeProgress);
            path.lineTo(boxRect.left, boxRect.top);
            path.lineTo(boxRect.left + boxRect.width() * sizeProgress, boxRect.top);

            path.moveTo(boxRect.right, boxRect.bottom - boxRect.height() * sizeProgress);
            path.lineTo(boxRect.right, boxRect.bottom);
            path.lineTo(boxRect.right - boxRect.width() * sizeProgress, boxRect.bottom);
        }

        canvas.drawPath(path, getPathPaint());
    }
}
