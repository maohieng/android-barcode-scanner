package github.jomutils.android.barcode;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.common.InputImage;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Executor;

public abstract class BarcodeImageAnalyzer implements ImageAnalysis.Analyzer {

    private final BarcodeScanner barcodeScanner;
    private final Executor listenerExecutor;

    protected BarcodeImageAnalyzer(BarcodeScanner barcodeScanner, Executor listenerExecutor) {
        this.barcodeScanner = barcodeScanner;
        this.listenerExecutor = listenerExecutor;
    }

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
                .addOnSuccessListener(listenerExecutor, this::onProceed)
                .addOnFailureListener(listenerExecutor, this::onProcessFail);
    }

    public abstract void onProceed(List<Barcode> barcodes);

    public abstract void onProcessFail(Exception e);
}
