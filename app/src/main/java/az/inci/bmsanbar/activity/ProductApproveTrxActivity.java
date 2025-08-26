package az.inci.bmsanbar.activity;

import static android.R.drawable.ic_dialog_alert;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static az.inci.bmsanbar.util.GlobalParameters.cameraScanning;
import static az.inci.bmsanbar.util.UrlConstructor.addQueryParameters;
import static az.inci.bmsanbar.util.UrlConstructor.createUrl;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import az.inci.bmsanbar.AppConfig;
import az.inci.bmsanbar.CustomException;
import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.Doc;
import az.inci.bmsanbar.model.Inventory;
import az.inci.bmsanbar.model.Trx;
import az.inci.bmsanbar.model.v2.ProductApproveRequest;
import az.inci.bmsanbar.model.v2.ProductApproveRequestItem;
import az.inci.bmsanbar.model.v2.ResponseMessage;
import az.inci.bmsanbar.util.DBHelper;
import az.inci.bmsanbar.util.PrinterHelper;

public class ProductApproveTrxActivity extends ScannerSupportActivity {
    private PrinterHelper printerHelper;
    private RecyclerView trxListView;
    private SearchView searchView;
    private List<Trx> trxList;
    private List<Inventory> invList;
    private boolean docCreated;
    private String trxNo;
    private String notes = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_approve_trx_layout);
        setEdgeToEdge();
        printerHelper = new PrinterHelper(this);
        trxListView = findViewById(R.id.trx_list_view);

        ImageButton selectInvBtn = findViewById(R.id.inv_list);
        ImageButton uploadBtn = findViewById(R.id.send);
        ImageButton printBtn = findViewById(R.id.print);
        ImageButton scanCam = findViewById(R.id.scan_cam);
        EditText notesEdit = findViewById(R.id.doc_description);

        int mode = getIntent().getIntExtra("mode", AppConfig.VIEW_MODE);
        if (mode == AppConfig.VIEW_MODE) {
            docCreated = true;
            trxNo = getIntent().getStringExtra("trxNo");
            notes = getIntent().getStringExtra("notes");
            notesEdit.setText(notes);
        } else
            trxNo = new SimpleDateFormat("yyyyMMddhhmmss", Locale.getDefault()).format(new Date());

        printBtn.setOnClickListener(v -> {
            if (!trxList.isEmpty()) {
                String report = getPrintForm();
                showProgressDialog(true);
                printerHelper.print(report);
            }
        });

        selectInvBtn.setOnClickListener(v -> {
            if (invList == null) getInvList();
            else showInvList();
        });

        uploadBtn.setOnClickListener(v -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                    ProductApproveTrxActivity.this);
            dialogBuilder.setMessage("Göndərmək istəyirsiniz?")
                    .setPositiveButton("Bəli", (dialog1, which) -> {
                        int status = appUser.isApprovePrdFlag() ? 0 : 2;
                        uploadDoc(status);
                    })
                    .setNegativeButton("Xeyr", null);
            dialogBuilder.show();
        });

        scanCam.setVisibility(cameraScanning ? VISIBLE : GONE);
        Intent intent = new Intent(this, BarcodeScannerCamera.class);
        intent.putExtra("serialScan", false);
        scanCam.setOnClickListener(v -> openCameraScanner());

        notesEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                notes = s.toString();
                ContentValues values = new ContentValues();
                values.put(DBHelper.NOTES, notes);
                dbHelper.updateApproveDoc(trxNo, values);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
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

        loadFooter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.pick_menu, menu);

        menu.findItem(R.id.inv_attributes).setVisible(false);
        menu.findItem(R.id.pick_report).setVisible(false);
        menu.findItem(R.id.doc_list).setVisible(false);

        MenuItem searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    ((TrxAdapter) Objects.requireNonNull(trxListView.getAdapter())).getFilter()
                            .filter(newText);
                    return true;
                }
            });
        }
        return true;
    }

    private void createNewDoc() {
        Doc doc = new Doc();
        doc.setTrxNo(trxNo);
        doc.setTrxDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        dbHelper.addProductApproveDoc(doc);
        docCreated = true;
        setTitle(trxNo);
    }

    public void loadData() {
        setTitle(trxNo);
        trxList = dbHelper.getApproveTrxList(trxNo);
        TrxAdapter adapter = new TrxAdapter(this, trxList);
        trxListView.setLayoutManager(new LinearLayoutManager(this));
        trxListView.setAdapter(adapter);

        if (trxList.isEmpty()) findViewById(R.id.trx_list_scroll).setVisibility(View.GONE);
        else findViewById(R.id.trx_list_scroll).setVisibility(View.VISIBLE);
    }

    @Override
    public void onScanComplete(String barcode) {
        getInvFromServer(barcode);
    }

    private void getInvFromServer(String barcode) {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("inv", "by-barcode");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("barcode", barcode);
            url = addQueryParameters(url, parameters);
            try {
                Inventory currentInv = httpClient.getSimpleObject(url, "GET", null, Inventory.class);
                if (currentInv != null) runOnUiThread(() -> showAddInvDialog(currentInv));
            } catch (CustomException e) {
                logger.logError(e.toString());
                runOnUiThread(() -> showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert));
            } finally {
                runOnUiThread(() -> showProgressDialog(false));
            }
        }).start();
    }

    private void getInvList() {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("inv", "by-user-producer-list");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("user-id", appUser.getId());
            url = addQueryParameters(url, parameters);
            try {
                invList = httpClient.getListData(url, "GET", null, Inventory[].class);
                runOnUiThread(() -> {
                    if (invList != null) showInvList();
                });
            } catch (CustomException e) {
                logger.logError(e.toString());
                runOnUiThread(() -> showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert));
            } finally {
                runOnUiThread(() -> showProgressDialog(false));
            }
        }).start();
    }

    private void showInvList() {
        View view = getLayoutInflater().inflate(R.layout.result_list_dialog, findViewById(android.R.id.content), false);
        ListView listView = view.findViewById(R.id.result_list);
        InventoryAdapter adapter = new InventoryAdapter(this, R.layout.inv_list_item, invList);
        SearchView searchView = view.findViewById(R.id.search);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(
                (parent, view1, position, id) -> showAddInvDialog((Inventory) view1.getTag()));
        listView.setOnItemLongClickListener((parent, view1, position, id) -> {
            showInfoDialog((Inventory) view1.getTag());
            return true;
        });
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Mallar")
                .setView(view)
                .create();
        dialog.show();
    }

    private void showAddInvDialog(Inventory inventory) {
        if (inventory.getInvCode() == null)
            showMessageDialog(getString(R.string.info), getString(R.string.good_not_found),
                    android.R.drawable.ic_dialog_info);
        else {
            EditText qtyEdit = new EditText(this);
            qtyEdit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            qtyEdit.requestFocus();
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(inventory.getInvCode())
                    .setMessage(inventory.getInvName())
                    .setCancelable(false)
                    .setView(qtyEdit)
                    .setPositiveButton("OK", (dialog1, which) -> {
                        try {
                            double qty = Double.parseDouble(qtyEdit.getText().toString());
                            Trx containingTrx = containingTrx(inventory.getInvCode());
                            if (containingTrx.getTrxId() > 0)
                                updateApproveTrxQty(containingTrx,
                                        qty + containingTrx.getQty());
                            else {
                                Trx trx = Trx.parseFromInv(inventory);
                                trx.setQty(qty);
                                trx.setTrxNo(trxNo);
                                addApproveTrx(trx);
                            }
                            loadData();
                        } catch (NumberFormatException e) {
                            showAddInvDialog(inventory);
                        }
                    })
                    .setNegativeButton("Ləğv et", null);
            AlertDialog dialog = dialogBuilder.create();

            Objects.requireNonNull(dialog.getWindow())
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            dialog.show();
        }
    }

    private void showEditInvDialog(Trx trx) {
        if (trx == null)
            showMessageDialog(getString(R.string.info), getString(R.string.good_not_found),
                    android.R.drawable.ic_dialog_info);
        else {
            EditText qtyEdit = new EditText(this);
            qtyEdit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            qtyEdit.setText(String.format(Locale.getDefault(), "%.0f", trx.getQty()));
            qtyEdit.selectAll();
            qtyEdit.requestFocus();
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(trx.getInvCode())
                    .setMessage(trx.getInvName())
                    .setCancelable(false)
                    .setView(qtyEdit)
                    .setPositiveButton("OK", (dialog1, which) -> {
                        try {
                            double qty = Double.parseDouble(qtyEdit.getText().toString());
                            updateApproveTrxQty(trx, qty);
                            loadData();
                        } catch (NumberFormatException e) {
                            showEditInvDialog(trx);
                        }
                    })
                    .setNegativeButton("Ləğv et", null);
            AlertDialog dialog = dialogBuilder.create();

            Objects.requireNonNull(dialog.getWindow())
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            dialog.show();
        }
    }

    private void addApproveTrx(Trx trx) {
        dbHelper.addApproveTrx(trx);
        if (!docCreated) createNewDoc();
        loadData();
    }

    private void updateApproveTrxQty(Trx trx, double qty) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.QTY, qty);
        dbHelper.updateApproveTrx(String.valueOf(trx.getTrxId()), values);
    }

    private Trx containingTrx(String invCode) {
        for (Trx trx : trxList) {
            if (trx.getInvCode().equals(invCode) && !trx.isReturned())
                return trx;
        }

        return new Trx();
    }

    private void uploadDoc(int status) {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("inv-move", "approve-prd", "insert");
            ProductApproveRequest request = new ProductApproveRequest();
            request.setTrxNo(trxNo);
            request.setTrxDate(new java.sql.Date(System.currentTimeMillis()).toString());
            request.setStatus(status);
            request.setNotes(notes);
            request.setUserId(appUser.getId());
            List<ProductApproveRequestItem> requestItems = new ArrayList<>();
            for (Trx trx : trxList) {
                ProductApproveRequestItem requestItem = new ProductApproveRequestItem();
                requestItem.setInvCode(trx.getInvCode());
                requestItem.setInvName(trx.getInvName());
                requestItem.setInvBrand(trx.getInvBrand());
                requestItem.setBarcode(trx.getBarcode());
                requestItem.setQty(trx.getQty());
                requestItems.add(requestItem);
            }
            request.setRequestItems(requestItems);

            try {
                ResponseMessage message = httpClient.executeUpdate(url, request);
                runOnUiThread(() -> {
                    if (message.getStatusCode() == 0) {
                        dbHelper.deleteApproveDoc(trxNo);
                        finish();
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

    private String getPrintForm() {
        String html = "<html><head><style>*{margin:0px; padding:0px}" +
                "table,tr,th,td{border: 1px solid black;border-collapse: collapse; font-size: 12px}" +
                "th{background-color: #636d72;color:white}td,th{padding:0 4px 0 4px}</style>" +
                "</head><body>";
        html = html.concat("<h3 style='text-align: center'>İstehsaldan mal qəbulu</h3></br>");
        html = html.concat("<h4>" + notes + "</h4></br>");
        html = html.concat("<table>");
        html = html.concat("<tr><th>Mal kodu</th>");
        html = html.concat("<th>Mal adı</th>");
        html = html.concat("<th>Barkod</th>");
        html = html.concat("<th>Brend</th>");
        html = html.concat("<th>İç sayı</th>");
        html = html.concat("<th>Miqdar</th>");
        trxList.sort(Comparator.comparing(Trx::getInvCode));
        for (Trx trx : trxList) {

            html = html.concat("<tr><td>" + trx.getInvCode() + "</td>");
            html = html.concat("<td>" + trx.getInvName() + "</td>");
            html = html.concat("<td>" + trx.getBarcode() + "</td>");
            html = html.concat("<td>" + trx.getInvBrand() + "</td>");
            html = html.concat("<td>" + trx.getNotes() + "</td>");
            html = html.concat("<td style='text-align: right'>" + trx.getQty() + "</td></tr>");
        }

        html = html.concat("</table></body></head>");

        return html;
    }

    private void showInfoDialog(Inventory inventory) {
        String info = inventory.getInvName();
        info += "\n\nBrend: " + inventory.getInvBrand();
        info += "\n\nİç sayı: " + inventory.getInternalCount();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Məlumat");
        builder.setMessage(info);
        builder.setPositiveButton("Şəkil", (dialog, which) -> {
            Intent photoIntent = new Intent(this, PhotoActivity.class);
            photoIntent.putExtra("invCode", inventory.getInvCode());
            startActivity(photoIntent);
        });
        builder.show();
    }

    private class InventoryAdapter extends ArrayAdapter<Inventory> implements Filterable {
        List<Inventory> list;
        ProductApproveTrxActivity activity;
        private final int resourceId;

        public InventoryAdapter(@NonNull Context context, int resourceId, @NonNull List<Inventory> objects) {
            super(context, resourceId, objects);
            list = objects;
            activity = (ProductApproveTrxActivity) context;
            this.resourceId = resourceId;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Inventory inventory = list.get(position);
            if (convertView == null)
                convertView = getLayoutInflater().inflate(resourceId, parent, false);
            TextView invCode = convertView.findViewById(R.id.inv_code);
            TextView invName = convertView.findViewById(R.id.inv_name);
            TextView barcode = convertView.findViewById(R.id.scan_cam);
            TextView invBrand = convertView.findViewById(R.id.inv_brand);

            invCode.setText(inventory.getInvCode());
            invName.setText(inventory.getInvName());
            barcode.setText(inventory.getBarcode());
            invBrand.setText(inventory.getInvBrand());
            convertView.setTag(inventory);
            return convertView;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {

                    FilterResults results = new FilterResults();
                    List<Inventory> filteredArrayData = new ArrayList<>();
                    constraint = constraint.toString().toLowerCase();

                    for (Inventory inventory : activity.invList) {
                        if (inventory.getBarcode()
                                .concat(inventory.getInvCode())
                                .concat(inventory.getInvName())
                                .concat(inventory.getInvBrand())
                                .toLowerCase()
                                .contains(constraint))
                            filteredArrayData.add(inventory);
                    }

                    results.count = filteredArrayData.size();
                    results.values = filteredArrayData;
                    return results;
                }

                /** @noinspection unchecked*/
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    list = (List<Inventory>) results.values;
                    notifyDataSetChanged();
                }
            };
        }
    }

    private class TrxAdapter extends RecyclerView.Adapter<TrxAdapter.Holder> implements Filterable {
        private final ProductApproveTrxActivity activity;
        List<Trx> trxList;

        public TrxAdapter(Context context, List<Trx> trxList) {
            this.trxList = trxList;
            activity = (ProductApproveTrxActivity) context;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater()
                    .inflate(R.layout.approve_trx_item_layout, parent, false);
            return new Holder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Trx trx = trxList.get(position);
            holder.itemView.setOnLongClickListener(view -> {
                Trx selectedTrx = trxList.get(position);
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                        ProductApproveTrxActivity.this);
                dialogBuilder.setTitle(selectedTrx.getInvName())
                        .setMessage("Silmək istəyirsiniz?")
                        .setPositiveButton("Bəli", (dialog1, which) -> {
                            dbHelper.deleteApproveTrx(String.valueOf(selectedTrx.getTrxId()));
                            loadData();
                        })
                        .setNegativeButton("Xeyr", null);
                dialogBuilder.show();
                return true;
            });
            holder.itemView.setOnClickListener(view -> {
                Trx selectedTrx = trxList.get(position);
                showEditInvDialog(selectedTrx);
            });
            holder.invCode.setText(trx.getInvCode());
            holder.invName.setText(trx.getInvName());
            holder.qty.setText(String.valueOf(trx.getQty()));
            holder.invBrand.setText(String.valueOf(trx.getInvBrand()));
            holder.notes.setText(String.valueOf(trx.getNotes()));
        }

        @Override
        public int getItemCount() {
            return trxList.size();
        }

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
                                .concat(trx.getInvBrand())
                                .toLowerCase()
                                .contains(constraint))
                            filteredArrayData.add(trx);
                    }

                    results.count = filteredArrayData.size();
                    results.values = filteredArrayData;
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    trxList = (List<Trx>) results.values;
                    notifyDataSetChanged();
                }
            };
        }

        private class Holder extends RecyclerView.ViewHolder {
            TextView invCode;
            TextView invName;
            TextView qty;
            TextView invBrand;
            TextView notes;

            public Holder(@NonNull View itemView) {
                super(itemView);

                invCode = itemView.findViewById(R.id.inv_code);
                invName = itemView.findViewById(R.id.inv_name);
                qty = itemView.findViewById(R.id.qty);
                invBrand = itemView.findViewById(R.id.inv_brand);
                notes = itemView.findViewById(R.id.notes);
            }
        }
    }
}