package github.jomutils.android.barcode;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarCodeScannerActivity extends AppCompatActivity {

    private static final String TAG = "CameraXBasic";
    public static final String EXTRA_BARCODE_FORMATS = "extra-barcode-format";
    public static final String EXTRA_BARCODE_RESULTS = "extra-barcode-results";

    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final List<String> REQUIRED_PERMISSIONS;

    static {
        REQUIRED_PERMISSIONS = new ArrayList<>();
        REQUIRED_PERMISSIONS.add(Manifest.permission.CAMERA);
    }

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

    private PreviewView viewFinder;
    private ImageCapture imageCapture;
    private Camera camera;
    private ExecutorService cameraExecutor;
//    private DisplayManager displayManager;

    BarcodeScannerOptions barcodeScannerOptions;
    BarcodeScanner barcodeScanner;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

//        displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        cameraExecutor = Executors.newSingleThreadExecutor();

        viewFinder = findViewById(R.id.viewFinder);
        findViewById(R.id.camera_capture_button).setOnClickListener(v -> takePhoto());

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
        }

//        final int[] formats = getIntent().getIntArrayExtra(EXTRA_BARCODE_FORMATS);
//        if (formats == null) {
//            throw new IllegalArgumentException("Extra sending barcode formats is required.");
//        }
//
//        for (int format : formats) {
//
//        }

        barcodeScannerOptions =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_QR_CODE,
                                Barcode.FORMAT_CODE_128,
                                Barcode.FORMAT_CODE_39,
                                Barcode.FORMAT_CODE_93,
                                Barcode.FORMAT_CODABAR,
                                Barcode.FORMAT_ITF
                        )
                        .build();
        barcodeScanner = BarcodeScanning.getClient(/*barcodeScannerOptions*/);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {
        for (String requiredPermission : REQUIRED_PERMISSIONS) {
            boolean granted = ContextCompat.checkSelfPermission(getBaseContext(), requiredPermission)
                    == PackageManager.PERMISSION_GRANTED;
            if (!granted)
                return false;
        }

        return true;
    }

    private void takePhoto() {
        if (imageCapture == null) {
            return;
        }

        // Create time-stamped output file to hold the image
//        File photoFile = new File(getOutputFileDir(),
//                new SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg");
//        final ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();


        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                super.onCaptureSuccess(image);
                Log.i(TAG, "onCaptureSuccess: ");
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed: ${exc.message}", exception);
            }
        });

        // Set up image capture listener, which is triggered after photo has
        // been taken
