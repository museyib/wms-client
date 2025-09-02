package az.inci.wmsclient.activity;

import static android.R.drawable.ic_dialog_alert;
import static android.R.drawable.ic_dialog_info;
import static az.inci.wmsclient.fragment.StringDataHelper.getStringData;
import static az.inci.wmsclient.util.GlobalParameters.cameraScanning;
import static az.inci.wmsclient.util.UrlConstructor.addQueryParameters;
import static az.inci.wmsclient.util.UrlConstructor.createUrl;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import az.inci.wmsclient.CustomException;
import az.inci.wmsclient.R;
import az.inci.wmsclient.model.v2.ResponseMessage;
import az.inci.wmsclient.model.v3.PurchaseTrx;
import az.inci.wmsclient.model.v3.UpdatePurchaseTrxRequest;

public class PurchaseTrxActivity extends ScannerSupportActivity
        implements SearchView.OnQueryTextListener {
    private List<PurchaseTrx> trxList;
    private RecyclerView trxListView;
    private ImageButton sendButton;
    private String trxNo;
    private int focusPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_trx);
        setEdgeToEdge();
        sendButton = findViewById(R.id.send);
        trxListView = findViewById(R.id.trx_list);

        sendButton.setEnabled(false);
        sendButton.setBackgroundColor(getResources().getColor(R.color.colorZeroQty, getTheme()));

        ImageButton scanCam = findViewById(R.id.scan_cam);
        CheckBox continuousCheck = findViewById(R.id.continuous_check);
        CheckBox readyCheck = findViewById(R.id.readyToSend);

        continuousCheck.setOnCheckedChangeListener((compoundButton, b) -> isContinuous = b);

        readyCheck.setOnCheckedChangeListener((compoundButton, b) -> {
            sendButton.setEnabled(b);
            sendButton.setBackgroundColor(
                    b ? Color.GREEN : getResources().getColor(R.color.colorZeroQty, getTheme()));
        });

        if (cameraScanning) scanCam.setVisibility(View.VISIBLE);

        loadFooter();

        Intent intent = getIntent();
        trxNo = intent.getStringExtra("trxNo");
        setTitle(trxNo);

        loadData();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        PurchaseTrxAdapter adapter = (PurchaseTrxAdapter) trxListView.getAdapter();
        if (adapter != null)
            adapter.getFilter().filter(newText);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem item = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) item.getActionView();
        if (searchView != null) {
            searchView.setOnQueryTextListener(this);
            searchView.setActivated(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onScanComplete(String barcode) {
        PurchaseTrx trx = getByBarcode(barcode);
        if (trx != null)
            goToScannedItem(trx);

        loadData();
    }

    public void loadData() {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("purchase", "trx-list") + "?trx-no=" + trxNo;
            try {
                trxList = httpClient.getListData(url, "GET", null, PurchaseTrx[].class);
                if (trxList != null) {
                    runOnUiThread(() -> {
                        PurchaseTrxAdapter adapter = new PurchaseTrxAdapter(this, trxList);
                        trxListView.setLayoutManager(new LinearLayoutManager(this));
                        trxListView.setAdapter(adapter);
                        adapter.notifyItemRangeChanged(0, trxList.size());
                    });
                }
            } catch (CustomException e) {
                logger.logError(e.toString());
                runOnUiThread(() -> showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert));
            } finally {
                runOnUiThread(() -> showProgressDialog(false));
            }

        }).start();
    }

    private void goToScannedItem(PurchaseTrx trx) {
        focusPosition = trxList.indexOf(trx);
        if (trx.getCountedQty() >= trx.getQty()) {
            showMessageDialog(getString(R.string.info), getString(R.string.already_picked),
                    ic_dialog_info);
            playSound(SOUND_FAIL);
        } else {
            if (!isContinuous) showEditPickedQtyDialog(trx);
            else {
                double qty = trx.getCountedQty() + trx.getUomFactor();
                if (qty > trx.getQty()) qty = trx.getQty();
                trx.setCountedQty(qty);
                updatePickTrx(trx);
            }
            playSound(SOUND_SUCCESS);
        }
    }

    private void updatePickTrx(PurchaseTrx trx) {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("purchase", "update-qty");
            UpdatePurchaseTrxRequest request = new UpdatePurchaseTrxRequest();
            request.setUserId(appUser.getId());
            request.setDeviceId(getDeviceIdString());
            request.setTrxNo(trxNo);
            request.setTrxId(trx.getTrxId());
            request.setQty(trx.getCountedQty());
            try {
                ResponseMessage message = httpClient.executeUpdate(url, request);
                runOnUiThread(() -> {
                    showMessageDialog(message.getTitle(), message.getBody(), message.getIconId());
                    PurchaseTrxAdapter adapter = (PurchaseTrxAdapter) trxListView.getAdapter();
                    if (adapter != null) {
                        adapter.notifyItemChanged(focusPosition);
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
    }

    public void showEditPickedQtyDialog(PurchaseTrx trx) {
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

        pickedQtyEdit.setText(decimalFormat.format(trx.getCountedQty()));
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
                        trx.setCountedQty(pickedQty);
                        updatePickTrx(trx);
                        loadData();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog1, which) -> dialog1.dismiss())
                .setNeutralButton(R.string.equate, (dialog1, which) -> {
                    trx.setCountedQty(trx.getQty());
                    updatePickTrx(trx);
                    loadData();
                });
        AlertDialog dialog = dialogBuilder.create();

        Objects.requireNonNull(dialog.getWindow())
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private void showInfoDialog(PurchaseTrx trx) {
        Log.e("WHS_CODE", trx.getWhsCode());
        StringBuilder info = new StringBuilder();
        info.append("\n\nÖlçü vahidi: ").append(trx.getUom());
        info.append("\n\nBarkodlar:");
        for (String s : trx.getBarcodes()) {
            info.append("\n").append(s);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Məlumat");
        builder.setMessage(info.toString());
        builder.setPositiveButton("Şəkil", (dialog, which) -> {
            Intent photoIntent = new Intent(this, PhotoActivity.class);
            photoIntent.putExtra("invCode", trx.getInvCode());
            startActivity(photoIntent);
        });
        builder.setNeutralButton("Say", (dialog, which) -> {
            String url = createUrl("inv", "qty");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("inv-code", trx.getInvCode());
            parameters.put("whs-code", trx.getWhsCode());
            url = addQueryParameters(url, parameters);
            getStringData(this, url, "Anbarda say");
        });
        builder.show();
    }

    private PurchaseTrx getByBarcode(String barcode) {
        for (PurchaseTrx trx : trxList) {
            if (trx.getBarcodes().contains(barcode))
                return trx;
        }

        return null;
    }

    private class PurchaseTrxAdapter extends RecyclerView.Adapter<PurchaseTrxAdapter.ViewHolder> implements Filterable {
        private final Context context;
        private final List<PurchaseTrx> localDataList;
        private View itemView;

        public PurchaseTrxAdapter(Context context, List<PurchaseTrx> localDataList) {
            this.context = context;
            this.localDataList = localDataList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            itemView = getLayoutInflater()
                    .inflate(R.layout.purch_trx_item_layout, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PurchaseTrx trx = localDataList.get(position);
            holder.invCode.setText(trx.getInvCode());
            holder.invName.setText(trx.getInvName());
            holder.qty.setText(String.valueOf(trx.getQty()));
            holder.pickedQty.setText(String.valueOf(trx.getCountedQty()));
            itemView.setOnClickListener(v -> {
                focusPosition = localDataList.indexOf(trx);
                ((PurchaseTrxActivity) context).showEditPickedQtyDialog(trx);
            });

            itemView.setOnLongClickListener(v -> {
                ((PurchaseTrxActivity) context).showInfoDialog(trx);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return localDataList.size();
        }

        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    List<PurchaseTrx> filteredData = new ArrayList<>();
                    constraint = constraint.toString().toLowerCase();

                    for (PurchaseTrx trx : trxList) {
                        if (trx.getInvCode()
                                .concat(trx.getInvName())
                                .concat(trx.getBarcodes().toString()).contains(constraint)) {
                            filteredData.add(trx);
                        }
                    }

                    filterResults.count = filteredData.size();
                    filterResults.values = filteredData;
                    return filterResults;
                }

                /** @noinspection unchecked*/
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    List<PurchaseTrx> newList = (List<PurchaseTrx>) results.values;
                    DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                        @Override
                        public int getOldListSize() {
                            return localDataList.size();
                        }

                        @Override
                        public int getNewListSize() {
                            return newList.size();
                        }

                        @Override
                        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                            return false;
                        }

                        @Override
                        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                            return false;
                        }
                    });
                    localDataList.clear();
                    localDataList.addAll(newList);
                    result.dispatchUpdatesTo(PurchaseTrxAdapter.this);
                }
            };
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView invCode;
            TextView invName;
            TextView qty;
            TextView pickedQty;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);

                invCode = itemView.findViewById(R.id.inv_code);
                invName = itemView.findViewById(R.id.inv_name);
                qty = itemView.findViewById(R.id.qty);
                pickedQty = itemView.findViewById(R.id.counted);
            }
        }
    }
}