package github.jomutils.android.barcode;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.mlkit.vision.barcode.Barcode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static github.jomutils.android.barcode.BarcodeScannerViewModel.REQUEST_CODE_PERMISSIONS;
import static github.jomutils.android.barcode.BarcodeScannerViewModel.REQUIRED_PERMISSIONS;
import static github.jomutils.android.barcode.Constants.EXTRA_BARCODE_FORMATS;
import static github.jomutils.android.barcode.Constants.EXTRA_BARCODE_RESULT;

public class BarcodeScannerSampleActivity extends AppCompatActivity {

    private static final String TAG = "CameraXBasic";


    /**
     * Creates a starter intent of {@link BarcodeScannerSampleActivity} with extra barcode formats.
     *
     * @param context
     * @param formats an array of {@link Barcode}'s Formats. {@code null} for all formats support.
     * @return
     */
    public static Intent starter(Context context, int[] formats) {
        Intent starter = new Intent(context, BarcodeScannerSampleActivity.class);
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

    private ExecutorService cameraExecutor;
    private PreviewView previewView;

    BarcodeScannerViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        cameraExecutor = Executors.newSingleThreadExecutor();

        previewView = findViewById(R.id.viewFinder);

        final int[] formats = getIntent().getIntArrayExtra(EXTRA_BARCODE_FORMATS);
        BarcodeScannerViewModel.Factory factory = new BarcodeScannerViewModel.Factory(getApplication(), formats);
        viewModel = new ViewModelProvider(this, factory).get(BarcodeScannerViewModel.class);

        viewModel.getBarcodeResultObservable().observe(this, barcodeResult -> {
            if (barcodeResult != null) {
                // Vibrate
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(250);
                }

                // Finish with result
                Intent data = new Intent();
                data.putExtra(EXTRA_BARCODE_RESULT, barcodeResult);
                setResult(RESULT_OK, data);
                finish();
            }
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
    protected void onDestroy() {
        cameraExecutor.shutdown();
        super.onDestroy();
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
        // Create a Preview
        DisplayMetrics displayMetrics = new DisplayMetrics();
        previewView.getDisplay().getRealMetrics(displayMetrics);

        final int aspectRatio = CameraHelper.getAspectRatio(displayMetrics.widthPixels, displayMetrics.heightPixels);

        final int rotation = previewView.getDisplay().getRotation();

        final Preview preview = new Preview.Builder()
                .setTargetAspectRatio(aspectRatio)
                .setTargetRotation(rotation)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        final ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetAspectRatio(aspectRatio)
                .setTargetRotation(rotation)
                .setImageQueueDepth(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, viewModel.getImageAnalyzer());

        // Unbind use cases before rebinding
        cameraProvider.unbindAll();

        // Bind use cases to camera
        /*final Camera camera = */
        cameraProvider.bindToLifecycle(
                BarcodeScannerSampleActivity.this,
                cameraSelector,
                preview,
                imageAnalysis
        );
    }

    ///////////////////////////////////////////////////////////////////////////
    // Classes
    ///////////////////////////////////////////////////////////////////////////

}
