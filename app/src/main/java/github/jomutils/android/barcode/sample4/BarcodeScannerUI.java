package github.jomutils.android.barcode.sample4;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.Size;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;

import com.google.mlkit.vision.barcode.Barcode;

import java.util.List;

import github.jomutils.android.barcode.BarcodeResult;
import github.jomutils.android.barcode.R;
import github.jomutils.android.barcode.WorkflowState;
import github.jomutils.android.barcode.camera.CameraReticleAnimator;
import github.jomutils.android.barcode.camera.GraphicOverlay;
import github.jomutils.android.barcode.settings.PreferenceUtils;
import github.jomutils.android.barcode.widget.BarcodeLoadingGraphic;
import github.jomutils.android.barcode.widget.BarcodeReticleGraphic;

public class BarcodeScannerUI {

    public interface ScannerCallback {
        public void onCameraStart(@NonNull Camera camera);

        public void onBarcodeDetectedResult(@NonNull BarcodeResult barcodeResult);
    }

    public interface WorkflowCallback {
        public void onWorkflowStateChanged(@NonNull WorkflowState workflowState);
    }

    private static final String TAG = "BarcodeScannerUI";

    private final Context context;
    private final ScanningViewModel viewModel;
    private final PreviewView previewView;
    private final GraphicOverlay graphicOverlay;

    private boolean isCameraLive = false;

    private final CameraReticleAnimator cameraReticleAnimator;

    private ScannerCallback callback;
    private WorkflowCallback workflowCallback;

    public BarcodeScannerUI(ScanningViewModel viewModel, PreviewView previewView, GraphicOverlay graphicOverlay) {
        this.context = graphicOverlay.getContext();
        this.viewModel = viewModel;
        this.previewView = previewView;
        this.graphicOverlay = graphicOverlay;
        // Setup Camera Preview Box
        cameraReticleAnimator = new CameraReticleAnimator(this.graphicOverlay);
    }

    public void setCallback(ScannerCallback callback) {
        this.callback = callback;
    }

    public void setWorkflowCallback(WorkflowCallback workflowCallback) {
        this.workflowCallback = workflowCallback;
    }

    public void bindToLifecycle(final LifecycleOwner lifecycleOwner) {
        viewModel.getWorkflowState().observe(lifecycleOwner, workflowState -> {
            Log.i(TAG, "workflowState: " + workflowState);
            if (workflowState == null)
                return;

            switch (workflowState) {
                case PROCESSING:
                case DETECTED:
                case PROCEED:
                    freezeCamera();
                    break;
            }

            if (workflowCallback != null) {
                workflowCallback.onWorkflowStateChanged(workflowState);
            }
        });

        viewModel.getDetectedBarcodeResult().observe(lifecycleOwner, barcodeResult -> {
            if (barcodeResult != null) {
                Log.i(TAG, "detectedBarcodeResult: " + barcodeResult);

                // Play sound and vibrate
                soundAndVibrate();

                if (this.callback != null) {
                    callback.onBarcodeDetectedResult(barcodeResult);
                }
            }
        });

        viewModel.getAllBarcodesObservable().observe(lifecycleOwner, barcodes -> {
            if (barcodes != null)
                onCameraProcessing(barcodes);
        });

        viewModel.getProcessCameraProvider().observe(lifecycleOwner, processCameraProvider -> {
            if (processCameraProvider != null) {
                final Camera camera = startCamera(processCameraProvider, lifecycleOwner);

                if (this.callback != null) {
                    this.callback.onCameraStart(camera);
                }
            }
        });
    }

//    public BarcodeScannerUI(AppCompatActivity appCompatActivity,)

    private Camera startCamera(@NonNull ProcessCameraProvider cameraProvider, LifecycleOwner lifecycleOwner) {
        isCameraLive = true;

        final Size size = getBarcodeReticleBoxSize();

        return viewModel.startCamera(
                cameraProvider,
                lifecycleOwner,
                previewView,
                size);
    }

