package az.inci.wmsclient.activity;

import static android.R.drawable.ic_dialog_alert;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static az.inci.wmsclient.util.GlobalParameters.cameraScanning;
import static az.inci.wmsclient.util.UrlConstructor.addQueryParameters;
import static az.inci.wmsclient.util.UrlConstructor.createUrl;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import az.inci.wmsclient.CustomException;
import az.inci.wmsclient.R;
import az.inci.wmsclient.model.Inventory;
import az.inci.wmsclient.model.v2.ResponseMessage;

public class EditShelfActivity extends ScannerSupportActivity {
    private final List<Inventory> inventoryList = new ArrayList<>();
    private EditText shelfBarcodeEdit;
    private ListView invListView;
    private String shelfBarcode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_shelf);
        setEdgeToEdge();

        shelfBarcodeEdit = findViewById(R.id.shelf_barcode);
        invListView = findViewById(R.id.inv_list_view);

        Button scanCam = findViewById(R.id.scan_cam);
        ImageButton sendBtn = findViewById(R.id.send);
        ImageButton clearBtn = findViewById(R.id.clear);

        scanCam.setVisibility(cameraScanning ? VISIBLE : GONE);
        scanCam.setOnClickListener(v -> openCameraScanner());

        sendBtn.setOnClickListener(v -> {
            if (!inventoryList.isEmpty()) uploadData();
        });

        clearBtn.setOnClickListener(v -> clearAndRefreshList());

        invListView.setOnItemLongClickListener((parent, view, position, id) -> {
            Inventory item = (Inventory) parent.getItemAtPosition(position);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setMessage("Silmək istəyirsinizmi? " + item)
                    .setPositiveButton("Bəli", (dialog, which) -> deleteAndRefreshList(item))
                    .setNegativeButton("Xeyr", null);
            dialogBuilder.create().show();
            return true;
        });

        invListView.setOnItemClickListener((parent, view, position, id) -> {
            Inventory item = (Inventory) parent.getItemAtPosition(position);
            AlertDialog alertDialog = new AlertDialog.Builder(this).setMessage(
                    "Barkod: " + item.getBarcode()).setNeutralButton("OK", null).create();

            alertDialog.show();
        });

        loadFooter();
    }

    @Override
    public void onScanComplete(String barcode) {
        if (barcode != null) {
            if (shelfBarcode.isEmpty()) {
                if (!barcode.startsWith("#")) {
                    showMessageDialog(getString(R.string.info), "Vitrin barkodu daxil etməlisiniz!",
                            ic_dialog_alert);
                    return;
                }
                shelfBarcode = barcode;
                shelfBarcodeEdit.setText(shelfBarcode);
            } else {
                if (barcode.startsWith("#")) {
                    showMessageDialog(getString(R.string.info), "Mal barkodu daxil etməlisiniz!",
                            ic_dialog_alert);
                    return;
                }
                getDataByBarcode(barcode);
            }
        }
    }

    private void addAndRefreshList(Inventory inventory) {
        if (!inventoryList.contains(inventory)) inventoryList.add(inventory);
        ArrayAdapter<Inventory> adapter = new ArrayAdapter<>(this, R.layout.simple_list_item,
                inventoryList);
        invListView.setAdapter(adapter);
    }

    private void deleteAndRefreshList(Inventory inventory) {
        inventoryList.remove(inventory);
        ArrayAdapter<Inventory> adapter = new ArrayAdapter<>(this, R.layout.simple_list_item,
                inventoryList);
        invListView.setAdapter(adapter);
    }

    private void clearAndRefreshList() {
        shelfBarcodeEdit.setText("");
        inventoryList.clear();
        shelfBarcode = "";
        ArrayAdapter<Inventory> adapter = new ArrayAdapter<>(this, R.layout.simple_list_item,
                inventoryList);
        invListView.setAdapter(adapter);
    }

    private void getDataByBarcode(String barcode) {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("inv", "by-barcode");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("barcode", barcode);
            url = addQueryParameters(url, parameters);
            try {
                Inventory inventory = httpClient.getSimpleObject(url, "GET", null, Inventory.class);
                if (inventory != null) {
                    runOnUiThread(() -> {
                        if (inventory.getInvCode() == null) {
                            showMessageDialog(getString(R.string.error),
                                    getString(R.string.good_not_found),
                                    ic_dialog_alert);
                            playSound(SOUND_FAIL);
                        } else {
                            addAndRefreshList(inventory);
                            playSound(SOUND_SUCCESS);
                        }
                    });
                }
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

    private void uploadData() {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("inv", "update-shelf-barcode");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("whs-code", appUser.getWhsCode());
            parameters.put("shelf-barcode", shelfBarcode.replaceFirst("#", "%23"));
            url = addQueryParameters(url, parameters);

            List<String> barcodeList = new ArrayList<>();
            for (Inventory inventory : inventoryList) {
                barcodeList.add(inventory.getBarcode());
            }
            try {
                ResponseMessage message = httpClient.executeUpdate(url, barcodeList);
                runOnUiThread(() -> showMessageDialog(message.getTitle(), message.getBody(), message.getIconId()));
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
}