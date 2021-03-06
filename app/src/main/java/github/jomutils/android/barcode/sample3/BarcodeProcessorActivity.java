package github.jomutils.android.barcode.sample3;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.mlkit.vision.barcode.Barcode;

import java.util.List;

import github.jomutils.android.barcode.BarcodeResult;
import github.jomutils.android.barcode.R;
import github.jomutils.android.barcode.camera.CameraReticleAnimator;
import github.jomutils.android.barcode.camera.GraphicOverlay;
import github.jomutils.android.barcode.settings.PreferenceUtils;
import github.jomutils.android.barcode.settings.SettingsActivity;
import github.jomutils.android.barcode.widget.BarcodeConfirmingGraphic;
import github.jomutils.android.barcode.widget.BarcodeLoadingGraphic;
import github.jomutils.android.barcode.widget.BarcodeReticleGraphic;

import static github.jomutils.android.barcode.WorkflowState.CONFIRMING;
import static github.jomutils.android.barcode.WorkflowState.DETECTED;
import static github.jomutils.android.barcode.WorkflowState.DETECTING;
import static github.jomutils.android.barcode.WorkflowState.PROCEED;
import static github.jomutils.android.barcode.WorkflowState.PROCESSING;
import static github.jomutils.android.barcode.sample.Constants.EXTRA_BARCODE_FORMATS;
import static github.jomutils.android.barcode.sample.Constants.EXTRA_BARCODE_RESULT;
import static github.jomutils.android.barcode.sample1.BarcodeScannerViewModel.REQUEST_CODE_PERMISSIONS;
import static github.jomutils.android.barcode.sample1.BarcodeScannerViewModel.REQUIRED_PERMISSIONS;

public class BarcodeProcessorActivity extends AppCompatActivity {

    private static final String TAG = "BarcodeProcessorActivit";


    /**
     * Creates a starter intent of {@link BarcodeProcessorActivity} with extra barcode formats.
     *
     * @param context
     * @param formats an array of {@link Barcode}'s Formats. {@code null} for all formats support.
     * @return
     */
    public static Intent starter(Context context, int[] formats) {
        Intent starter = new Intent(context, BarcodeProcessorActivity.class);
        starter.putExtra(EXTRA_BARCODE_FORMATS, formats);
        return starter;
    }

    public static void startForResult(Activity activity, int[] formats, int requestCode) {
        Intent starter = starter(activity, formats);
        activity.startActivityForResult(starter, requestCode);
    }

    public static void startForResult(Fragment fragment, int[] formats, int requestCode) {
        Intent starter = starter(fragment.requireContext(), formats);
        fragment.startActivityForResult(starter, requestCode);
    }


    private PreviewView previewView;
    private Camera camera;

    private GraphicOverlay graphicOverlay;
    private Chip promptChip;
    private AnimatorSet promptChipAnimator;

    private CameraReticleAnimator cameraReticleAnimator;

    private View flashButton;

    BarcodeProcessorViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_processor);

        findViewById(R.id.close_button).setOnClickListener(v -> onBackPressed());
        flashButton = findViewById(R.id.flash_button);
        flashButton.setOnClickListener(v -> {
            if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
                if (v.isSelected()) {
                    v.setSelected(false);
                    camera.getCameraControl().enableTorch(false);
                } else {
                    v.setSelected(true);
                    camera.getCameraControl().enableTorch(true);
                }
            } else {
                Toast.makeText(this, "No flash unit", Toast.LENGTH_LONG).show();
            }
        });
        findViewById(R.id.settings_button).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        previewView = findViewById(R.id.viewFinder);
        graphicOverlay = findViewById(R.id.graphicOverlay);

        promptChip = findViewById(R.id.bottom_prompt_chip);
        promptChipAnimator = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.bottom_prompt_chip_enter);
        promptChipAnimator.setTarget(promptChip);

        // Setup Camera Preview Box
        cameraReticleAnimator = new CameraReticleAnimator(graphicOverlay);

        final int[] formats = getIntent().getIntArrayExtra(EXTRA_BARCODE_FORMATS);
        BarcodeProcessorViewModel.Factory factory = new BarcodeProcessorViewModel.Factory(getApplication(), formats);
        viewModel = new ViewModelProvider(this, factory).get(BarcodeProcessorViewModel.class);

        viewModel.getWorkflowState().observe(this, workflowState -> {
            Log.i(TAG, "workflowState: " + workflowState);
            if (workflowState == null)
                return;

            boolean wasPromptChipGone = promptChip.getVisibility() == View.GONE;
            switch (workflowState) {
                case DETECTING:
                    promptChip.setVisibility(View.VISIBLE);
                    promptChip.setText(R.string.prompt_point_at_a_barcode);
                    break;
                case CONFIRMING:
                    promptChip.setVisibility(View.VISIBLE);
                    promptChip.setText(R.string.prompt_move_camera_closer);
                    break;
                case PROCESSING:
                    promptChip.setVisibility(View.VISIBLE);
                    promptChip.setText(R.string.prompt_processing);
                    stopCamera();
                    break;
                case DETECTED:
                case PROCEED:
                    promptChip.setVisibility(View.GONE);
                    stopCamera();
                    break;
                default:
                    promptChip.setVisibility(View.GONE);
                    break;
            }

            boolean shouldPlayPromptChipEnteringAnimation = wasPromptChipGone && promptChip.getVisibility() == View.VISIBLE;
            if (shouldPlayPromptChipEnteringAnimation && !promptChipAnimator.isRunning())
                promptChipAnimator.start();
        });

        viewModel.getDetectedBarcodeResult().observe(this, barcodeResult -> {
            if (barcodeResult != null) {
                Log.i(TAG, "detectedBarcodeResult: " + barcodeResult);

                // Play sound and vibrate
                soundAndVibrate();

                // Finish with result
                finishWithResult(barcodeResult);
            }
        });

        viewModel.getAllBarcodesObservable().observe(this, barcodes -> {
            if (barcodes != null)
                onCameraProcessing(barcodes);
        });

        viewModel.getProcessCameraProvider().observe(this, processCameraProvider -> {
            if (processCameraProvider != null) {
                startCamera(processCameraProvider);
            }
        });

        if (!viewModel.allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        viewModel.onRequestCameraPermission(requestCode, permissions, grantResults);
        final Boolean granted = viewModel.getPermissionGrantingObservable().getValue();
        if (granted != null && !granted) {
            finish();
        }
    }

    private void startCamera(@NonNull ProcessCameraProvider cameraProvider) {
        final RectF barcodeReticleBox = PreferenceUtils.getBarcodeReticleBox(graphicOverlay);
        final Size size = new Size(Math.round(barcodeReticleBox.width()), Math.round(barcodeReticleBox.height()));

        camera = viewModel.startCamera(
                cameraProvider,
                this,
                previewView,
                size);

        if (camera.getCameraInfo().hasFlashUnit()) {
            camera.getCameraControl().enableTorch(flashButton.isSelected());
        }
    }

    private void stopCamera() {
        final ProcessCameraProvider value = viewModel.getProcessCameraProvider().getValue();
        if (value != null) {
            value.unbindAll();
        }
    }

    private void soundAndVibrate() {
        MediaPlayer.create(this, R.raw.beep).start();
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(250);
        }
    }

    private void finishWithResult(BarcodeResult barcodeResult) {
        Intent data = new Intent();
        data.putExtra(EXTRA_BARCODE_RESULT, barcodeResult);
        setResult(RESULT_OK, data);
        // TODO: 4/18/21 Uncomment
        finish();
    }

    private void onCameraProcessing(@NonNull List<Barcode> barcodes) {
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
            viewModel.setWorkflowState(DETECTING);
        } else {
            barcodeInCenter = barcodes.get(0);

            cameraReticleAnimator.cancel();
            Log.i(TAG, "onCameraProcessing: Got a barcodeCenter " + barcodeInCenter.getBoundingBox());
            float sizeProgress = PreferenceUtils.getProgressToMeetBarcodeSizeRequirement(graphicOverlay, barcodeInCenter);
            if (sizeProgress < 1) {
                // Barcode in the camera view is too small, so prompt user to move camera closer.
                graphicOverlay.add(new BarcodeConfirmingGraphic(graphicOverlay, barcodeInCenter));
                viewModel.setWorkflowState(CONFIRMING);
            } else {
                // Barcode size in the camera view is sufficient.
                if (PreferenceUtils.shouldDelayLoadingBarcodeResult(graphicOverlay.getContext())) {
                    ValueAnimator loadingAnimator = createLoadingAnimator(graphicOverlay, barcodeInCenter);
                    loadingAnimator.start();
                    graphicOverlay.add(new BarcodeLoadingGraphic(graphicOverlay, loadingAnimator));
                    viewModel.setWorkflowState(PROCESSING);
                } else {
                    viewModel.setWorkflowState(DETECTED);
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
                viewModel.setWorkflowState(PROCEED);
                viewModel.setDetectedBarcode(barcode);
            } else {
                graphicOverlay.invalidate();
            }
        });

        return valueAnimator;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Classes
    ///////////////////////////////////////////////////////////////////////////

}
