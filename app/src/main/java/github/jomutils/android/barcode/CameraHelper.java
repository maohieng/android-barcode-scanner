package github.jomutils.android.barcode;

import androidx.camera.core.AspectRatio;

// Borrow from https://github.com/zendroidhd/camerax-barcode-scanner/blob/development/app/src/main/java/com/technology/zenlight/barcodescanner/utils/helpers/CameraHelper.kt
public final class CameraHelper {

    private CameraHelper() {
        //no instance
    }

    private static final double RATIO_4_3_VALUE = 4.0 / 3.0;
    private static final double RATIO_16_9_VALUE = 16.0 / 9.0;

    public static int getAspectRatio(int width, int height) {
        double previewRatio = ((double) Math.max(width, height)) / ((double) Math.min(width, height));
        if (Math.abs(previewRatio - RATIO_4_3_VALUE) <= Math.abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }
}
