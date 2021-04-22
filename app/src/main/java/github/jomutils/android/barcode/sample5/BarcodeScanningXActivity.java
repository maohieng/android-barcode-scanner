package github.jomutils.android.barcode.sample5;

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
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.mlkit.vision.barcode.Barcode;

import github.jomutils.android.barcode.BarcodeResult;
import github.jomutils.android.barcode.R;
import github.jomutils.android.barcode.camera.GraphicOverlay;
import github.jomutils.android.barcode.settings.SettingsActivity;

import static github.jomutils.android.barcode.sample.Constants.EXTRA_BARCODE_RESULT;

public class BarcodeScanningXActivity extends AppCompatActivity {

    private static final String TAG = "BarcodeProcessorActivit";


    /**
     * Creates a starter intent of {@link BarcodeScanningXActivity} with extra barcode formats.
     *
     * @param context
     * @param formats an array of {@link Barcode}'s Formats. {@code null} for all formats support.
     * @return
     */
    public static Intent starter(Context context, int[] formats) {
        Intent starter = new Intent(context, BarcodeScanningXActivity.class);
        starter.putExtra(BarcodeScannerX.EXTRA_BARCODE_FORMATS, formats);
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
//    private FloatingActionButton fab;

    BarcodeScannerX scannerUI;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_barcode_scannerx);

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
//        fab = findViewById(R.id.fab);
//        fab.setOnClickListener(v -> scannerUI.unfreezeCamera(BarcodeScanningXActivity.this));

        previewView = findViewById(R.id.viewFinder);
        graphicOverlay = findViewById(R.id.graphicOverlay);

        promptChip = findViewById(R.id.bottom_prompt_chip);
        promptChipAnimator = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.bottom_prompt_chip_enter);
        promptChipAnimator.setTarget(promptChip);

        scannerUI = BarcodeScannerX.New(this, previewView, graphicOverlay);
        scannerUI.setCallback(new BarcodeScannerX.ScannerCallback() {
            @Override
            public void onCameraStart(@NonNull Camera camera) {
                BarcodeScanningXActivity.this.camera = camera;

                if (camera.getCameraInfo().hasFlashUnit()) {
                    camera.getCameraControl().enableTorch(flashButton.isSelected());
                }

//                fab.hide();
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
                case DETECTED:
                case PROCEED:
                    promptChip.setVisibility(View.GONE);
                    break;
            }

            boolean shouldPlayPromptChipEnteringAnimation = wasPromptChipGone && promptChip.getVisibility() == View.VISIBLE;
            if (shouldPlayPromptChipEnteringAnimation && !promptChipAnimator.isRunning())
                promptChipAnimator.start();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = scannerUI.onRequestCameraPermission(requestCode, permissions, grantResults);
        if (!granted) {
            setResult(RESULT_CANCELED);
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
