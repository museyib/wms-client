package az.inci.bmsanbar.activity;

import static android.R.drawable.ic_dialog_alert;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static az.inci.bmsanbar.util.GlobalParameters.cameraScanning;
import static az.inci.bmsanbar.util.UrlConstructor.addQueryParameters;
import static az.inci.bmsanbar.util.UrlConstructor.createUrl;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import az.inci.bmsanbar.CustomException;
import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.v2.ResponseMessage;
import az.inci.bmsanbar.model.v2.ShipDocInfo;
import az.inci.bmsanbar.model.v2.UpdateDeliveryRequest;

public class ConfirmDeliveryActivity extends ScannerSupportActivity {
    private String driverCode;
    private ListView docListView;
    private EditText driverCodeEditText;
    private List<String> docList;
    private boolean docCreated;
    private String note;
    private boolean transitionFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.confirm_delivery_layout);
        setEdgeToEdge();

        driverCodeEditText = findViewById(R.id.driver);
        Button scanCam = findViewById(R.id.scan_cam);
        docListView = findViewById(R.id.ship_trx_list_view);
        ImageButton send = findViewById(R.id.send);
        Button cancel = findViewById(R.id.cancel_button);
        CheckBox transitionCheck = findViewById(R.id.transition_check);

        scanCam.setVisibility(cameraScanning ? VISIBLE : GONE);

        docList = new ArrayList<>();

        send.setOnClickListener(v -> {
            if (!docList.isEmpty())
                changeDocStatus();
        });

        docListView.setOnItemLongClickListener((parent, view, position, id) -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setMessage(R.string.want_to_delete)
                    .setPositiveButton(R.string.delete, (dialogInterface, i) -> {
                        String trxNo = (String) parent.getItemAtPosition(position);
                        docList.remove(trxNo);
                        loadData();
                    })
                    .setNegativeButton(R.string.cancel, null);
            dialogBuilder.show();
            return true;
        });

        scanCam.setVisibility(cameraScanning ? VISIBLE : GONE);
        scanCam.setOnClickListener(v -> openCameraScanner());

        cancel.setOnClickListener(v -> clearFields());
        transitionCheck.setOnCheckedChangeListener((buttonView, isChecked) -> transitionFlag = isChecked);

        loadFooter();
    }

    @Override
    public void onScanComplete(String barcode) {

        if (docCreated) getShipDetails(barcode);
        else setDriverCode(barcode);
    }

    public void setDriverCode(String driverCode) {
        if (driverCode.startsWith("PER")) {
            showProgressDialog(true);
            new Thread(() -> {
                String url = createUrl("personnel", "get-name");
                Map<String, String> parameters = new HashMap<>();
                parameters.put("per-code", driverCode);
                url = addQueryParameters(url, parameters);
                try {
                    String perName = httpClient.getSimpleObject(url, "GET", null, String.class);
                    if (perName != null)
                        runOnUiThread(() -> {
                            if (!perName.isEmpty()) {
                                this.driverCode = driverCode;
                                docCreated = true;
                                driverCodeEditText.setText(driverCode);
                                ((TextView) findViewById(R.id.driver_name)).setText(perName);
                                playSound(SOUND_SUCCESS);
                            } else {
                                showMessageDialog(getString(R.string.error), getString(R.string.driver_code_incorrect), ic_dialog_alert);
                                playSound(SOUND_FAIL);
                            }
                        });
                } catch (CustomException e) {
                    logger.logError(e.toString());
                    runOnUiThread(() -> {
                        showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert);
                        playSound(SOUND_FAIL);
                    });
                } finally {
                    runOnUiThread(() -> showProgressDialog(false));
                }
            }).start();
        } else {
            showMessageDialog(getString(R.string.error), getString(R.string.driver_code_incorrect),
                    ic_dialog_alert);
            playSound(SOUND_FAIL);
        }
    }

    public void loadData() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.simple_list_item, docList);
        docListView.setAdapter(adapter);
    }

    private void getShipDetails(String trxNo) {
        if (docList.contains(trxNo)) return;

        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("logistics", "doc-info-for-confirm");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("trx-no", trxNo);
            url = addQueryParameters(url, parameters);
            try {
                ShipDocInfo docInfo = httpClient.getSimpleObject(url, "GET", null, ShipDocInfo.class);
                runOnUiThread(() -> addDoc(trxNo, docInfo));
            } catch (CustomException e) {
                logger.logError(e.toString());
                runOnUiThread(() -> showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert));
            } finally {
                runOnUiThread(() -> showProgressDialog(false));
            }
        }).start();
    }

    private void addDoc(String trxNo, ShipDocInfo docInfo) {
        if (docInfo != null) {
            if (!driverCode.equals(docInfo.getDriverCode())) {
                showMessageDialog(getString(R.string.info),
                        getString(R.string.not_shipped_for_current_driver) +
                                "\n\nYükləndiyi sürücü  və N/V nömrəsi:\n" +
                                docInfo.getDriverName() + " - " + docInfo.getVehicleCode() +
                                "\n" + docInfo.getDeliverNotes(),
                        android.R.drawable.ic_dialog_info);
                playSound(SOUND_FAIL);
                return;
            }

            playSound(SOUND_SUCCESS);
            docList.add(trxNo);
            loadData();
        }
    }

    private void changeDocStatus() {
        showProgressDialog(true);
        new Thread(() -> {
            List<UpdateDeliveryRequest> requestList = new ArrayList<>();
            note = "İstifadəçi: " + appUser.getId();
            String url = createUrl("logistics", "confirm-shipment");
            for (String trxNo : docList) {
                UpdateDeliveryRequest request = new UpdateDeliveryRequest();
                request.setTrxNo(trxNo);
                request.setNote(note);
                request.setDeliverPerson("");
                request.setDriverCode(driverCode);
                request.setTransitionFlag(transitionFlag);
                requestList.add(request);
            }
            try {
                ResponseMessage message = httpClient.executeUpdate(url, requestList);
                runOnUiThread(() -> {
                    showMessageDialog(message.getTitle(), message.getBody(), message.getIconId());
                    if (message.getStatusCode() == 0)
                        clearFields();
                });
            } catch (CustomException e) {
                logger.logError(e.toString());
                runOnUiThread(() -> {
                    showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert);
                    playSound(SOUND_FAIL);
                });
            } finally {
                runOnUiThread(() -> showProgressDialog(false));
            }
        }).start();
    }

    private void clearFields() {
        driverCode = "";
        driverCodeEditText.setText("");
        ((TextView) findViewById(R.id.driver_name)).setText("");
        docCreated = false;
        docList.clear();
        loadData();
    }
}