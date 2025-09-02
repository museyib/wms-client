package az.inci.wmsclient.activity;

import static android.Manifest.permission.CAMERA;
import static android.R.drawable.ic_dialog_alert;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import static az.inci.wmsclient.fragment.InvBarcodeHelper.requestInvBarcode;

import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Detector.Detections;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

import az.inci.wmsclient.R;
import az.inci.wmsclient.model.InvBarcode;
import az.inci.wmsclient.model.Trx;


public class BarcodeScannerCamera extends AppBaseActivity {
    private static final int CAMERA_PERMISSION = 200;
    Button add;
    TextView goodInfo;
    String trxType;
    boolean isContinuous;
    private CameraDevice cameraDevice;
    private SurfaceView surfaceView;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private CameraSource cameraSource;
    private CameraDevice.StateCallback stateCallback;
    private ToneGenerator toneGenerator;
    private String trxNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onScanComplete(String barcode) {
        if (!isContinuous) {
            getAndClose(barcode);
        } else {
            runOnUiThread(() -> {
                add.setEnabled(true);
                add.setText(barcode);
                add.setOnClickListener(view -> getAndContinue(barcode));
            });
        }
    }

    protected void startBackgroundThread() {
        backgroundThread = new HandlerThread("Camera Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert);
        }
    }

    private void openCamera() {
        initObjects();

        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            if (checkCameraPermission()) {
                startCameraSource();
                manager.openCamera(cameraId, stateCallback, backgroundHandler);
                startBackgroundThread();
            } else requestCameraPermission();
        } catch (CameraAccessException e) {
            showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert);
        }
    }

    private void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
            stopBackgroundThread();
        }
    }

    private void initObjects() {
        setContentView(R.layout.activity_barcode_scanner_camera);
        surfaceView = findViewById(R.id.surface_view);
        add = findViewById(R.id.increase);
        goodInfo = findViewById(R.id.good_info);

        isContinuous = getIntent().getBooleanExtra("serialScan", false);

        if (isContinuous) {
            trxNo = getIntent().getStringExtra("trxNo");
            trxType = getIntent().getStringExtra("trxType");
            add.setVisibility(View.VISIBLE);
            goodInfo.setVisibility(View.VISIBLE);
            add.setEnabled(false);
        }
        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
    }

    @Override
    protected void onResume() {
        super.onResume();
        openCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
    }

    private void startCameraSource() {
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.ALL_FORMATS).build();

        CameraSource.Builder sourceBuilder = new CameraSource.Builder(this, barcodeDetector);

        cameraSource = sourceBuilder.setRequestedPreviewSize(1920, 1080).setAutoFocusEnabled(true).build();
        Callback surfaceCallback = new Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                try {
                    if (checkCameraPermission()) cameraSource.start(surfaceView.getHolder());
                    else requestCameraPermission();
                } catch (IOException e) {
                    showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert);
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                cameraSource.stop();
            }
        };

        stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                cameraDevice = camera;
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                cameraDevice.close();
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                cameraDevice.close();
                cameraDevice = null;
            }
        };
        surfaceView.getHolder().addCallback(surfaceCallback);


        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(@NonNull Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {
                    String barcode = barcodes.valueAt(0).displayValue;
                    onScanComplete(barcode);
                }
            }
        });
    }

    private boolean checkCameraPermission() {
        return ActivityCompat.checkSelfPermission(this, CAMERA) == PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{CAMERA}, CAMERA_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION) {
            if (grantResults[0] == PERMISSION_DENIED) finish();
            else openCamera();
        }
    }

    private void getAndClose(String barcode) {
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
        Intent intent = new Intent();
        intent.putExtra("barcode", barcode);
        setResult(1, intent);
        finish();
    }

    private void getAndContinue(String barcode) {
        switch (trxType) {
            case "pick":
                getPickTrx(barcode);
                break;
            case "pack":
                getPackTrx(barcode);
                break;
        }
        add.setEnabled(false);
        add.setText(getString(R.string.increase));
    }

    private void checkInvBarcodeForPick(InvBarcode invBarcode) {
        Trx trx = dbHelper.getPickTrxByInvCode(invBarcode.getInvCode(), trxNo);
        if (trx == null) {
            showMessageDialog(getString(R.string.info), getString(R.string.good_not_found), android.R.drawable.ic_dialog_info);
            playSound(SOUND_FAIL);
        } else {
            trx.setUomFactor(invBarcode.getUomFactor());
            updateScannedItemForPick(trx);
        }
    }

    private void checkInvBarcodeForPack(InvBarcode invBarcode) {
        Trx trx = dbHelper.getPackTrxByInvCode(invBarcode.getInvCode(), trxNo);
        if (trx == null) {
            showMessageDialog(getString(R.string.info), getString(R.string.good_not_found), android.R.drawable.ic_dialog_info);
            playSound(SOUND_FAIL);
        } else {
            trx.setUomFactor(invBarcode.getUomFactor());
            updateScannedItemForPack(trx);
        }
    }

    private void getPickTrx(String barcode) {
        Trx trx = dbHelper.getPickTrxByBarcode(barcode, trxNo);
        if (trx != null) {
            updateScannedItemForPick(trx);
        } else {
            requestInvBarcode(this, barcode, this::checkInvBarcodeForPick);
        }
    }

    private void getPackTrx(String barcode) {
        Trx trx = dbHelper.getPackTrxByBarcode(barcode, trxNo);
        if (trx != null) {
            updateScannedItemForPack(trx);
        } else {
            requestInvBarcode(this, barcode, this::checkInvBarcodeForPack);
        }
    }

    private void updateScannedItemForPick(Trx trx) {
        if (trx.getPickedQty() >= trx.getQty()) {
            showMessageDialog(getString(R.string.info), getString(R.string.already_picked), android.R.drawable.ic_dialog_info);
            playSound(SOUND_FAIL);
        } else {
            double qty = trx.getPickedQty() + trx.getUomFactor();
            if (qty > trx.getPickedQty()) {
                qty = trx.getQty();
            }
            trx.setPickedQty(qty);
            dbHelper.updatePickTrx(trx);
            playSound(SOUND_SUCCESS);
        }
        goodInfo.setText(String.format("%s: %s", trx.getInvName(), trx.getPickedQty()));
    }

    private void updateScannedItemForPack(Trx trx) {
        if (trx.getPackedQty() >= trx.getQty()) {
            showMessageDialog(getString(R.string.info), getString(R.string.already_packed), android.R.drawable.ic_dialog_info);
            playSound(SOUND_FAIL);
        } else {
            double qty = trx.getPackedQty() + trx.getUomFactor();
            if (qty > trx.getQty()) {
                qty = trx.getQty();
            }
            trx.setPackedQty(qty);
            dbHelper.updatePackTrx(trx);
            playSound(SOUND_SUCCESS);
        }
        goodInfo.setText(String.format("%s: %s", trx.getInvName(), trx.getPackedQty()));
    }
}