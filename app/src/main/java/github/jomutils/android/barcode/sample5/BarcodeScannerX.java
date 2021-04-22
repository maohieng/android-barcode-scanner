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

    public static final String EXTRA_BARCODE_FORMATS = "extra-barcode-format";

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

        // TODO: 4/20/21 Try to fix on some device drawing black - https://stackoverflow.com/a/44607874/857346
        this.graphicOverlay.setLayerType(GraphicOverlay.LAYER_TYPE_SOFTWARE, null);

        this.mainHandler = new Handler(Looper.getMainLooper());
        // Setup Camera Preview Box
        this.cameraReticleAnimator = new CameraReticleAnimator(this.graphicOverlay);

    }

    /**
     * Used at Activity's onCreate()
     *
     * @param activity an {@link AppCompatActivity} which optionally contains intent extra {@link #EXTRA_BARCODE_FORMATS}.
     */
    public static BarcodeScannerX New(AppCompatActivity activity,
                                      PreviewView previewView,
                                      GraphicOverlay graphicOverlay) {
        final int[] formats = activity.getIntent().getIntArrayExtra(EXTRA_BARCODE_FORMATS);

        BarcodeScannerXViewModel.Factory factory = new BarcodeScannerXViewModel.Factory(activity.getApplication(), formats);
        BarcodeScannerXViewModel viewModel = new ViewModelProvider(activity, factory).get(BarcodeScannerXViewModel.class);

        BarcodeScannerX scannerX = new BarcodeScannerX(viewModel, previewView, graphicOverlay);
        scannerX.bindToLifecycle(activity);
        scannerX.checkPermission(activity);

        return scannerX;
    }

    /**
     * Used at Fragment's onCreateView() or onViewCreated() or onActivityCreated()
     *
     * @param fragment       a Fragment that should has arguments contains {@link Barcode} formats.
     * @param previewView    CameraX PreviewView
     * @param graphicOverlay
     */
    public static BarcodeScannerX New(Fragment fragment,
                                      PreviewView previewView,
                                      GraphicOverlay graphicOverlay) {
        final Bundle arguments = fragment.getArguments();
        final int[] formats = arguments != null ? arguments.getIntArray(EXTRA_BARCODE_FORMATS) : null;

        BarcodeScannerXViewModel.Factory factory = new BarcodeScannerXViewModel.Factory(fragment.requireActivity().getApplication(), formats);
        BarcodeScannerXViewModel viewModel = new ViewModelProvider(fragment, factory).get(BarcodeScannerXViewModel.class);

        BarcodeScannerX scannerX = new BarcodeScannerX(viewModel, previewView, graphicOverlay);
        scannerX.bindToLifecycle(fragment);
        scannerX.checkPermission(fragment.requireActivity());

        return scannerX;
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
                onBarcodeProcessing(barcodes);
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

        Log.i(TAG, "startCamera: GraphOverlay(" + graphicOverlay.getWidth() + ", " + graphicOverlay.getHeight() + ")");

        return viewModel.startCamera(
                cameraProvider,
                lifecycleOwner,
                previewView);
    }

    @MainThread
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
        //viewModel.freezeCamera();
        //Log.i(TAG, "freezeCamera: ");
    }

    private void onBarcodeProcessing(@NonNull List<Barcode> barcodes) {

        if (!isCameraLive) return;

        //Log.d(TAG, "Barcode result size: " + barcodes.size());

        GraphicOverlay graphicOverlay = this.graphicOverlay;

        // Picks the barcode, if exists, that covers the center of graphic overlay.
        Barcode barcodeInCenter = null;
        if (PreferenceUtils.getCheckBarcodeInCenter(context)) {
            for (Barcode barcode : barcodes) {
                final Rect boundingBox = barcode.getBoundingBox();
                if (boundingBox != null) {
                    final RectF barcodeBox = graphicOverlay.translateRect(boundingBox);
                    final boolean contains = barcodeBox.contains(graphicOverlay.getWidth() / 2f, graphicOverlay.getHeight() / 2f);
                    if (contains) {
                        barcodeInCenter = barcode;
                        break;
                    }
                }
            }
        } else if (!barcodes.isEmpty()) {
            barcodeInCenter = barcodes.get(0);
        }

        graphicOverlay.clear();
        if (barcodeInCenter == null) {
            cameraReticleAnimator.start();
            graphicOverlay.add(new BarcodeReticleGraphic(graphicOverlay, cameraReticleAnimator));
            viewModel.setWorkflowState(WorkflowState.DETECTING);
        } else {
            cameraReticleAnimator.cancel();
            Log.i(TAG, "onCameraProcessing: barcodeInCenter " + barcodeInCenter.getBoundingBox());
            float sizeProgress = PreferenceUtils.getProgressToMeetBarcodeSizeRequirement(graphicOverlay, barcodeInCenter);
            if (sizeProgress < 1) {
//             Barcode in the camera view is too small, so prompt user to move camera closer.
                graphicOverlay.add(new BarcodeConfirmingGraphic(graphicOverlay, barcodeInCenter));
                viewModel.setWorkflowState(WorkflowState.CONFIRMING);
            } else {
//             Barcode size in the camera view is sufficient.
                if (PreferenceUtils.shouldDelayLoadingBarcodeResult(context)) {
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

//        final BarcodeGraphicBase graphic = (BarcodeGraphicBase) graphicOverlay.getGraphics().get(0);
//        final RectF boxRect = graphic.getBoxRect();
//        Log.i(TAG, "onBarcodeProcessing: "+boxRect.toShortString());
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
