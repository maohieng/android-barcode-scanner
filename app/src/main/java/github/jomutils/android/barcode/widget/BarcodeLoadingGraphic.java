package github.jomutils.android.barcode.widget;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;

import github.jomutils.android.barcode.camera.GraphicOverlay;

/**
 * Draws the graphic to indicate the barcode result is in loading.
 */
public class BarcodeLoadingGraphic extends BarcodeGraphicBase {

    private final ValueAnimator loadingAnimator;

    private final PointF[] boxClockwiseCoordinates;

    private final Point[] coordinateOffsetBits;

    private final PointF lastPathPoint;

    public BarcodeLoadingGraphic(GraphicOverlay overlay, ValueAnimator loadingAnimator) {
        super(overlay);
        this.loadingAnimator = loadingAnimator;

        final RectF boxRect = getBoxRect();
        boxClockwiseCoordinates = new PointF[]{
                new PointF(boxRect.left, boxRect.top),
                new PointF(boxRect.right, boxRect.top),
                new PointF(boxRect.right, boxRect.bottom),
                new PointF(boxRect.left, boxRect.bottom)
        };

        coordinateOffsetBits = new Point[]{
                new Point(1, 0),
                new Point(0, 1),
                new Point(-1, 0),
                new Point(0, -1)
        };

        lastPathPoint = new PointF();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        final RectF boxRect = getBoxRect();
        float boxPerimeter = (boxRect.width() + boxRect.height()) * 2;
        Path path = new Path();

        // The distance between the box's left-top corner and the starting point of white colored path.
        float offsetLen = boxPerimeter * ((Float) loadingAnimator.getAnimatedValue()) % boxPerimeter;
        int i = 0;
        while (i < 4) {
            float edgeLen = (i % 2 == 0) ? boxRect.width() : boxRect.height();
            if (offsetLen <= edgeLen) {
                lastPathPoint.x = boxClockwiseCoordinates[i].x + coordinateOffsetBits[i].x * offsetLen;
                lastPathPoint.y = boxClockwiseCoordinates[i].y + coordinateOffsetBits[i].y * offsetLen;
                path.moveTo(lastPathPoint.x, lastPathPoint.y);
                break;
            }

            offsetLen -= edgeLen;
            i++;
        }

        // Computes the path based on the determined starting point and path length.
        float pathLen = boxPerimeter * 0.3f;
        for (int j = 0; j < 4; j++) {
            int index = (i + j) % 4;
            int nextIndex = (i + j + 1) % 4;
            // The length between path's current end point and reticle box's next coordinate point.
            float lineLen = Math.abs(boxClockwiseCoordinates[nextIndex].x - lastPathPoint.x) +
                    Math.abs(boxClockwiseCoordinates[nextIndex].y - lastPathPoint.y);
            if (lineLen >= pathLen) {
                path.lineTo(
                        lastPathPoint.x + pathLen * coordinateOffsetBits[index].x,
                        lastPathPoint.y + pathLen * coordinateOffsetBits[index].y
                );
                break;
            }

            lastPathPoint.x = boxClockwiseCoordinates[nextIndex].x;
            lastPathPoint.y = boxClockwiseCoordinates[nextIndex].y;
            path.lineTo(lastPathPoint.x, lastPathPoint.y);
            pathLen -= lineLen;
        }

        canvas.drawPath(path, getPathPaint());
    }
}
