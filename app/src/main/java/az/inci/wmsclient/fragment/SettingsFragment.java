package az.inci.wmsclient.fragment;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import az.inci.wmsclient.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
