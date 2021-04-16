package github.jomutils.android.barcode.sample;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import github.jomutils.android.barcode.BarCodeScannerActivity;
import github.jomutils.android.barcode.BarcodeScannerViewModel;
import github.jomutils.android.barcode.R;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final int REQUEST_CODE_START_SCANNER = 11;

    private NavHostFragment navHostFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScanning();
            }
        });

        navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_START_SCANNER) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    BarcodeScannerViewModel.BarcodeResult barcodeResult = data.getParcelableExtra(BarCodeScannerActivity.EXTRA_BARCODE_RESULT);

                    if (barcodeResult != null) {
                        final FirstFragment firstFragment = (FirstFragment) navHostFragment
                                .getChildFragmentManager()
                                .getFragments()
                                .get(0);

                        firstFragment.updateContent(barcodeResult.toString());
                    }
                }
            }
        }
    }

    private void startScanning() {
        Intent intent = new Intent(this, BarCodeScannerActivity.class);
        startActivityForResult(intent, REQUEST_CODE_START_SCANNER);
    }
}