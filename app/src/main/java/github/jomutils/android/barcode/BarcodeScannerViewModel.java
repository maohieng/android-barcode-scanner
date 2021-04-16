package github.jomutils.android.barcode;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class BarcodeScannerViewModel extends AndroidViewModel {

    public static class Factory extends ViewModelProvider.NewInstanceFactory {
        private final Application application;
        private final int[] formats;

        public Factory(Application application, @Nullable int[] formats) {
            this.application = application;
            this.formats = formats;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new BarcodeScannerViewModel(application, formats);
        }
    }

    private static final String TAG = "BarcodeScannerViewModel";

    public static final int REQUEST_CODE_PERMISSIONS = 10;
    public static final List<String> REQUIRED_PERMISSIONS;

    static {
        REQUIRED_PERMISSIONS = new ArrayList<>();
        REQUIRED_PERMISSIONS.add(Manifest.permission.CAMERA);
    }

    private final BarcodeScanner barcodeScanner;
    private final BarcodeImageAnalyzer imageAnalyzer;

    private final MutableLiveData<Boolean> permissionGrantingObservable = new MutableLiveData<>();
    private final MutableLiveData<ProcessCameraProvider> processCameraProvider = new MutableLiveData<>();
    private final MutableLiveData<BarcodeResult> barcodeResultObservable = new MutableLiveData<>();

    public BarcodeScannerViewModel(@NonNull Application application, @Nullable int[] formats) {
        super(application);
        imageAnalyzer = this.new BarcodeImageAnalyzer();

        if (formats == null || formats.length == 0) {
            barcodeScanner = BarcodeScanning.getClient();
        } else {
            BarcodeScannerOptions.Builder builder;
            if (formats.length == 1) {
                builder = new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(formats[0]);
            } else {
                int[] nextFormats = new int[formats.length - 1];
                System.arraycopy(formats, 1, nextFormats, 0, formats.length - 1);
                builder = new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(formats[0], nextFormats);
            }

            barcodeScanner = BarcodeScanning.getClient(builder.build());
        }

        // Request camera permissions
        if (allPermissionsGranted()) {
            processCameraProvider();
            permissionGrantingObservable.setValue(true);
        } else {
            permissionGrantingObservable.setValue(false);
        }
    }

    public void onRequestCameraPermission(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                processCameraProvider();
                permissionGrantingObservable.setValue(true);
            } else {
                permissionGrantingObservable.setValue(false);
            }
        }
    }

    public boolean allPermissionsGranted() {
        for (String requiredPermission : REQUIRED_PERMISSIONS) {
            boolean granted = ContextCompat.checkSelfPermission(getApplication(), requiredPermission)
                    == PackageManager.PERMISSION_GRANTED;
            if (!granted)
                return false;
        }

        return true;
    }

    private void processCameraProvider() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getApplication());
        cameraProviderFuture.addListener(() -> {
            try {
                final ProcessCameraProvider processCameraProvider = cameraProviderFuture.get();
                BarcodeScannerViewModel.this.processCameraProvider.setValue(processCameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error getting ProcessCameraProvider", e);
            }
        }, ContextCompat.getMainExecutor(getApplication()));
    }

    public BarcodeImageAnalyzer getImageAnalyzer() {
        return imageAnalyzer;
    }

    public LiveData<Boolean> getPermissionGrantingObservable() {
        return permissionGrantingObservable;
    }

    public LiveData<ProcessCameraProvider> getProcessCameraProvider() {
        return processCameraProvider;
    }

    public LiveData<BarcodeResult> getBarcodeResultObservable() {
        return barcodeResultObservable;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Classes
    ///////////////////////////////////////////////////////////////////////////

    private class BarcodeImageAnalyzer implements ImageAnalysis.Analyzer {

        @Override
        public void analyze(ImageProxy imageProxy) {
            final ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
            InputImage inputImage = InputImage.fromByteBuffer(buffer,
                    imageProxy.getWidth(),
                    imageProxy.getHeight(),
                    imageProxy.getImageInfo().getRotationDegrees(),
                    InputImage.IMAGE_FORMAT_NV21
            );

            // Pass image to an ML Kit Vision API
            process(inputImage);

            imageProxy.close();
        }

        private void process(InputImage image) {
            /*Task<List<Barcode>> result = */
            barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        // Task completed successfully
                        //ArrayList<BarcodeResult> barcodeResults = new ArrayList<>();
                        for (Barcode barcode : barcodes) {
                            // barcodeResults.add(BarcodeResult.fromBarcode(barcode));
                            final BarcodeResult barcodeResult = BarcodeResult.fromBarcode(barcode);
                            barcodeResultObservable.setValue(barcodeResult);
                            break;
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Task failed with an exception
                        Log.e(TAG, "Error processing", e);
                    });
        }
    }

}
