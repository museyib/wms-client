package az.inci.wmsclient.activity;

import static android.text.TextUtils.isEmpty;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.device.ScanManager;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public abstract class ScannerSupportActivity extends AppBaseActivity {
    protected String model;
    protected boolean isContinuous = true;
    protected ScanManager scanManager;
    protected boolean busy = false;
    private final BroadcastReceiver urovoScanReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            byte[] barcodeArray = intent.getByteArrayExtra(ScanManager.DECODE_DATA_TAG);
            int length = intent.getIntExtra(ScanManager.BARCODE_LENGTH_TAG, 0);
            String barcode = new String(barcodeArray, 0, length);
            onScanComplete(barcode);
            busy = false;
        }
    };
    protected boolean isUrovoOpen = false;
    ActivityResultLauncher<Intent> barcodeResultLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        barcodeResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        result -> {
            Intent data = result.getData();
            if (data != null) {
                String barcode = data.getStringExtra("barcode");
                if (!isEmpty(barcode)) onScanComplete(barcode);
            }
        });
    }

    private void initUrovoScanner() {
        try {
            busy = false;
            isUrovoOpen = true;
            scanManager = new ScanManager();
            scanManager.openScanner();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ScanManager.ACTION_DECODE);
            ContextCompat.registerReceiver(this, urovoScanReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
        } catch (RuntimeException e) {
            logger.logError(e.toString());
        }
    }

    private void toggleUrovoScanner() {
        if (!isUrovoOpen) {
            initUrovoScanner();
        }

        if (!busy) {
            scanManager.startDecode();
            busy = true;
        } else {
            scanManager.stopDecode();
            busy = false;
        }
    }

    private void stopScan() {
        isUrovoOpen = false;
        try {
            if (scanManager != null && scanManager.getScannerState()) {
                scanManager.closeScanner();
                unregisterReceiver(urovoScanReceiver);
            }
        } catch (RuntimeException e) {
            logger.logError(e.toString());
        }
    }

    public abstract void onScanComplete(String barcode);

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 520 || keyCode == 521 || keyCode == 522) {
            toggleUrovoScanner();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopScan();
    }

    protected void openCameraScanner() {
        openCameraScanner(new Intent(this, BarcodeScannerCamera.class));
    }

    protected void openCameraScanner(Intent intent) {
        barcodeResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> barcodeResultLauncher() {
        return registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
            Intent data = result.getData();
            if (data != null) {
                String barcode = data.getStringExtra("barcode");
                if (!isEmpty(barcode)) onScanComplete(barcode);
            }
        });
    }
}