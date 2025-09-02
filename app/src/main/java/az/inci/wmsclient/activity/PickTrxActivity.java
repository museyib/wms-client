package az.inci.wmsclient.activity;

import static android.R.drawable.ic_dialog_alert;
import static android.R.drawable.ic_dialog_info;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static az.inci.wmsclient.fragment.InvBarcodeHelper.requestInvBarcode;
import static az.inci.wmsclient.fragment.LatestMovementsHelper.showInventoryHistory;
import static az.inci.wmsclient.fragment.StringDataHelper.getStringData;
import static az.inci.wmsclient.util.GlobalParameters.cameraScanning;
import static az.inci.wmsclient.util.UrlConstructor.addQueryParameters;
import static az.inci.wmsclient.util.UrlConstructor.createUrl;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import az.inci.wmsclient.CustomException;
import az.inci.wmsclient.R;
import az.inci.wmsclient.model.InvBarcode;
import az.inci.wmsclient.model.Trx;
import az.inci.wmsclient.model.v2.CollectTrxRequest;
import az.inci.wmsclient.model.v2.ResetPickRequest;
import az.inci.wmsclient.model.v2.ResponseMessage;
import az.inci.wmsclient.util.DBHelper;

public class PickTrxActivity extends ScannerSupportActivity
        implements SearchView.OnQueryTextListener {
    private List<Trx> trxList;
    private ListView trxListView;
    private ImageButton sendButton;
    private SearchView searchView;
    private String trxNo;
    private int focusPosition;
    private boolean onFocus;
    private boolean pickedAll;
    private int activeSeconds;
    private int currentSeconds;
    private long startTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pick_trx_layout);
        setEdgeToEdge();
        sendButton = findViewById(R.id.send);
        trxListView = findViewById(R.id.trx_list);

        ImageButton scanCam = findViewById(R.id.scan_cam);
        ImageButton equateAll = findViewById(R.id.equate_all);
        ImageButton reload = findViewById(R.id.reload);
        ImageButton reset = findViewById(R.id.reset);
        CheckBox continuousCheck = findViewById(R.id.continuous_check);
        CheckBox readyCheck = findViewById(R.id.readyToSend);

        currentSeconds = 0;

        trxListView.setItemsCanFocus(true);
        sendButton.setEnabled(false);
        sendButton.setBackgroundColor(getResources().getColor(R.color.colorZeroQty, getTheme()));

        continuousCheck.setOnCheckedChangeListener((compoundButton, b) -> isContinuous = b);

        readyCheck.setOnCheckedChangeListener((compoundButton, b) -> {
            sendButton.setEnabled(b);
            sendButton.setBackgroundColor(b ? Color.GREEN : getResources().getColor(R.color.colorZeroQty, getTheme()));
        });

        if (cameraScanning) scanCam.setVisibility(View.VISIBLE);

        loadFooter();

        Intent intent = getIntent();
        trxNo = intent.getStringExtra("trxNo");
        String prevTrxNo = intent.getStringExtra("prevTrxNo");
        String pickArea = intent.getStringExtra("pickArea");
        activeSeconds = dbHelper.getPickActiveSeconds(trxNo);
        setTitle(prevTrxNo + " - " + pickArea);

        trxListView.setOnItemClickListener((parent, view, position, id) -> {
            onFocus = true;
            Trx trx = (Trx) view.getTag();
            showEditPickedQtyDialog(trx);
        });

        trxListView.setOnItemLongClickListener((parent, view, position, id) -> {
            Trx trx = (Trx) view.getTag();
            showInfoDialog(trx);
            return true;
        });

        sendButton.setOnClickListener(v -> {
            if (!trxList.isEmpty())
                if (pickedAll) sendTrx();
                else {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                    dialogBuilder.setMessage("Mallar tam yığılmayıb. Göndərmək istəyirsiniz?")
                            .setNegativeButton("Bəli", (dialogInterface, i) -> sendTrx())
                            .setPositiveButton("Xeyr", null);

                    dialogBuilder.show();
                    playSound(SOUND_FAIL);
                }
        });


        scanCam.setVisibility(cameraScanning ? VISIBLE : GONE);
        scanCam.setOnClickListener(v -> {
            Intent barcodeIntent = new Intent(PickTrxActivity.this, BarcodeScannerCamera.class);
            barcodeIntent.putExtra("serialScan", isContinuous);
            barcodeIntent.putExtra("trxNo", trxNo);
            barcodeIntent.putExtra("trxType", "pick");
            openCameraScanner(barcodeIntent);
        });

        equateAll.setOnClickListener(v -> {
            playSound(SOUND_FAIL);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setMessage("Sayları eyniləşdirmək istəyirsiniz?")
                    .setNegativeButton("Bəli", (dialogInterface, i) -> {
                        for (Trx trx : trxList) {
                            trx.setPickedQty(trx.getQty());
                            dbHelper.updatePickTrx(trx);
                        }

                        loadData();
                    })
                    .setPositiveButton("Xeyr", null);
            dialogBuilder.show();
        });

        reload.setOnClickListener(view -> {
            playSound(SOUND_FAIL);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setMessage("Sayları sıfırlamaq istəyirsiniz?")
                    .setNegativeButton("Bəli", (dialogInterface, i) -> {
                        for (Trx trx : trxList) {
                            trx.setPickedQty(0);
                            dbHelper.updatePickTrx(trx);
                        }

                        loadData();
                    })
                    .setPositiveButton("Xeyr", null);
            dialogBuilder.show();
        });

        reset.setOnClickListener(v -> {
            playSound(SOUND_FAIL);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setMessage("Sənədi geri göndərmək istəyirsiniz?")
                    .setNegativeButton("Bəli", (dialogInterface, i) -> {
                        boolean modified = false;
                        for (Trx trx : trxList) {
                            if (trx.getPickedQty() > 0) {
                                modified = true;
                                break;
                            }
                        }
                        if (modified)
                            showMessageDialog(getString(R.string.info),
                                    "Sənəddə dəyişiklik edilib, geri göndərilə bilməz!",
                                    ic_dialog_info);
                        else reset();
                    })
                    .setPositiveButton("Xeyr", null);
            dialogBuilder.show();
        });

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!searchView.isIconified())
                    searchView.setIconified(true);
                else finish();
            }
        });

        loadData();
    }

    private void showInfoDialog(Trx trx) {
        String info = trx.getNotes().replaceAll("; ", "\n");
        info = info.replaceAll("\\\\n", "\n");
        info += "\n\nÖlçü vahidi: " + trx.getUom();
        info += "\n\nBrend: " + trx.getInvBrand();
        info += "\n\nBarkodlar:" + dbHelper.barcodeList(trx.getInvCode(), DBHelper.PICK_TRX);
        AlertDialog.Builder builder = new AlertDialog.Builder(PickTrxActivity.this);
        builder.setTitle("Məlumat");
        builder.setMessage(info);
        builder.setPositiveButton("Şəkil", (dialog, which) -> {
            Intent photoIntent = new Intent(PickTrxActivity.this, PhotoActivity.class);
            photoIntent.putExtra("invCode", trx.getInvCode());
            photoIntent.putExtra("notes", trx.getNotes());
            startActivity(photoIntent);
        });
        builder.setNegativeButton("Say", (dialog, which) -> {
            String url = createUrl("inv", "qty");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("whs-code", trx.getWhsCode());
            parameters.put("inv-code", trx.getInvCode());
            url = addQueryParameters(url, parameters);
            getStringData(this, url, "Anbarda say");
        });

        builder.setNeutralButton("Tarixçə",
                (dialog, which) -> showInventoryHistory(this, trx.getInvCode(), trx.getWhsCode()));
        builder.show();
    }

    private void checkInvBarcode(InvBarcode invBarcode) {
        Trx trx = dbHelper.getPickTrxByInvCode(invBarcode.getInvCode(), trxNo);
        if (trx == null) {
            showMessageDialog(getString(R.string.info), getString(R.string.good_not_found),
                    ic_dialog_info);
            playSound(SOUND_FAIL);
        } else {
            trx.setUomFactor(invBarcode.getUomFactor());
            goToScannedItem(trx);
        }
    }

    private void goToScannedItem(Trx trx) {

        onFocus = true;
        focusPosition = trxList.indexOf(trx);
        if (trx.getPickedQty() >= trx.getQty()) {
            showMessageDialog(getString(R.string.warning), getString(R.string.already_picked),
                    ic_dialog_alert);
            playSound(SOUND_FAIL);
        } else {
            if (!isContinuous)
                showEditPickedQtyDialog(trx);
            else {
                double qty = trx.getPickedQty() + trx.getUomFactor();
                if (qty > trx.getQty()) {
//                    qty = trx.getQty();
                    showMessageDialog(getString(R.string.warning), getString(R.string.exceeded_ordered_qty),
                            ic_dialog_alert);
                    playSound(SOUND_FAIL);
                    return;
                }
                trx.setPickedQty(qty);
                dbHelper.updatePickTrx(trx);
            }
            playSound(SOUND_SUCCESS);
        }
        loadData();
    }

    @Override
    public void onScanComplete(String barcode) {
        Trx trx = dbHelper.getPickTrxByBarcode(barcode, trxNo);
        if (trx != null) goToScannedItem(trx);
        else requestInvBarcode(this, barcode, this::checkInvBarcode);
    }

    public void loadData() {
        pickedAll = true;
        trxList = dbHelper.getPickTrx(trxNo);
        TrxAdapter trxAdapter = new TrxAdapter(this, R.layout.pick_trx_item_layout, trxList);
        trxListView.setAdapter(trxAdapter);
        trxListView.setSelection(focusPosition);
        trxListView.requestFocus();
    }

    public void showEditPickedQtyDialog(Trx trx) {
        focusPosition = trxList.indexOf(trx);
        View view = getLayoutInflater().inflate(R.layout.edit_picked_qty_dialog_layout,
                findViewById(android.R.id.content), false);


        TextView invCodeView = view.findViewById(R.id.inv_code);
        TextView invNameView = view.findViewById(R.id.inv_name);
        EditText qtyEdit = view.findViewById(R.id.qty);
        EditText pickedQtyEdit = view.findViewById(R.id.picked_qty);

        invCodeView.setText(trx.getInvCode());
        invNameView.setText(trx.getInvName());

        qtyEdit.setText(decimalFormat.format(trx.getQty()));
        qtyEdit.setEnabled(false);

        pickedQtyEdit.setText(decimalFormat.format(trx.getPickedQty()));
        pickedQtyEdit.selectAll();

        invNameView.setOnClickListener(view1 -> showInfoDialog(trx));

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(view)
                .setPositiveButton(R.string.ok, (dialog1, which) -> {
                    double pickedQty;
                    if (!pickedQtyEdit.getText().toString().isEmpty())
                        pickedQty = Double.parseDouble(pickedQtyEdit.getText().toString());
                    else pickedQty = -1;

                    if (pickedQty < 0 || pickedQty > trx.getQty()) {
                        Toast.makeText(this, R.string.quantity_not_correct, Toast.LENGTH_LONG)
                                .show();
                        showEditPickedQtyDialog(trx);
                    } else {
                        trx.setPickedQty(pickedQty);
                        dbHelper.updatePickTrx(trx);
                        loadData();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog1, which) -> dialog1.dismiss())
                .setNeutralButton(R.string.equate, (dialog1, which) -> {
                    trx.setPickedQty(trx.getQty());
                    dbHelper.updatePickTrx(trx);
                    loadData();
                });
        AlertDialog dialog = dialogBuilder.create();

        Objects.requireNonNull(dialog.getWindow())
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setOnQueryTextListener(this);
            searchView.setActivated(true);
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        TrxAdapter adapter = (TrxAdapter) trxListView.getAdapter();
        if (adapter != null) adapter.getFilter().filter(newText);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
        startTime = System.currentTimeMillis();
    }

    @Override
    protected void onPause() {
        super.onPause();
        currentSeconds += (int) ((System.currentTimeMillis() - startTime) / 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activeSeconds += currentSeconds;
        dbHelper.updatePickActiveSeconds(trxNo, activeSeconds);
    }

    private void sendTrx() {
        showProgressDialog(true);
        List<CollectTrxRequest> requestList = new ArrayList<>();
        currentSeconds += (int) ((System.currentTimeMillis() - startTime) / 1000);
        activeSeconds += currentSeconds;
        for (Trx trx : trxList) {
            CollectTrxRequest request = new CollectTrxRequest();
            request.setTrxId(trx.getTrxId());
            request.setQty(trx.getPickedQty());
            request.setSeconds(activeSeconds);
            request.setPickStatus("A");
            request.setDeviceId(getDeviceIdString());
            requestList.add(request);
        }

        String url = createUrl("pick", "collect");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("trx-no", null);
        url = addQueryParameters(url, parameters);
        try {
            ResponseMessage message = httpClient.executeUpdate(url, requestList);
            runOnUiThread(() -> {
                if (message.getStatusCode() == 0) {
                    dbHelper.deletePickTrx(trxNo);
                    finish();
                    showMessageDialog(message.getTitle(), message.getBody(), message.getIconId());
                } else
                    showMessageDialog(message.getTitle(), message.getBody(), message.getIconId());
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
    }

    private void reset() {
        showProgressDialog(true);
        String url = createUrl("pick", "reset");
        ResetPickRequest request = new ResetPickRequest();
        request.setTrxNo(trxNo);
        request.setUserId(appUser.getId());
        try {
            ResponseMessage message = httpClient.executeUpdate(url, request);
            runOnUiThread(() -> {
                if (message.getStatusCode() == 0) {
                    dbHelper.deletePickTrx(trxNo);
                    finish();
                } else {
                    showMessageDialog(message.getTitle(), message.getBody(), message.getIconId());
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
    }

    class TrxAdapter extends ArrayAdapter<Trx> implements Filterable {
        PickTrxActivity activity;
        List<Trx> list;

        TrxAdapter(@NonNull Context context, int resourceId, @NonNull List<Trx> objects) {
            super(context, resourceId, objects);
            list = objects;
            activity = (PickTrxActivity) context;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Trx trx = list.get(position);
            if (convertView == null)
                convertView = getLayoutInflater()
                        .inflate(R.layout.pick_trx_item_layout, parent, false);

            if (position == activity.focusPosition && activity.onFocus)
                convertView.setBackgroundColor(Color.LTGRAY);
            else convertView.setBackgroundColor(Color.TRANSPARENT);

            if (trx.getPickedQty() == 0)
                convertView.setBackgroundColor(
                        activity.getResources().getColor(R.color.colorZeroQty, getTheme()));
            else if (trx.getPickedQty() < trx.getQty())
                convertView.setBackgroundColor(Color.YELLOW);

            TextView invCode = convertView.findViewById(R.id.inv_code);
            TextView invName = convertView.findViewById(R.id.inv_name);
            TextView invBrand = convertView.findViewById(R.id.inv_brand);
            TextView qty = convertView.findViewById(R.id.qty);
            TextView pickedQty = convertView.findViewById(R.id.picked);

            invCode.setText(trx.getInvCode());
            invName.setText(trx.getInvName());
            invBrand.setText(trx.getInvBrand());
            qty.setText(activity.decimalFormat.format(trx.getQty()));
            pickedQty.setText(activity.decimalFormat.format(trx.getPickedQty()));
            convertView.setTag(trx);

            if (trx.getPickedQty() < trx.getQty()) activity.pickedAll = false;

            return convertView;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    List<Trx> filteredArrayData = new ArrayList<>();
                    constraint = constraint.toString().toLowerCase();

                    for (Trx trx : activity.trxList) {
                        if (trx.getInvCode()
                                .concat(trx.getInvName())
                                .concat(trx.getBarcode())
                                .toLowerCase()
                                .contains(constraint)) filteredArrayData.add(trx);
                    }

                    results.count = filteredArrayData.size();
                    results.values = filteredArrayData;
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    list = (List<Trx>) results.values;
                    notifyDataSetChanged();
                }
            };
        }
    }
}
