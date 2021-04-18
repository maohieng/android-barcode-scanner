package github.jomutils.android.barcode.sample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.mlkit.vision.barcode.Barcode;

import github.jomutils.android.barcode.BarcodeScannerViewModel;
import github.jomutils.android.barcode.R;

import static github.jomutils.android.barcode.BarcodeScannerViewModel.REQUEST_CODE_PERMISSIONS;
import static github.jomutils.android.barcode.BarcodeScannerViewModel.REQUIRED_PERMISSIONS;
import static github.jomutils.android.barcode.sample.Constants.EXTRA_BARCODE_FORMATS;
import static github.jomutils.android.barcode.sample.Constants.EXTRA_BARCODE_RESULT;

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

    private PreviewView previewView;
    private Camera camera;

    BarcodeScannerViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        previewView = findViewById(R.id.viewFinder);

        final int[] formats = getIntent().getIntArrayExtra(EXTRA_BARCODE_FORMATS);
        BarcodeScannerViewModel.Factory factory = new BarcodeScannerViewModel.Factory(getApplication(), formats);
        viewModel = new ViewModelProvider(this, factory).get(BarcodeScannerViewModel.class);

        viewModel.getBarcodeResultObservable().observe(this, barcodeResult -> {
            if (barcodeResult != null) {
                // Play sound and vibrate
                soundAndVibrate();

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        viewModel.onRequestCameraPermission(requestCode, permissions, grantResults);
        final Boolean granted = viewModel.getPermissionGrantingObservable().getValue();
        if (granted != null && !granted) {
            finish();
        }
    }

    private void startCamera(@NonNull ProcessCameraProvider cameraProvider) {
        camera = viewModel.startCamera(
                cameraProvider,
                this,
                previewView);
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

    ///////////////////////////////////////////////////////////////////////////
    // Classes
    ///////////////////////////////////////////////////////////////////////////

}