//        imageCapture.takePicture(
//                outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
//                    @Override
//                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
//                        Uri savedUri = Uri.fromFile(photoFile);
//                        String msg = "Photo capture succeeded: " + savedUri;
//                        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
//                        Log.d(TAG, msg);
//                    }
//
//                    @Override
//                    public void onError(@NonNull ImageCaptureException exception) {
//                        Log.e(TAG, "Photo capture failed: ${exc.message}", exception);
//                    }
//                });

    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    final ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                    final Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(viewFinder.createSurfaceProvider());

                    CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                    imageCapture = new ImageCapture.Builder().build();

                    final ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build();
                    imageAnalysis.setAnalyzer(cameraExecutor, new BarcodeImageAnalyzer());

                    try {
                        // Unbind use cases before rebinding
                        cameraProvider.unbindAll();

                        // Bind use cases to camera
                        camera = cameraProvider.bindToLifecycle(BarCodeScannerActivity.this,
                                cameraSelector,
                                preview,
                                imageCapture,
                                imageAnalysis);

                    } catch (Exception exc) {
                        Log.e(TAG, "Use case binding failed", exc);
                    }
                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG, "Error getting ProcessCameraProvider", e);
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private File getOutputFileDir() {
        final File[] externalMediaDirs = getExternalMediaDirs();
        if (externalMediaDirs != null && externalMediaDirs.length > 0) {
            File file = new File(externalMediaDirs[0], getString(R.string.app_name));
            if (!file.exists()) {
                boolean created = file.mkdir();
                if (created) {
                    return file;
                }
            }
        }

        return getFilesDir();
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
            process(inputImage);

//            @SuppressLint("UnsafeExperimentalUsageError")
//            Image mediaImage = imageProxy.getImage();
//
//            if (mediaImage != null) {
//                InputImage image =
//                        InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
//                // Pass image to an ML Kit Vision API
//                process(image);
//            }

            imageProxy.close();
        }

        private void process(InputImage image) {
            /*Task<List<Barcode>> result = */
            barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        // Task completed successfully
                        ArrayList<BarcodeResult> barcodeResults = new ArrayList<>();
                        for (Barcode barcode : barcodes) {
//                            Rect bounds = barcode.getBoundingBox();
//                            Point[] corners = barcode.getCornerPoints();
//
//                            String rawValue = barcode.getRawValue();
//
//                            int valueType = barcode.getValueType();
//                            // See API reference for complete list of supported types
//                            switch (valueType) {
//                                case Barcode.TYPE_WIFI:
//                                    String ssid = barcode.getWifi().getSsid();
//                                    String password = barcode.getWifi().getPassword();
//                                    int type = barcode.getWifi().getEncryptionType();
//                                    break;
//                                case Barcode.TYPE_URL:
//                                    String title = barcode.getUrl().getTitle();
//                                    String url = barcode.getUrl().getUrl();
//                                    break;
//                            }

                            barcodeResults.add(BarcodeResult.fromBarcode(barcode));
                            Log.i(TAG, "process: " + barcode.getFormat());
                        }

                        if (!barcodeResults.isEmpty()) {
                            Intent data = new Intent();
                            data.putParcelableArrayListExtra(EXTRA_BARCODE_RESULTS, barcodeResults);
                            setResult(RESULT_OK, data);
                            Log.i(TAG, "Success processing");
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Task failed with an exception
                        Log.e(TAG, "Error processing", e);
                    });
        }
    }

    public static class BarcodeResult implements Parcelable {
        public String rawValue;
        public String displayValue;
        public int valueType;
        public int format;
        public Wifi wifi;
        public Url url;
        public Rect boundingBox;
        public Point[] cornerPoints;

        public BarcodeResult() {
        }

        protected BarcodeResult(Parcel in) {
            rawValue = in.readString();
            displayValue = in.readString();
            valueType = in.readInt();
            format = in.readInt();
            wifi = in.readParcelable(Wifi.class.getClassLoader());
            url = in.readParcelable(Url.class.getClassLoader());
            boundingBox = in.readParcelable(Rect.class.getClassLoader());
            cornerPoints = in.createTypedArray(Point.CREATOR);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(rawValue);
            dest.writeString(displayValue);
            dest.writeInt(valueType);
            dest.writeInt(format);
            dest.writeParcelable(wifi, flags);
            dest.writeParcelable(url, flags);
            dest.writeParcelable(boundingBox, flags);
            dest.writeTypedArray(cornerPoints, flags);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<BarcodeResult> CREATOR = new Creator<BarcodeResult>() {
            @Override
            public BarcodeResult createFromParcel(Parcel in) {
                return new BarcodeResult(in);
            }

            @Override
            public BarcodeResult[] newArray(int size) {
                return new BarcodeResult[size];
            }
        };

        public static BarcodeResult fromBarcode(Barcode barcode) {
            BarcodeResult result = new BarcodeResult();
            result.rawValue = barcode.getRawValue();
            result.valueType = barcode.getValueType();
            result.format = barcode.getFormat();
            result.displayValue = barcode.getDisplayValue();
            result.wifi = Wifi.fromBarcodeWifi(barcode.getWifi());
            result.url = Url.fromBarcodeUrl(barcode.getUrl());
            result.boundingBox = barcode.getBoundingBox();
            result.cornerPoints = barcode.getCornerPoints();

            return result;
        }

        @Override
        public String toString() {
            return "BarcodeResult{" +
                    "rawValue='" + rawValue + '\'' +
                    ", displayValue='" + displayValue + '\'' +
                    ", valueType=" + valueType +
                    ", format=" + format +
                    ", wifi=" + wifi +
                    ", url=" + url +
                    ", boundingBox=" + boundingBox +
                    ", cornerPoints=" + Arrays.toString(cornerPoints) +
                    '}';
        }

        public static class Wifi implements Parcelable {
            public final String ssid;
            public final String password;
            public final int encryptionType;

            public Wifi(String ssid, String password, int encryptionType) {
                this.ssid = ssid;
                this.password = password;
                this.encryptionType = encryptionType;
            }

            public static Wifi fromBarcodeWifi(Barcode.WiFi bcWiFi) {
                if (bcWiFi == null)
                    return null;

                return new Wifi(bcWiFi.getSsid(), bcWiFi.getPassword(), bcWiFi.getEncryptionType());
            }

            protected Wifi(Parcel in) {
                ssid = in.readString();
                password = in.readString();
                encryptionType = in.readInt();
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {
                dest.writeString(ssid);
                dest.writeString(password);
                dest.writeInt(encryptionType);
            }

            @Override
            public int describeContents() {
                return 0;
            }

            public static final Creator<Wifi> CREATOR = new Creator<Wifi>() {
                @Override
                public Wifi createFromParcel(Parcel in) {
                    return new Wifi(in);
                }

                @Override
                public Wifi[] newArray(int size) {
                    return new Wifi[size];
                }
            };

            @Override
            public String toString() {
                return "Wifi{" +
                        "ssid='" + ssid + '\'' +
                        ", password='" + password + '\'' +
                        ", encryptionType=" + encryptionType +
                        '}';
            }
        }

        public static class Url implements Parcelable {
            public final String title;
            public final String url;

            public Url(String title, String url) {
                this.title = title;
                this.url = url;
            }


            protected Url(Parcel in) {
                title = in.readString();
                url = in.readString();
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {
                dest.writeString(title);
                dest.writeString(url);
            }

            @Override
            public int describeContents() {
                return 0;
            }

            public static final Creator<Url> CREATOR = new Creator<Url>() {
                @Override
                public Url createFromParcel(Parcel in) {
                    return new Url(in);
                }

                @Override
                public Url[] newArray(int size) {
                    return new Url[size];
                }
            };

            public static Url fromBarcodeUrl(Barcode.UrlBookmark urlBookmark) {
                if (urlBookmark == null) {
                    return null;
                }

                return new Url(urlBookmark.getTitle(), urlBookmark.getUrl());
            }

            @Override
            public String toString() {
                return "Url{" +
                        "title='" + title + '\'' +
                        ", url='" + url + '\'' +
                        '}';
            }
        }
    }

}
