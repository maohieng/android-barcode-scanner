package github.jomutils.android.barcode.sample;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import github.jomutils.android.barcode.BarcodeResult;

public class MainViewModel extends AndroidViewModel {

    private MutableLiveData<BarcodeResult> barcodeResultObservable = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
    }

    void setBarcodeResult(BarcodeResult result) {
        this.barcodeResultObservable.setValue(result);
    }

    public LiveData<BarcodeResult> getBarcodeResultObservable() {
        return barcodeResultObservable;
    }
}
