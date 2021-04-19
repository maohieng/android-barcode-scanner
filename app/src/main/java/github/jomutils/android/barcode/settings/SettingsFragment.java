package github.jomutils.android.barcode.settings;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import github.jomutils.android.barcode.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
