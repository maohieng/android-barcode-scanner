package github.jomutils.android.barcode.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.preference.PreferenceManager;

import androidx.annotation.StringRes;

import com.google.mlkit.vision.barcode.Barcode;

import github.jomutils.android.barcode.R;
import github.jomutils.android.barcode.camera.GraphicOverlay;

public final class PreferenceUtils {
    private PreferenceUtils() {
        //no instance
    }

    private static boolean getBooleanPref(Context context, @StringRes int prefKeyId, boolean defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(prefKeyId), defaultValue);
    }

    private static int getIntPref(Context context, @StringRes int prefKeyId, int defaultValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = context.getString(prefKeyId);
        return sharedPreferences.getInt(prefKey, defaultValue);
    }

    public static float getProgressToMeetBarcodeSizeRequirement(GraphicOverlay overlay, Barcode barcode) {
        Context context = overlay.getContext();
        if (getBooleanPref(context, R.string.pref_key_enable_barcode_size_check, false)) {
            float reticleBoxWidth = getBarcodeReticleBox(overlay).width();
            float x = barcode.getBoundingBox() != null ? (float) barcode.getBoundingBox().width() : 0f;
            float barcodeWidth = overlay.translateX(x);
            float requiredWidth = reticleBoxWidth * getIntPref(context, R.string.pref_key_minimum_barcode_width, 50) / 100;
            return coerceAtMost(barcodeWidth / requiredWidth, 1f);
        } else {
            return 1f;
        }
    }

    private static float coerceAtMost(float val, final float max) {
        final float max1 = Math.max(val, max);
        return Math.min(max, max1);
    }

    public static RectF getBarcodeReticleBox(GraphicOverlay overlay) {
        Context context = overlay.getContext();
        float overlayWidth = (float) overlay.getWidth();
        float overlayHeight = (float) overlay.getHeight();
        float boxWidth = overlayWidth * getIntPref(context, R.string.pref_key_barcode_reticle_width, 80) / 100;
        float boxHeight = overlayHeight * getIntPref(context, R.string.pref_key_barcode_reticle_height, 35) / 100;
        float cx = overlayWidth / 2;
        float cy = overlayHeight / 2;
        return new RectF(cx - boxWidth / 2, cy - boxHeight / 2, cx + boxWidth / 2, cy + boxHeight / 2);
    }

    public static boolean shouldDelayLoadingBarcodeResult(Context context) {
        return getBooleanPref(context, R.string.pref_key_delay_loading_barcode_result, true);
    }

    public static boolean getCheckBarcodeInCenter(Context context) {
        return getBooleanPref(context, R.string.pref_key_enable_barcode_center_screen, false);
    }
}
