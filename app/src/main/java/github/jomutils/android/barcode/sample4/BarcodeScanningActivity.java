package github.jomutils.android.barcode.sample4;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.mlkit.vision.barcode.Barcode;

import github.jomutils.android.barcode.BarcodeResult;
import github.jomutils.android.barcode.R;
import github.jomutils.android.barcode.camera.GraphicOverlay;
import github.jomutils.android.barcode.settings.SettingsActivity;

import static github.jomutils.android.barcode.sample.Constants.EXTRA_BARCODE_FORMATS;
import static github.jomutils.android.barcode.sample.Constants.EXTRA_BARCODE_RESULT;
import static github.jomutils.android.barcode.sample1.BarcodeScannerViewModel.REQUEST_CODE_PERMISSIONS;
import static github.jomutils.android.barcode.sample1.BarcodeScannerViewModel.REQUIRED_PERMISSIONS;

public class BarcodeScanningActivity extends AppCompatActivity {

    private static final String TAG = "BarcodeProcessorActivit";


    /**
     * Creates a starter intent of {@link BarcodeScanningActivity} with extra barcode formats.
     *
     * @param context
     * @param formats an array of {@link Barcode}'s Formats. {@code null} for all formats support.
     * @return
     */
    public static Intent starter(Context context, int[] formats) {
        Intent starter = new Intent(context, BarcodeScanningActivity.class);
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

    private View flashButton;

    ScanningViewModel viewModel;
    BarcodeScannerUI scannerUI;

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

        final int[] formats = getIntent().getIntArrayExtra(EXTRA_BARCODE_FORMATS);
        ScanningViewModel.Factory factory = new ScanningViewModel.Factory(getApplication(), formats);
        viewModel = new ViewModelProvider(this, factory).get(ScanningViewModel.class);

        scannerUI = new BarcodeScannerUI(viewModel, previewView, graphicOverlay);
        scannerUI.setCallback(new BarcodeScannerUI.ScannerCallback() {
            @Override
            public void onCameraStart(@NonNull Camera camera) {
                BarcodeScanningActivity.this.camera = camera;

                if (camera.getCameraInfo().hasFlashUnit()) {
                    camera.getCameraControl().enableTorch(flashButton.isSelected());
                }
            }

            @Override
            public void onBarcodeDetectedResult(@NonNull BarcodeResult barcodeResult) {
                // Finish with result
                finishWithResult(barcodeResult);
            }
        });

        scannerUI.setWorkflowCallback(workflowState -> {
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
                    break;
                default:
                    promptChip.setVisibility(View.GONE);
                    break;
            }

            boolean shouldPlayPromptChipEnteringAnimation = wasPromptChipGone && promptChip.getVisibility() == View.VISIBLE;
            if (shouldPlayPromptChipEnteringAnimation && !promptChipAnimator.isRunning())
                promptChipAnimator.start();
        });

        scannerUI.bindToLifecycle(this);

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

    private void finishWithResult(BarcodeResult barcodeResult) {
        Intent data = new Intent();
        data.putExtra(EXTRA_BARCODE_RESULT, barcodeResult);
        setResult(RESULT_OK, data);
        finish();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Classes
    ///////////////////////////////////////////////////////////////////////////

}
