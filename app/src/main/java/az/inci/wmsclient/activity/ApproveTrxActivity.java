package az.inci.wmsclient.activity;

import static android.R.drawable.ic_dialog_alert;
import static android.R.drawable.ic_dialog_info;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static az.inci.wmsclient.util.GlobalParameters.cameraScanning;
import static az.inci.wmsclient.util.UrlConstructor.addQueryParameters;
import static az.inci.wmsclient.util.UrlConstructor.createUrl;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SearchView;
import android.widget.Spinner;
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

import az.inci.wmsclient.AppConfig;
import az.inci.wmsclient.CustomException;
import az.inci.wmsclient.R;
import az.inci.wmsclient.model.Customer;
import az.inci.wmsclient.model.Doc;
import az.inci.wmsclient.model.Inventory;
import az.inci.wmsclient.model.Sbe;
import az.inci.wmsclient.model.Trx;
import az.inci.wmsclient.model.Whs;
import az.inci.wmsclient.model.v2.ResponseMessage;
import az.inci.wmsclient.model.v2.TransferRequest;
import az.inci.wmsclient.model.v2.TransferRequestItem;
import az.inci.wmsclient.util.DBHelper;
import az.inci.wmsclient.util.PrinterHelper;

public class ApproveTrxActivity extends ScannerSupportActivity {
    PrinterHelper printerHelper;
    private RadioButton fromWhsBtn;
    private Button selectSrcBtn;
    private RecyclerView trxListView;
    private TextView srcTxt;
    private Spinner trgWhsListSpinner;
    private SearchView searchView;
    private List<Trx> trxList;
    private List<Whs> trgWhsList;
    private List<Inventory> invList;
    private boolean docCreated;
    private boolean isReady;
    private int trxTypeId = 27;
    private double amount;
    private String trxNo;
    private String trgWhsCode;
    private String trgWhsName;
    private String srcWhsCode;
    private String srcWhsName;
    private String bpCode;
    private String bpName;
    private String sbeCode;
    private String sbeName;
    private Customer customer;
    private Sbe sbe;
    private Whs srcWhs;
    private Whs trgWhs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.approve_trx_layout);
        setEdgeToEdge();
        printerHelper = new PrinterHelper(this);

        selectSrcBtn = findViewById(R.id.select_src);
        trxListView = findViewById(R.id.trx_list_view);
        fromWhsBtn = findViewById(R.id.src_whs_btn);
        trgWhsListSpinner = findViewById(R.id.trg_whs_list);
        srcTxt = findViewById(R.id.src_text);

        ImageButton selectInvBtn = findViewById(R.id.inv_list);
        ImageButton uploadBtn = findViewById(R.id.send);
        ImageButton printBtn = findViewById(R.id.print);
        ImageButton scanCam = findViewById(R.id.scan_cam);
        RadioButton fromCustomerBtn = findViewById(R.id.customer_btn);

        int mode = getIntent().getIntExtra("mode", AppConfig.VIEW_MODE);
        if (mode == AppConfig.VIEW_MODE) {
            docCreated = true;
            trxNo = getIntent().getStringExtra("trxNo");
            trxTypeId = getIntent().getIntExtra("trxTypeId", 27);
            trgWhsCode = getIntent().getStringExtra("trgWhsCode");
            trgWhsName = getIntent().getStringExtra("trgWhsName");
            srcWhsCode = getIntent().getStringExtra("srcWhsCode");
            srcWhsName = getIntent().getStringExtra("srcWhsName");
            bpCode = getIntent().getStringExtra("bpCode");
            bpName = getIntent().getStringExtra("bpName");
            sbeCode = getIntent().getStringExtra("sbeCode");
            sbeName = getIntent().getStringExtra("sbeName");
            amount = getIntent().getDoubleExtra("amount", 0);

            switch (trxTypeId) {
                case 27:
                    fromCustomerBtn.setChecked(true);
                    break;
                case 53:
                    fromWhsBtn.setChecked(true);
                    break;
            }

            isReady = !srcTxt.getText().toString().isEmpty();
        } else {
            trxNo = new SimpleDateFormat("yyyyMMddhhmmss", Locale.getDefault()).format(new Date());
        }

        fromCustomerBtn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            trxTypeId = isChecked ? 27 : 53;
            isReady = false;
            srcTxt.setText("");
        });

        fromWhsBtn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            trxTypeId = isChecked ? 53 : 27;
            isReady = false;
            srcTxt.setText("");
        });

        printBtn.setOnClickListener(v -> {
            if (isReady) {
                String report;
                switch (trxTypeId) {
                    case 27:
                        report = getReturnForm();
                        break;
                    case 53:
                        report = getTransferForm();
                        break;
                    default:
                        report = "";
                }
                printerHelper.print(report);
            } else {
                showMessageDialog(getString(R.string.info), "Mənbə seçilməyib",
                        ic_dialog_info);
            }
        });

        selectSrcBtn.setOnClickListener(v -> {
            switch (trxTypeId) {
                case 27:
                    getSbeList();
                    break;
                case 53:
                    getWhsList();
                    break;
            }
        });

        trgWhsListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                trgWhs = (Whs) parent.getItemAtPosition(position);
                trgWhsCode = trgWhs.getWhsCode();
                trgWhsName = trgWhs.getWhsName();
                if (docCreated) {
                    updateDocTrg();
                } else {
                    createNewDoc();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        selectInvBtn.setOnClickListener(v -> {
            if (invList == null) {
                getInvList();
            } else {
                showInvList();
            }
        });

        uploadBtn.setOnClickListener(v -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ApproveTrxActivity.this);
            dialogBuilder.setMessage("Göndərmək istəyirsiniz?")
                    .setPositiveButton("Bəli", (dialog1, which) -> {
                        switch (trxTypeId) {
                            case 27:
                                break;
                            case 53:
                                uploadTransfer();
                                break;
                        }
                    })
                    .setNegativeButton("Xeyr", null);
            dialogBuilder.show();
        });

        scanCam.setVisibility(cameraScanning ? VISIBLE : GONE);
        scanCam.setOnClickListener(v -> openCameraScanner());

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!searchView.isIconified())
                    searchView.setIconified(true);
                else if (trxList.isEmpty()) {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ApproveTrxActivity.this);
                    dialogBuilder.setMessage("Mal daxil edilməyib. Sənəd silinsin?")
                            .setPositiveButton("Bəli", (dialog1, which) -> {
                                dbHelper.deleteApproveDoc(trxNo);
                                finish();
                            })
                            .setNegativeButton("Xeyr", (dialog12, which) -> finish());
                    dialogBuilder.show();
                } else
                    finish();
            }
        });

        loadTrgWhsList();

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
                    TrxAdapter adapter = (TrxAdapter) trxListView.getAdapter();
                    if (adapter != null)
                        adapter.getFilter().filter(newText);
                    return true;
                }
            });
        }
        return true;
    }

    private void createNewDoc() {
        Doc doc = new Doc();
        doc.setTrxNo(trxNo);
        doc.setTrxTypeId(trxTypeId);
        doc.setTrxDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        doc.setWhsCode(trgWhs.getWhsCode());
        switch (trxTypeId) {
            case 27:
                doc.setBpCode(bpCode);
                doc.setBpName(bpName);
                doc.setSbeCode(sbeCode);
                doc.setSbeName(sbeName);
                break;
            case 53:
                doc.setSrcWhsCode(srcWhsCode);
                doc.setSrcWhsName(srcWhsName);
        }
        doc.setAmount(amount);
        dbHelper.addApproveDoc(doc);
        docCreated = true;
        setTitle(trxNo);
    }

    private void updateDocAmount() {
        ContentValues values = new ContentValues();
        values.put(DBHelper.AMOUNT, amount);
        dbHelper.updateApproveDoc(trxNo, values);
        isReady = !srcTxt.getText().toString().isEmpty();
        updateButtonsStatus();
    }

    private void updateDocTrg() {
        ContentValues values = new ContentValues();
        values.put(DBHelper.TRG_WHS_CODE, trgWhs.getWhsCode());
        values.put(DBHelper.TRG_WHS_NAME, trgWhs.getWhsName());
        dbHelper.updateApproveDoc(trxNo, values);
        isReady = !srcTxt.getText().toString().isEmpty();
        updateButtonsStatus();
    }

    private void updateDocSrc() {
        ContentValues values = new ContentValues();
        switch (trxTypeId) {
            case 27:
                bpCode = customer.getBpCode();
                bpName = customer.getBpName();
                sbeCode = customer.getSbeCode();
                sbeName = customer.getSbeName();
                values.put(DBHelper.TRX_TYPE_ID, trxTypeId);
                values.put(DBHelper.BP_CODE, customer.getBpCode());
                values.put(DBHelper.BP_NAME, customer.getBpName());
                values.put(DBHelper.SBE_CODE, customer.getSbeCode());
                values.put(DBHelper.SBE_NAME, customer.getSbeName());
                values.put(DBHelper.SRC_WHS_CODE, "");
                values.put(DBHelper.SRC_WHS_NAME, "");
                break;
            case 53:
                srcWhsCode = srcWhs.getWhsCode();
                srcWhsName = srcWhs.getWhsName();
                values.put(DBHelper.TRX_TYPE_ID, trxTypeId);
                values.put(DBHelper.BP_CODE, "");
                values.put(DBHelper.BP_NAME, "");
                values.put(DBHelper.SBE_CODE, "");
                values.put(DBHelper.SBE_NAME, "");
                values.put(DBHelper.SRC_WHS_CODE, srcWhs.getWhsCode());
                values.put(DBHelper.SRC_WHS_NAME, srcWhs.getWhsName());
                break;
        }
        dbHelper.updateApproveDoc(trxNo, values);
        isReady = !srcTxt.getText().toString().isEmpty();
        updateButtonsStatus();
    }

    public void loadData() {
        customer = new Customer();
        customer.setBpCode(bpCode);
        customer.setBpName(bpName);
        customer.setSbeCode(sbeCode);
        customer.setSbeName(sbeName);
        sbe = new Sbe();
        sbe.setSbeCode(sbeCode);
        sbe.setSbeName(sbeName);
        trgWhs = new Whs();
        trgWhs.setWhsCode(trgWhsCode);
        trgWhs.setWhsName(trgWhsName);
        srcWhs = new Whs();
        srcWhs.setWhsCode(srcWhsCode);
        srcWhs.setWhsName(srcWhsName);
        srcTxt.setText(trxTypeId == 27 ? customer.toString() : srcWhs.toString());
        setTitle(trxNo);
        trxList = dbHelper.getApproveTrxList(trxNo);
        TrxAdapter adapter = new TrxAdapter(this, trxList);
        trxListView.setLayoutManager(new LinearLayoutManager(this));
        trxListView.setAdapter(adapter);

        if (trxList.isEmpty()) {
            findViewById(R.id.trx_list_scroll).setVisibility(View.GONE);
        } else {
            findViewById(R.id.trx_list_scroll).setVisibility(View.VISIBLE);
        }

        updateButtonsStatus();
    }

    @Override
    public void onScanComplete(String barcode) {
        getInvFromServer(barcode);
    }

    public void onScanComplete(Intent intent) {
    }

    private void updateButtonsStatus() {
        if (trxTypeId == 27 && !trxList.isEmpty()) {
            fromWhsBtn.setEnabled(!isReady);
            selectSrcBtn.setEnabled(!isReady);
        } else {
            fromWhsBtn.setEnabled(true);
            selectSrcBtn.setEnabled(true);
        }
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
                if (currentInv != null) showAddInvDialog(currentInv);
            } catch (CustomException e) {
                logger.logError(e.toString());
                runOnUiThread(() -> showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert));
            } finally {
                runOnUiThread(() -> showProgressDialog(false));
            }
        }).start();
    }

    private void getSbeList() {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("src", "sbe");
            try {
                List<Sbe> sbeList = httpClient.getListData(url, "GET", null, Sbe[].class);
                if (sbeList != null) runOnUiThread(() -> showSbeList(sbeList));
            } catch (CustomException e) {
                logger.logError(e.toString());
                runOnUiThread(() -> showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert));
            } finally {
                runOnUiThread(() -> showProgressDialog(false));
            }

        }).start();
    }

    private void getCustomerList(String sbeCode) {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("src", "customer");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("sbe-code", sbeCode);
            url = addQueryParameters(url, parameters);
            try {
                List<Customer> customerList = httpClient.getListData(url, "GET", null, Customer[].class);
                if (customerList != null) runOnUiThread(() -> showCustomerList(customerList));
            } catch (CustomException e) {
                logger.logError(e.toString());
                runOnUiThread(() -> showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert));
            } finally {
                runOnUiThread(() -> showProgressDialog(false));
            }
        }).start();
    }

    private void getWhsList() {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("src", "whs");
            try {
                List<Whs> whsList = httpClient.getListData(url, "GET", null, Whs[].class);
                if (whsList != null) runOnUiThread(() -> showSrcWhsList(whsList));
            } catch (CustomException e) {
                logger.logError(e.toString());
                runOnUiThread(() -> showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert));
            } finally {
                runOnUiThread(() -> showProgressDialog(false));
            }
        }).start();
    }

    private void loadTrgWhsList() {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("src", "whs", "target");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("user-id", appUser.getId());
            url = addQueryParameters(url, parameters);
            try {
                trgWhsList = httpClient.getListData(url, "GET", null, Whs[].class);
                if (trgWhsList != null) 
                    runOnUiThread(this::publishTrgWhsList);
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
            String url = createUrl("inv");
            try {
                invList = httpClient.getListData(url, "GET", null, Inventory[].class);
                if (invList != null) runOnUiThread(this::showInvList);
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
        InventoryAdapter adapter = new InventoryAdapter(this, invList);
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
        listView.setOnItemClickListener((parent, view1, position, id) -> showAddInvDialog((Inventory) view1.getTag()));
        listView.setOnItemLongClickListener((parent, view1, position, id) -> {
            showInfoDialog((Inventory) view1.getTag());
            return true;
        });
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Mallar").setView(view).show();
    }

    private void publishTrgWhsList() {
        ArrayAdapter<Whs> adapter = new ArrayAdapter<>(this,
                R.layout.support_simple_spinner_dropdown_item,
                trgWhsList);
        trgWhsListSpinner.setAdapter(adapter);
        trgWhsListSpinner.setSelection(trgWhsList.indexOf(trgWhs));
    }

    private void showSbeList(List<Sbe> sbeList) {
        View view = getLayoutInflater().inflate(R.layout.result_list_dialog, findViewById(android.R.id.content), false);
        ListView listView = view.findViewById(R.id.result_list);
        ArrayAdapter<Sbe> adapter = new ArrayAdapter<>(this, R.layout.simple_list_item, sbeList);
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
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        AlertDialog dialog = dialogBuilder.setTitle("Təmsilçilər").setView(view).create();
        dialog.show();

        listView.setOnItemClickListener((adapterView, view1, i, l) -> {
            dialog.dismiss();
            sbe = (Sbe) adapterView.getItemAtPosition(i);
            getCustomerList(sbe.getSbeCode());
        });
    }

    private void showCustomerList(List<Customer> customerList) {
        View view = getLayoutInflater().inflate(R.layout.result_list_dialog, findViewById(android.R.id.content), false);
        ListView listView = view.findViewById(R.id.result_list);
        ArrayAdapter<Customer> adapter = new ArrayAdapter<>(this, R.layout.simple_list_item, customerList);
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
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        AlertDialog dialog = dialogBuilder.setTitle("Müştərilər").setView(view).create();
        dialog.show();

        listView.setOnItemClickListener((adapterView, view1, i, l) -> {
            dialog.dismiss();
            customer = (Customer) adapterView.getItemAtPosition(i);
            srcTxt.setText(customer.toString());
            updateDocSrc();
            if (!trxList.isEmpty()) {
                fromWhsBtn.setEnabled(false);
                selectSrcBtn.setEnabled(false);
                for (Trx trx : trxList) {
                    dbHelper.deleteApproveTrx(String.valueOf(trx.getTrxId()));
                    amount -= trx.getAmount();
                    splitTrx(trx);
                }
            }
        });
    }

    private void showSrcWhsList(List<Whs> whsList) {
        View view = getLayoutInflater().inflate(R.layout.result_list_dialog, findViewById(android.R.id.content), false);
        ListView listView = view.findViewById(R.id.result_list);
        ArrayAdapter<Whs> adapter = new ArrayAdapter<>(this, R.layout.simple_list_item, whsList);
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

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        AlertDialog dialog = dialogBuilder.setTitle("Anbarlar").setView(view).create();
        dialog.show();

        listView.setOnItemClickListener((adapterView, view1, i, l) -> {
            dialog.dismiss();
            srcWhs = (Whs) adapterView.getItemAtPosition(i);
            srcTxt.setText(srcWhs.toString());
            if (docCreated) {
                updateDocSrc();
            } else {
                createNewDoc();
            }
        });
    }

    private void showAddInvDialog(Inventory inventory) {
        if (inventory.getInvCode() == null) {
            showMessageDialog(getString(R.string.info), getString(R.string.good_not_found),
                    ic_dialog_info);
        } else {
            EditText qtyEdit = new EditText(this);
            qtyEdit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            qtyEdit.selectAll();
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
                            if (containingTrx.getTrxId() > 0) {
                                updateApproveTrxQty(containingTrx,
                                        qty + containingTrx.getQty());
                            } else {
                                Trx trx = Trx.parseFromInv(inventory);
                                trx.setQty(qty);
                                trx.setTrxNo(trxNo);
                                trx.setAmount(trx.getPrice() * qty);
                                if (trxTypeId == 27 && !isReturned(trx.getInvCode())) {
                                    splitTrx(trx);
                                } else {
                                    addApproveTrx(trx);
                                }
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
        if (trx == null) {
            showMessageDialog(getString(R.string.info), getString(R.string.good_not_found),
                    ic_dialog_info);
        } else {
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
        amount += trx.getAmount();
        if (docCreated) {
            updateDocAmount();
        } else {
            createNewDoc();
        }
        loadData();
    }

    private void updateApproveTrxQty(Trx trx, double qty) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.QTY, qty);
        dbHelper.updateApproveTrx(String.valueOf(trx.getTrxId()), values);
        amount += (qty - trx.getQty()) * trx.getPrice();
        updateDocAmount();
    }

    private Trx containingTrx(String invCode) {
        for (Trx trx : trxList) {
            if (trx.getInvCode().equals(invCode) && !trx.isReturned()) {
                return trx;
            }
        }

        return new Trx();
    }

    private boolean isReturned(String invCode) {
        for (Trx trx : trxList) {
            if (trx.getInvCode().equals(invCode) && trx.isReturned()) {
                return true;
            }
        }

        return false;
    }

    private void splitTrx(Trx trx) {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("trx", "split-trx");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("bp-code", bpCode);
            parameters.put("inv-code", trx.getInvCode());
            parameters.put("qty", String.valueOf(trx.getQty()));
            url = addQueryParameters(url, parameters);
            try {
                List<Trx> splitTrxList = httpClient.getListData(url, "GET", null, Trx[].class);

                if (splitTrxList != null) {
                    runOnUiThread(() -> {
                        if (!splitTrxList.isEmpty()) {
                            addSplitTrx(splitTrxList, trx);
                        } else {
                            addApproveTrx(trx);
                        }
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

    private void addSplitTrx(List<Trx> splitTrxList, Trx baseTrx) {
        double qtySum = baseTrx.getQty();

        for (int i = 0; i < splitTrxList.size(); i++) {
            Trx trx = splitTrxList.get(i);
            trx.setTrxNo(baseTrx.getTrxNo());
            trx.setTrxDate(baseTrx.getTrxDate());
            trx.setInvCode(baseTrx.getInvCode());
            trx.setInvBrand(baseTrx.getInvBrand());
            trx.setInvName(baseTrx.getInvName());
            trx.setDiscount(trx.getQty() * trx.getPrice() * trx.getDiscountRatio() / 100);
            trx.setAmount(trx.getPrice() * trx.getQty());
            if (qtySum < trx.getQty()) {
                trx.setQty(qtySum);
                trx.setDiscount(trx.getQty() * trx.getPrice() * trx.getDiscountRatio() / 100);
                trx.setAmount(trx.getPrice() * trx.getQty());
            }
            amount += trx.getAmount();
            dbHelper.addApproveTrx(trx);
            qtySum -= trx.getQty();
        }

        if (qtySum > 0) {
            baseTrx.setQty(qtySum);
            baseTrx.setAmount(qtySum * baseTrx.getPrice());
            dbHelper.addApproveTrx(baseTrx);
        }
        loadData();
    }

    private String getTransferForm() {
        String html = "<html><head><style>*{margin:0px; padding:0px}" +
                "table,tr,th,td{border: 1px solid black;border-collapse: collapse; font-size: 12px}" +
                "th{background-color: #636d72;color:white}td,th{padding:0 4px 0 4px}</style>" +
                "</head><body>";
        html = html.concat("<h3 style='text-align: center'>Anbardan anbara transfer</h3></br>");
        html = html.concat(
                "<div style='font-size: 14px'><b>Çıxış anbarı: " + srcWhs.toString() + "</br>");
        html = html.concat("Giriş anbarı: " + trgWhs.toString() + "</b></div></br>");
        html = html.concat("<table>");
        html = html.concat("<tr><th>Mal kodu</th>");
        html = html.concat("<th>Mal adı</th>");
        html = html.concat("<th>Barkod</th>");
        html = html.concat("<th>Brend</th>");
        html = html.concat("<th>Miqdar</th>");
        html = html.concat("<th>Qiymət</th>");
        html = html.concat("<th>Məbləğ</th>");
        trxList.sort(Comparator.comparing(Trx::getPrevTrxNo));
        for (Trx trx : trxList) {

            html = html.concat(
                    "<tr><td>" + trx.getInvCode() + "</td><td>" + trx.getInvName() + "</td>");
            html = html.concat("<td>" + trx.getBarcode() + "</td>");
            html = html.concat("<td>" + trx.getInvBrand() + "</td>");
            html = html.concat("<td style='text-align: right'>" + format(trx.getQty()) + "</td>");
            html = html.concat("<td style='text-align: right'>" + format(trx.getPrice()) + "</td>");
            html = html.concat(
                    "<td style='text-align: right'>" + format(trx.getQty() * trx.getPrice()) +
                            "</td>");
        }

        html = html.concat("<tr><td colspan='6' style='text-align: right'><b>Cəmi</b></td>");
        html = html.concat("<td style='text-align: right'><b>" + format(amount) + "</b></td></tr>");
        html = html.concat("</table></body></head>");

        return html;
    }

    private String getReturnForm() {
        String html = "<html><head><meta charset=\"UTF-8\"><style>*{margin:0px; padding:0px}" +
                "table,tr,th,td{border: 1px solid black;border-collapse: collapse; font-size: 12px}" +
                "th{background-color: #636d72;color:white}td,th{padding:0 4px 0 4px}</style>" +
                "</head><body>";
        html = html.concat("<h3 style='text-align: center'>Satışdan geri qaytarma</h3></br>");
        html = html.concat(
                "<div style='font-size: 14px'><b>Müştəri: " + customer.toString() + "</br>");
        html = html.concat("Təmsilçi: " + sbe.toString() + "</br>");
        html = html.concat("Anbar: " + trgWhs.toString() + "</b></div></br>");
        html = html.concat("<table>");
        html = html.concat("<tr><th>Mal kodu</th>");
        html = html.concat("<th>Mal adı</th>");
        html = html.concat("<th>Barkod</th>");
        html = html.concat("<th>Brend</th>");
        html = html.concat("<th>Miqdar</th>");
        html = html.concat("<th>Qiymət</th>");
        html = html.concat("<th>Endirim (%)</th>");
        html = html.concat("<th>Məbləğ</th>");
        html = html.concat("<th>Satış qaiməsi</th></tr>");
        double invAmount;
        double discountAmount = 0;
        double amountWithoutDiscount = 0;
        if (trxTypeId == 27) {
            trxList.sort(Comparator.comparing(Trx::getPrevTrxNo));
        } else {
            trxList.sort(Comparator.comparingInt(Trx::getTrxId));
        }
        for (Trx trx : trxList) {
            invAmount = trx.getAmount() - trx.getDiscount();
            discountAmount += trx.getDiscount();
            amountWithoutDiscount += invAmount;

            html = html.concat(
                    "<tr><td>" + trx.getInvCode() + "</td><td>" + trx.getInvName() + "</td>");
            html = html.concat("<td>" + trx.getBarcode() + "</td>");
            html = html.concat("<td>" + trx.getInvBrand() + "</td>");
            html = html.concat("<td style='text-align: right'>" + format(trx.getQty()) + "</td>");
            html = html.concat("<td style='text-align: right'>" + format(trx.getPrice()) + "</td>");
            html = html.concat(
                    "<td style='text-align: right'>" + format(trx.getDiscountRatio()) + "</td>");
            html = html.concat("<td style='text-align: right'>" + format(invAmount) + "</td>");
            html = html.concat("<td>" + trx.getPrevTrxNo() + "</td></tr>");
        }

        html = html.concat("<tr><td colspan='7' style='text-align: right'><b>Cəmi</b></td>");
        html = html.concat("<td style='text-align: right'><b>" + format(amount) + "</b></td></tr>");
        html = html.concat("<tr><td colspan='7' style='text-align: right'><b>Endirim</b></td>");
        html = html.concat(
                "<td style='text-align: right'><b>" + format(discountAmount) + "</b></td></tr>");
        html = html.concat(
                "<tr><td colspan='7' style='text-align: right'><b>Cəmi (Endirimli)</b></td>");
        html = html.concat("<td style='text-align: right'><b>" + format(amountWithoutDiscount) +
                "</b></td></tr>");
        html = html.concat("</table></body></html>");

        return html;
    }

    private String format(double value) {
        return String.format(Locale.getDefault(), "%.03f", value);
    }

    private void uploadTransfer() {
        if (isReady) {
            showProgressDialog(true);
            new Thread(() -> {
                String url = createUrl("inv-move", "create-transfer");
                TransferRequest request = new TransferRequest();
                request.setSrcWhsCode(srcWhs.getWhsCode());
                request.setTrgWhsCode(trgWhs.getWhsCode());
                request.setUserId(appUser.getId());
                List<TransferRequestItem> items = new ArrayList<>();
                for (Trx trx : trxList) {
                    TransferRequestItem requestItem = new TransferRequestItem();
                    requestItem.setInvCode(trx.getInvCode());
                    requestItem.setQty(trx.getQty());
                    items.add(requestItem);
                }
                request.setRequestItems(items);

                try {
                    ResponseMessage message = httpClient.executeUpdate(url, request);
                    runOnUiThread(() -> {
                        if (message.getStatusCode() == 0) {
                            dbHelper.deleteApproveDoc(trxNo);
                            finish();
                        } else {
                            showMessageDialog(getString(R.string.error), message.getBody(),
                                    message.getIconId());
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
            showMessageDialog(getString(R.string.info), "Mənbə təyin edilməyib",
                    ic_dialog_info);
        }
    }

    private void showInfoDialog(Inventory inventory) {
        String info = inventory.getInvName();
        info += "\n\nBrend: " + inventory.getInvBrand();
        AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(this);
        dialogbuilder.setTitle("Məlumat");
        dialogbuilder.setMessage(info);
        dialogbuilder.setPositiveButton("Şəkil", (dialog, which) -> {
            Intent photoIntent = new Intent(this, PhotoActivity.class);
            photoIntent.putExtra("invCode", inventory.getInvCode());
            startActivity(photoIntent);
        });
        dialogbuilder.show();
    }

    private class InventoryAdapter extends ArrayAdapter<Inventory> implements Filterable {
        List<Inventory> list;
        ApproveTrxActivity activity;

        public InventoryAdapter(@NonNull Context context, @NonNull List<Inventory> objects) {
            super(context, 0, 0, objects);
            list = objects;
            activity = (ApproveTrxActivity) context;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Inventory inventory = list.get(position);
            if (convertView == null) {
                convertView = getLayoutInflater()
                        .inflate(R.layout.inv_list_item, parent, false);
            }
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
                                .contains(constraint)) {
                            filteredArrayData.add(inventory);
                        }
                    }

                    results.count = filteredArrayData.size();
                    results.values = filteredArrayData;
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    list = (List<Inventory>) results.values;
                    notifyDataSetChanged();
                }
            };
        }
    }

    private class TrxAdapter extends RecyclerView.Adapter<TrxAdapter.Holder> implements Filterable {
        private final ApproveTrxActivity activity;
        List<Trx> trxList;
        View itemView;

        public TrxAdapter(Context context, List<Trx> trxList) {
            this.trxList = trxList;
            activity = (ApproveTrxActivity) context;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            itemView = getLayoutInflater()
                    .inflate(R.layout.approve_trx_item_layout, parent, false);
            return new Holder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Trx trx = trxList.get(position);
            itemView.setOnLongClickListener(view -> {
                Trx selectedTrx = trxList.get(position);
                if (!selectedTrx.isReturned()) {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                            ApproveTrxActivity.this);
                    dialogBuilder.setTitle(selectedTrx.getInvName())
                            .setMessage("Silmək istəyirsiniz?")
                            .setPositiveButton("Bəli", (dialog1, which) -> {
                                dbHelper.deleteApproveTrx(
                                        String.valueOf(selectedTrx.getTrxId()));
                                amount -= selectedTrx.getQty() * selectedTrx.getPrice();
                                updateDocAmount();
                                loadData();
                            })
                            .setNegativeButton("Xeyr", null);
                    dialogBuilder.show();
                }
                return true;
            });
            itemView.setOnClickListener(view -> {
                Trx selectedTrx = trxList.get(position);
                if (!selectedTrx.isReturned()) showEditInvDialog(selectedTrx);
            });
            holder.invCode.setText(trx.getInvCode());
            holder.invName.setText(trx.getInvName());
            holder.qty.setText(String.valueOf(trx.getQty()));
            holder.invBrand.setText(String.valueOf(trx.getInvBrand()));
            itemView.setTag(trx);
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

            public Holder(@NonNull View itemView) {
                super(itemView);

                invCode = itemView.findViewById(R.id.inv_code);
                invName = itemView.findViewById(R.id.inv_name);
                qty = itemView.findViewById(R.id.qty);
                invBrand = itemView.findViewById(R.id.inv_brand);
            }
        }
    }
}