package az.inci.bmsanbar.activity;

import android.os.Bundle;

import az.inci.bmsanbar.R;

public class SettingsActivity extends AppBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setEdgeToEdge();
    }
}