package az.inci.wmsclient.activity;

import android.os.Bundle;

import az.inci.wmsclient.R;

public class SettingsActivity extends AppBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setEdgeToEdge();
    }
}