package github.jomutils.android.barcode.sample5;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.google.mlkit.vision.barcode.Barcode;

import java.util.List;

import github.jomutils.android.barcode.BarcodeResult;
import github.jomutils.android.barcode.R;
import github.jomutils.android.barcode.WorkflowState;
import github.jomutils.android.barcode.camera.CameraReticleAnimator;
import github.jomutils.android.barcode.camera.GraphicOverlay;
import github.jomutils.android.barcode.settings.PreferenceUtils;
import github.jomutils.android.barcode.widget.BarcodeConfirmingGraphic;
import github.jomutils.android.barcode.widget.BarcodeLoadingGraphic;
import github.jomutils.android.barcode.widget.BarcodeReticleGraphic;
import github.jomutils.android.barcode.widget.GoogleGraphicOverlay;

import static github.jomutils.android.barcode.sample.Constants.EXTRA_BARCODE_FORMATS;

/**
 * See {@link BarcodeScanningXActivity} for example of using this class.
 */
public class BarcodeScannerX {

    public abstract static class ScannerCallback {
        public abstract void onCameraStart(@NonNull Camera camera);

        public abstract void onBarcodeDetectedResult(@NonNull BarcodeResult barcodeResult);

    }

    public interface WorkflowCallback {
        public void onWorkflowStateChanged(@NonNull WorkflowState workflowState);
    }

    private static final String TAG = "BarcodeScannerUI";

    private final Context context;
    private final BarcodeScannerXViewModel viewModel;

    private final PreviewView previewView;
    private final GraphicOverlay graphicOverlay;

    private final Handler mainHandler;
    private final CameraReticleAnimator cameraReticleAnimator;

    private Camera camera;
    private boolean isCameraLive = false;

    private ScannerCallback callback;
    private WorkflowCallback workflowCallback;

    /**
     * What you should do after instantiate an object using this constructor:
     * 1. invoke {@link #bindToLifecycle(LifecycleOwner)}
     * 2. invoke {@link #checkPermission(Activity)}
     */
    public BarcodeScannerX(BarcodeScannerXViewModel viewModel, PreviewView previewView, GraphicOverlay graphicOverlay) {
        this.viewModel = viewModel;
        this.context = graphicOverlay.getContext();
        this.previewView = previewView;
        this.graphicOverlay = graphicOverlay;

        mainHandler = new Handler(Looper.getMainLooper());
        // Setup Camera Preview Box
        cameraReticleAnimator = new CameraReticleAnimator(this.graphicOverlay);
    }

    /**
     * Used at Activity's onCreate()
     */
    public BarcodeScannerX(AppCompatActivity activity, PreviewView previewView, GraphicOverlay graphicOverlay) {
        final int[] formats = activity.getIntent().getIntArrayExtra(EXTRA_BARCODE_FORMATS);

        BarcodeScannerXViewModel.Factory factory = new BarcodeScannerXViewModel.Factory(activity.getApplication(), formats);
        this.viewModel = new ViewModelProvider(activity, factory).get(BarcodeScannerXViewModel.class);

        this.context = graphicOverlay.getContext();
        this.previewView = previewView;
        this.graphicOverlay = graphicOverlay;

        mainHandler = new Handler(Looper.getMainLooper());
        // Setup Camera Preview Box
        cameraReticleAnimator = new CameraReticleAnimator(this.graphicOverlay);

        bindToLifecycle(activity);

        checkPermission(activity);
    }

