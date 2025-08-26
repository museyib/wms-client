package az.inci.bmsanbar.fragment;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import az.inci.bmsanbar.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
