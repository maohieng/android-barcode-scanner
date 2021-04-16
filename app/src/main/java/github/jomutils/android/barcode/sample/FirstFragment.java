package github.jomutils.android.barcode.sample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import github.jomutils.android.barcode.R;


public class FirstFragment extends Fragment {

    private TextView textContent;
    private TextView textValue;

    private MainViewModel mainViewModel;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        textContent = view.findViewById(R.id.textview_first);
        textValue = view.findViewById(R.id.textValue);
        view.findViewById(R.id.button_first).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        mainViewModel.getBarcodeResultObservable().observe(getViewLifecycleOwner(), barcodeResult -> {
            if (barcodeResult != null) {
                textValue.setText(barcodeResult.displayValue);
                textContent.setText(barcodeResult.toString());
            }
        });
    }
}