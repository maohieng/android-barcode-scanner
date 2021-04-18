package github.jomutils.android.barcode.widget;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

import androidx.core.content.ContextCompat;

import github.jomutils.android.barcode.R;
import github.jomutils.android.barcode.camera.GraphicOverlay;
import github.jomutils.android.barcode.settings.PreferenceUtils;

public class BarcodeGraphicBase extends GraphicOverlay.Graphic {

    private final Paint boxPaint;
    private final Paint scrimPaint;
    private final Paint eraserPaint;

    private final float boxCornerRadius;
    private final Paint pathPaint;

    private final RectF boxRect;

    public BarcodeGraphicBase(GraphicOverlay overlay) {
        super(overlay);
        boxPaint = new Paint();
        boxPaint.setColor(ContextCompat.getColor(getContext(), R.color.barcode_reticle_stroke));
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(getContext().getResources().getDimensionPixelOffset(R.dimen.barcode_reticle_stroke_width));

        scrimPaint = new Paint();
        scrimPaint.setColor(ContextCompat.getColor(getContext(), R.color.barcode_reticle_background));

        eraserPaint = new Paint();
        eraserPaint.setStrokeWidth(boxPaint.getStrokeWidth());
        eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        boxCornerRadius = getContext().getResources().getDimensionPixelOffset(R.dimen.barcode_reticle_corner_radius);

        pathPaint = new Paint();
        pathPaint.setColor(Color.WHITE);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(boxPaint.getStrokeWidth());
        pathPaint.setPathEffect(new CornerPathEffect(boxCornerRadius));

        boxRect = PreferenceUtils.getBarcodeReticleBox(overlay);
    }

    @Override
    public void draw(Canvas canvas) {
        // Draws the dark background scrim and leaves the box area clear.
        canvas.drawRect(0f, 0f, (float) canvas.getWidth(), (float) canvas.getHeight(), scrimPaint);
        // As the stroke is always centered, so erase twice with FILL and STROKE respectively to clear
        // all area that the box rect would occupy.
        eraserPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(boxRect, boxCornerRadius, boxCornerRadius, eraserPaint);
        eraserPaint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(boxRect, boxCornerRadius, boxCornerRadius, eraserPaint);
        // Draws the box.
        canvas.drawRoundRect(boxRect, boxCornerRadius, boxCornerRadius, boxPaint);
    }

    public float getBoxCornerRadius() {
        return boxCornerRadius;
    }

    public Paint getPathPaint() {
        return pathPaint;
    }

    public RectF getBoxRect() {
        return boxRect;
    }
}
