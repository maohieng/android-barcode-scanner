package github.jomutils.android.barcode.sample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

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

import github.jomutils.android.barcode.BarcodeScannerViewModel;
import github.jomutils.android.barcode.R;

import static github.jomutils.android.barcode.BarcodeScannerViewModel.REQUEST_CODE_PERMISSIONS;
import static github.jomutils.android.barcode.BarcodeScannerViewModel.REQUIRED_PERMISSIONS;
import static github.jomutils.android.barcode.sample.Constants.EXTRA_BARCODE_FORMATS;
import static github.jomutils.android.barcode.sample.Constants.EXTRA_BARCODE_RESULT;

public class BarCodeScannerActivity extends AppCompatActivity {

    private static final String TAG = "CameraXBasic";


    /**
     * Creates a starter intent of {@link BarCodeScannerActivity} with extra barcode formats.
     *
     * @param context
     * @param formats an array of {@link Barcode}'s Formats. {@code null} for all formats support.
     * @return
     */
    public static Intent starter(Context context, int[] formats) {
        Intent starter = new Intent(context, BarCodeScannerActivity.class);
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
    private PreviewView viewFinder;

    BarcodeScannerViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        cameraExecutor = Executors.newSingleThreadExecutor();

        viewFinder = findViewById(R.id.viewFinder);

        final int[] formats = getIntent().getIntArrayExtra(EXTRA_BARCODE_FORMATS);
        BarcodeScannerViewModel.Factory factory = new BarcodeScannerViewModel.Factory(getApplication(), formats);
        viewModel = new ViewModelProvider(this, factory).get(BarcodeScannerViewModel.class);

        viewModel.getBarcodeResultObservable().observe(this, barcodeResult -> {
            if (barcodeResult != null) {
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
        final Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        final ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, viewModel.getImageAnalyzer());

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll();

            // Bind use cases to camera
            /*camera = */
            cameraProvider.bindToLifecycle(BarCodeScannerActivity.this,
                    cameraSelector,
                    preview,
                    imageAnalysis);

        } catch (Exception exc) {
            Log.e(TAG, "Use case binding failed", exc);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Classes
    ///////////////////////////////////////////////////////////////////////////

}
