package github.jomutils.android.barcode.sample4;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.tasks.TaskExecutors;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import github.jomutils.android.barcode.BarcodeImageAnalyzer;
import github.jomutils.android.barcode.BarcodeResult;
import github.jomutils.android.barcode.CameraHelper;
import github.jomutils.android.barcode.ScopedExecutor;
import github.jomutils.android.barcode.WorkflowState;

public class ScanningViewModel extends AndroidViewModel {

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
            return (T) new ScanningViewModel(application, formats);
        }
    }

    private static final String TAG = "BarcodeScannerViewModel";

    public static final int REQUEST_CODE_PERMISSIONS = 10;
    public static final List<String> REQUIRED_PERMISSIONS;

    static {
        REQUIRED_PERMISSIONS = new ArrayList<>();
        REQUIRED_PERMISSIONS.add(Manifest.permission.CAMERA);
    }

    private final CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    private Preview cameraPreview;

    private final ExecutorService analyzeExecutor;
    private final ScopedExecutor mainScopeExecutor;

    private final BarcodeScanner barcodeScanner;
    private final BarcodeImageAnalyzer imageAnalyzer;

    private final MutableLiveData<WorkflowState> workflowState = new MutableLiveData<>(WorkflowState.NOT_STARTED);

    private final MutableLiveData<Boolean> permissionGrantingObservable = new MutableLiveData<>();
    private final MutableLiveData<ProcessCameraProvider> processCameraProvider = new MutableLiveData<>();
    private final MutableLiveData<List<Barcode>> allBarcodesObservable = new MutableLiveData<>();
    private final MutableLiveData<BarcodeResult> detectedBarcode = new MutableLiveData<>();

    public ScanningViewModel(@NonNull Application application, @Nullable int[] formats) {
        super(application);
        analyzeExecutor = Executors.newSingleThreadExecutor();

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

        mainScopeExecutor = new ScopedExecutor(TaskExecutors.MAIN_THREAD);
        imageAnalyzer = new BarcodeImageAnalyzer(barcodeScanner, mainScopeExecutor) {

            @Override
            public void onProceed(List<Barcode> barcodes) {
                allBarcodesObservable.setValue(barcodes);
            }

            @Override
            public void onProcessFail(Exception e) {
                Log.e(TAG, "onProcessFail: ", e);
            }
        };

        // Request camera permissions
        if (allPermissionsGranted()) {
            processCameraProvider();
            permissionGrantingObservable.setValue(true);
        } else {
            permissionGrantingObservable.setValue(false);
        }
    }

    @Override
    protected void onCleared() {
        analyzeExecutor.shutdown();
        mainScopeExecutor.shutdown();
        barcodeScanner.close();
        super.onCleared();
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
                ScanningViewModel.this.processCameraProvider.setValue(processCameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error getting ProcessCameraProvider", e);
            }
        }, ContextCompat.getMainExecutor(getApplication()));
    }

    public Camera startCamera(ProcessCameraProvider processCameraProvider,
                              LifecycleOwner owner,
                              PreviewView previewView,
                              Size analyzeSize) {
        workflowState.setValue(WorkflowState.DETECTING);

        // Create a Preview
        DisplayMetrics displayMetrics = new DisplayMetrics();
        previewView.getDisplay().getRealMetrics(displayMetrics);

        final int aspectRatio = CameraHelper.getAspectRatio(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels
        );
        final int rotation = previewView.getDisplay().getRotation();

        final ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                /*.setTargetAspectRatio(aspectRatio)*/
                .setTargetRotation(rotation)
                .setImageQueueDepth(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(analyzeSize)
                .build();
        imageAnalysis.setAnalyzer(analyzeExecutor, imageAnalyzer);

        Preview.Builder previewBuilder = setupPreviewBuilder(previewView, aspectRatio, rotation);

//        setBokehEffect(previewBuilder, cameraSelector);

        cameraPreview = previewBuilder.build();
        cameraPreview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageCapture imageCapture = setupImageCapture();

        return processCameraProvider.bindToLifecycle(
                owner,
                cameraSelector,
                cameraPreview,
                imageCapture,
                imageAnalysis
        );
    }

    public void freezeCamera() {
        final ProcessCameraProvider value = processCameraProvider.getValue();
        if (value != null) {
            value.unbind(cameraPreview);
        }
    }

    // TODO: 4/19/21 test this method
    public void unFreezeCamera(LifecycleOwner lifecycleOwner) {
        final ProcessCameraProvider value = processCameraProvider.getValue();
        if (value != null && !value.isBound(cameraPreview)) {
            value.bindToLifecycle(lifecycleOwner, cameraSelector, cameraPreview);
        }
    }

    private Preview.Builder setupPreviewBuilder(PreviewView previewView,
                                                int aspectRatio,
                                                int rotation) {
        return new Preview.Builder()
                .setTargetAspectRatio(aspectRatio)
                .setTargetRotation(rotation);
    }

    private ImageCapture setupImageCapture() {
        return new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();
    }

    public void setWorkflowState(WorkflowState state) {
        final WorkflowState value = workflowState.getValue();
        if (value == null || value != state) {
            workflowState.setValue(state);
        }
    }

    public void setDetectedBarcode(Barcode barcode) {
        final BarcodeResult barcodeResult = BarcodeResult.fromBarcode(barcode);
//        final BarcodeResult value = detectedBarcode.getValue();
//        if (value == null || !value.equals(barcodeResult)) {
        detectedBarcode.setValue(barcodeResult);
//        }
    }

    public LiveData<Boolean> getPermissionGrantingObservable() {
        return permissionGrantingObservable;
    }

    public LiveData<ProcessCameraProvider> getProcessCameraProvider() {
        return processCameraProvider;
    }

    public LiveData<List<Barcode>> getAllBarcodesObservable() {
        return allBarcodesObservable;
    }

    public LiveData<BarcodeResult> getDetectedBarcodeResult() {
        return detectedBarcode;
    }

    public LiveData<WorkflowState> getWorkflowState() {
        return workflowState;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Classes
    ///////////////////////////////////////////////////////////////////////////

}