    // TODO: 4/19/21 test this method
    public void unfreezeCamera(LifecycleOwner lifecycleOwner) {
        isCameraLive = true;
        Log.i(TAG, "unfreezeCamera: ");
        viewModel.unFreezeCamera(lifecycleOwner);
    }

    @MainThread
    public void freezeCamera() {
        isCameraLive = false;
        Log.i(TAG, "freezeCamera: ");
        viewModel.freezeCamera();
    }

    private Size getBarcodeReticleBoxSize() {
        final RectF barcodeReticleBox = PreferenceUtils.getBarcodeReticleBox(graphicOverlay);
        return new Size(Math.round(barcodeReticleBox.width()), Math.round(barcodeReticleBox.height()));
    }

    private void onCameraProcessing(@NonNull List<Barcode> barcodes) {

        if (!isCameraLive) return;

        // Log.d(TAG, "Barcode result size: " + barcodes.size());
        GraphicOverlay graphicOverlay = this.graphicOverlay;

        // Picks the barcode, if exists, that covers the center of graphic overlay.
        Barcode barcodeInCenter = null;
//        for (Barcode barcode : barcodes) {
//            final Rect boundingBox = barcode.getBoundingBox();
//            if (boundingBox != null) {
//                final RectF box = graphicOverlay.translateRect(boundingBox);
//                final boolean contains = box.contains(graphicOverlay.getWidth() / 2f, graphicOverlay.getHeight() / 2f);
//                if (contains) {
//                    barcodeInCenter = barcode;
//                    break;
//                }
//            }
//        }

        graphicOverlay.clear();
        if (barcodes.isEmpty()) {
            cameraReticleAnimator.start();
            graphicOverlay.add(new BarcodeReticleGraphic(graphicOverlay, cameraReticleAnimator));
            viewModel.setWorkflowState(WorkflowState.DETECTING);
        } else {
            barcodeInCenter = barcodes.get(0);

            cameraReticleAnimator.cancel();
            Log.i(TAG, "onCameraProcessing: Got a barcodeCenter " + barcodeInCenter.getBoundingBox());
//            float sizeProgress = PreferenceUtils.getProgressToMeetBarcodeSizeRequirement(graphicOverlay, barcodeInCenter);
//            if (sizeProgress < 1) {
            // Barcode in the camera view is too small, so prompt user to move camera closer.
//                graphicOverlay.add(new BarcodeConfirmingGraphic(graphicOverlay, barcodeInCenter));
//                viewModel.setWorkflowState(WorkflowState.CONFIRMING);
//            } else {
            // Barcode size in the camera view is sufficient.
            if (PreferenceUtils.shouldDelayLoadingBarcodeResult(graphicOverlay.getContext())) {
                ValueAnimator loadingAnimator = createLoadingAnimator(graphicOverlay, barcodeInCenter);
                loadingAnimator.start();
                graphicOverlay.add(new BarcodeLoadingGraphic(graphicOverlay, loadingAnimator));
                viewModel.setWorkflowState(WorkflowState.PROCESSING);
            } else {
                viewModel.setWorkflowState(WorkflowState.DETECTED);
                viewModel.setDetectedBarcode(barcodeInCenter);
            }
//            }
        }
        graphicOverlay.invalidate();
    }

    private ValueAnimator createLoadingAnimator(final GraphicOverlay graphicOverlay, final Barcode barcode) {
        float endProgress = 1.1f;
        final ValueAnimator valueAnimator = ValueAnimator.ofFloat(0f, endProgress)
                .setDuration(2000);
        valueAnimator.addUpdateListener(animation -> {
            final Float animatedValue = (Float) valueAnimator.getAnimatedValue();
            if (animatedValue.compareTo(endProgress) >= 0) {
                Log.i(TAG, "createLoadingAnimator: animation DONE");
                graphicOverlay.clear();
                viewModel.setWorkflowState(WorkflowState.PROCEED);
                viewModel.setDetectedBarcode(barcode);
            } else {
                graphicOverlay.invalidate();
            }
        });

        return valueAnimator;
    }

    private void soundAndVibrate() {
        MediaPlayer.create(context, R.raw.beep).start();
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(250);
        }
    }
}