    /**
     * Used at Fragment's onCreateView() or onViewCreated() or onActivityCreated
     *
     * @param fragment       a Fragment that should has arguments contains {@link Barcode} formats.
     * @param previewView    CameraX PreviewView
     * @param graphicOverlay
     */
    public BarcodeScannerX(Fragment fragment, PreviewView previewView, GraphicOverlay graphicOverlay) {
        final Bundle arguments = fragment.getArguments();
        final int[] formats = arguments != null ? arguments.getIntArray(EXTRA_BARCODE_FORMATS) : null;

        BarcodeScannerXViewModel.Factory factory = new BarcodeScannerXViewModel.Factory(fragment.requireActivity().getApplication(), formats);
        this.viewModel = new ViewModelProvider(fragment, factory).get(BarcodeScannerXViewModel.class);

        this.context = graphicOverlay.getContext();
        this.previewView = previewView;
        this.graphicOverlay = graphicOverlay;

        mainHandler = new Handler(Looper.getMainLooper());
        // Setup Camera Preview Box
        cameraReticleAnimator = new CameraReticleAnimator(this.graphicOverlay);

        bindToLifecycle(fragment);

        checkPermission(fragment.requireActivity());
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
            if (barcodes != null) {
                // TODO: 4/19/21 uncomment
                onCameraProcessing(barcodes);
            }
        });

        viewModel.getProcessCameraProvider().observe(lifecycleOwner, processCameraProvider -> {
            if (processCameraProvider != null) {
                camera = startCamera(processCameraProvider, lifecycleOwner);

                if (this.callback != null) {
                    this.callback.onCameraStart(camera);
                }
            }
        });
    }

    private void checkPermission(Activity activity) {
        if (!viewModel.allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                    activity, BarcodeScannerXViewModel.REQUIRED_PERMISSIONS.toArray(new String[0]), BarcodeScannerXViewModel.REQUEST_CODE_PERMISSIONS);
        }
    }

    public boolean onRequestCameraPermission(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        viewModel.onRequestCameraPermission(requestCode, permissions, grantResults);
        final Boolean granted = viewModel.getPermissionGrantingObservable().getValue();
        return granted == null || granted;
    }

    private Camera startCamera(@NonNull ProcessCameraProvider cameraProvider, LifecycleOwner lifecycleOwner) {
        isCameraLive = true;

        if (graphicOverlay instanceof GoogleGraphicOverlay) {
            GoogleGraphicOverlay overlay = (GoogleGraphicOverlay) graphicOverlay;
            final RectF box = PreferenceUtils.getBarcodeReticleBox(overlay);
            overlay.setImageSourceInfo(
                    Math.round(box.width()),
                    Math.round(box.height()),
                    false);
        }

        return viewModel.startCamera(
                cameraProvider,
                lifecycleOwner,
                previewView);
    }

    public void unfreezeCamera(LifecycleOwner lifecycleOwner) {
        viewModel.unFreezeCamera(lifecycleOwner);
        Log.i(TAG, "unfreezeCamera: ");

        // Pending Image Analysis to let Preview finishes
        mainHandler.postDelayed(() -> {
            isCameraLive = true;
        }, 500);

        if (this.callback != null) {
            this.callback.onCameraStart(camera);
        }
    }

    @MainThread
    public void freezeCamera() {
        isCameraLive = false;
        viewModel.freezeCamera();
        Log.i(TAG, "freezeCamera: ");
    }

    private void onCameraProcessing(@NonNull List<Barcode> barcodes) {

        if (!isCameraLive) return;

        Log.d(TAG, "Barcode result size: " + barcodes.size());

        GraphicOverlay graphicOverlay = this.graphicOverlay;

        // Picks the barcode, if exists, that covers the center of graphic overlay.
        Barcode barcodeInCenter = null;
        for (Barcode barcode : barcodes) {
            final Rect boundingBox = barcode.getBoundingBox();
            if (boundingBox != null) {
                final RectF box = graphicOverlay.translateRect(boundingBox);
                final boolean contains = box.contains(graphicOverlay.getWidth() / 2f, graphicOverlay.getHeight() / 2f);
                if (contains) {
                    barcodeInCenter = barcode;
                    break;
                }
            }
        }

        graphicOverlay.clear();
        if (barcodeInCenter == null) {
            cameraReticleAnimator.start();
            graphicOverlay.add(new BarcodeReticleGraphic(graphicOverlay, cameraReticleAnimator));
            viewModel.setWorkflowState(WorkflowState.DETECTING);
        } else {
            cameraReticleAnimator.cancel();
            Log.i(TAG, "onCameraProcessing: Got a barcodeCenter " + barcodeInCenter.getBoundingBox());
            float sizeProgress = PreferenceUtils.getProgressToMeetBarcodeSizeRequirement(graphicOverlay, barcodeInCenter);
            if (sizeProgress < 1) {
//             Barcode in the camera view is too small, so prompt user to move camera closer.
                graphicOverlay.add(new BarcodeConfirmingGraphic(graphicOverlay, barcodeInCenter));
                viewModel.setWorkflowState(WorkflowState.CONFIRMING);
            } else {
//             Barcode size in the camera view is sufficient.
                if (PreferenceUtils.shouldDelayLoadingBarcodeResult(graphicOverlay.getContext())) {
                    ValueAnimator loadingAnimator = createLoadingAnimator(graphicOverlay, barcodeInCenter);
                    loadingAnimator.start();
                    graphicOverlay.add(new BarcodeLoadingGraphic(graphicOverlay, loadingAnimator));
                    viewModel.setWorkflowState(WorkflowState.PROCESSING);
                } else {
                    viewModel.setWorkflowState(WorkflowState.DETECTED);
                    viewModel.setDetectedBarcode(barcodeInCenter);
                }
            }
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
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(250);
            }
        }
    }
}
