package github.jomutils.android.barcode;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
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
import java.util.Arrays;
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
