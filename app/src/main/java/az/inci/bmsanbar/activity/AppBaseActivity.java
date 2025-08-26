package az.inci.bmsanbar.activity;

import static az.inci.bmsanbar.util.GlobalParameters.jwt;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import java.text.DecimalFormat;

import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.User;
import az.inci.bmsanbar.util.DBHelper;
import az.inci.bmsanbar.util.HttpClient;
import az.inci.bmsanbar.util.Logger;

public abstract class AppBaseActivity extends AppCompatActivity {
    public static int SOUND_SUCCESS = R.raw.barcodebeep;
    public static int SOUND_FAIL = R.raw.serror3;
    public static User appUser;
    private SoundPool soundPool;
    private AudioManager audioManager;
    private AlertDialog progressDialog;
    private AlertDialog.Builder dialogBuilder;
    protected HttpClient httpClient;
    protected int sound;
    protected DecimalFormat decimalFormat;
    protected SharedPreferences preferences;
    protected Logger logger;
    protected DBHelper dbHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        httpClient = HttpClient.getInstance(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        decimalFormat = new DecimalFormat();
        decimalFormat.setGroupingUsed(false);

        dbHelper = DBHelper.getInstance(this);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        soundPool = new SoundPool.Builder().setMaxStreams(10).setAudioAttributes(audioAttributes).build();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        jwt = preferences.getString("jwt", "");
        logger = new Logger(this);
    }

    public void loadFooter() {
        TextView userId = findViewById(R.id.user_info_id);
        userId.setText(appUser.getId());
        userId.append(" - ");
        userId.append(appUser.getName());
    }

    public void showProgressDialog(boolean b) {
        View view = getLayoutInflater().inflate(R.layout.progress_dialog_layout, findViewById(android.R.id.content), false);
        if (progressDialog == null)
            progressDialog = new AlertDialog.Builder(this).setView(view).setCancelable(false).create();

        if (b) progressDialog.show();
        else progressDialog.dismiss();
    }

    public void setEdgeToEdge() {
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    protected void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    public void showMessageDialog(String title, String message, int icon) {
        if (dialogBuilder == null) dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setIcon(icon).setTitle(title).setMessage(message).show();
    }

    public void playSound(int resourceId) {
        int volume = audioManager.getStreamMaxVolume(3);
        sound = soundPool.load(this, resourceId, 1);
        soundPool.setOnLoadCompleteListener((soundPool1, i, i1) -> soundPool.play(sound, volume, volume, 1, 0, 1));
    }

    protected void loadData() {}

    @SuppressLint("HardwareIds")
    public String getDeviceIdString() {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

}
