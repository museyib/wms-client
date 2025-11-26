package az.inci.wmsclient.activity;

import static android.R.drawable.ic_dialog_alert;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static az.inci.wmsclient.fragment.LatestMovementsHelper.showInventoryHistory;
import static az.inci.wmsclient.util.GlobalParameters.cameraScanning;
import static az.inci.wmsclient.util.UrlConstructor.addQueryParameters;
import static az.inci.wmsclient.util.UrlConstructor.createUrl;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import az.inci.wmsclient.CustomException;
import az.inci.wmsclient.R;
import az.inci.wmsclient.model.Inventory;
import az.inci.wmsclient.model.v2.ResponseMessage;
import az.inci.wmsclient.model.v4.Brand;
import az.inci.wmsclient.model.v4.UpdateInvRequest;

public class InventoryInfoActivity extends ScannerSupportActivity {
    private TextView infoText;
    private EditText keywordEdit;
    private Spinner searchField;
    private Button latestMovements;
    private String invCode;
    private String invName;
    private String defaultUomCode;
    private String whsCode;
    private Inventory inventory;
    private List<Brand> brandList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_info);
        setEdgeToEdge();
        infoText = findViewById(R.id.good_info);
        keywordEdit = findViewById(R.id.keyword_edit);
        searchField = findViewById(R.id.search_field);

        Button searchBtn = findViewById(R.id.search);
        Button scanCam = findViewById(R.id.scan_cam);
        Button editAttributes = findViewById(R.id.edit_attributes);
        Button editShelf = findViewById(R.id.edit_shelf_location);
        Button editBarcodes = findViewById(R.id.edit_barcodes);
        Button viewImage = findViewById(R.id.photo);
        Button editInv = findViewById(R.id.edit_inv_data);
        latestMovements = findViewById(R.id.latest_movements);

        scanCam.setVisibility(cameraScanning ? VISIBLE : GONE);
        searchField.setAdapter(ArrayAdapter.createFromResource(this, R.array.search_field_list,
                R.layout.spinner_item));

        searchBtn.setOnClickListener(v -> searchKeyword());
        scanCam.setOnClickListener(v -> openCameraScanner());
        editAttributes.setOnClickListener(v -> editAttributes());
        editBarcodes.setOnClickListener(v -> editBarcodes());
        viewImage.setOnClickListener(v -> viewImage());
        editShelf.setOnClickListener(v -> editShelfLocation());

        if (appUser.isChangeInvMasterFlag())
            editInv.setVisibility(VISIBLE);
        else
            editInv.setVisibility(GONE);
        editInv.setOnClickListener(v -> editInvData());
    }

    private void editInvData() {
        if (invCode != null) {
            View view = getLayoutInflater()
                    .inflate(R.layout.edit_inv_data_dialog,
                            findViewById(android.R.id.content), false);
            Spinner brandListSpinner = view.findViewById(R.id.brand_list_spinner);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            AlertDialog dialog = dialogBuilder
                    .setTitle(invCode)
                    .setView(view)
                    .setPositiveButton(getString(R.string.save), (dialogInterface, i) -> {
                        Brand brand = (Brand) brandListSpinner.getSelectedItem();
                        if (brand == null) {
                            showMessageDialog(getString(R.string.info), getString(R.string.brand_not_selected),
                                    ic_dialog_alert);
                        } else {
                            String brandCode = ((Brand) brandListSpinner.getSelectedItem()).getBrandCode();
                            showProgressDialog(true);
                            new Thread(() -> {
                                UpdateInvRequest request = new UpdateInvRequest();
                                request.setInvCode(invCode);
                                request.setBrandCode(brandCode);
                                request.setUserId(appUser.getId());
                                request.setDeviceId(getDeviceIdString());

                                try {
                                    ResponseMessage responseMessage = httpClient.executeUpdate(createUrl("inv", "update-inv-master-data"), request);
                                    runOnUiThread(() -> {
                                        loadData();
                                        showMessageDialog(getString(R.string.info), responseMessage.getBody(), ic_dialog_alert);
                                    });
                                } catch (CustomException e) {
                                    logger.logError(e.toString());
                                    runOnUiThread(() -> showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert));
                                } finally {
                                    runOnUiThread(() -> showProgressDialog(false));
                                }
                            }).start();
                        }
                    })
                    .create();
            dialog.show();

            showProgressDialog(true);
            new Thread(() -> {
                try {
                    brandList = httpClient.getListData(createUrl("inv", "brands"), "GET", null, Brand[].class);
                    runOnUiThread(() -> {
                        if (brandList != null) {
                            ArrayAdapter<Brand> adapter = new BrandAdapter(this, R.layout.spinner_item, brandList);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            brandListSpinner.setAdapter(adapter);
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
                            Brand brand = new Brand();
                            brand.setBrandCode(inventory.getInvBrand());
                            brandListSpinner.setSelection(brandList.indexOf(brand));
                        }
                    });
                } catch (CustomException e) {
                    logger.logError(e.toString());
                    runOnUiThread(() -> showMessageDialog(getString(R.string.error), e.toString(),
                            ic_dialog_alert));
                } finally {
                    runOnUiThread(() -> showProgressDialog(false));
                }
            }).start();
        }
    }

    @Override
    protected void loadData() {
        if (invCode != null) {
            getDataByInvCode(invCode);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    public void onScanComplete(String barcode) {
        getDataByBarcode(barcode);
    }

    public void viewImage() {
        if (invCode != null) {
            Intent intent = new Intent(this, PhotoActivity.class);
            intent.putExtra("invCode", invCode);
            startActivity(intent);
        }
    }

    public void editShelfLocation() {
        Intent intent = new Intent(this, EditShelfActivity.class);
        startActivity(intent);
    }

    public void searchKeyword() {
        String keyword = keywordEdit.getText().toString();

        if (keyword.isEmpty()) {
            showMessageDialog(getString(R.string.info), getString(R.string.keyword_not_entered),
                    android.R.drawable.ic_dialog_info);
            playSound(SOUND_FAIL);
        } else {
            searchForKeyword(keyword);
        }
    }

    private void showResultListDialog(List<Inventory> list) {
        if (list.isEmpty()) {
            showMessageDialog(getString(R.string.info), getString(R.string.good_not_found),
                    ic_dialog_alert);
            playSound(SOUND_FAIL);
            return;
        }

        View view = getLayoutInflater()
                .inflate(R.layout.result_list_dialog,
                        findViewById(android.R.id.content), false);

        ListView listView = view.findViewById(R.id.result_list);
        ArrayAdapter<Inventory> adapter = new ArrayAdapter<>(this, R.layout.simple_list_item, list);
        listView.setAdapter(adapter);
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

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        AlertDialog dialog = dialogBuilder.setTitle("Axtarışın nəticəsi").setView(view).create();
        dialog.show();

        listView.setOnItemClickListener((adapterView, view1, i, l) -> {
            inventory = (Inventory) adapterView.getItemAtPosition(i);
            invCode = inventory.getInvCode();
            defaultUomCode = inventory.getDefaultUomCode();
            getDataByInvCode(invCode);

            dialog.dismiss();
        });
    }

    private void getInfo(String url) {
        try {
            inventory = httpClient.getSimpleObject(url, "GET", null, Inventory.class);
            if (inventory != null) {
                runOnUiThread(() -> printInfo(inventory));
            }
        } catch (CustomException e) {
            logger.logError(e.toString());
            runOnUiThread(() -> showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert));
        } finally {
            runOnUiThread(() -> showProgressDialog(false));
        }
    }

    private void printInfo(Inventory inventory) {
        if (inventory.getInvCode() == null) {
            showMessageDialog(getString(R.string.error), getString(R.string.good_not_found),
                    ic_dialog_alert);
            playSound(SOUND_FAIL);
            return;
        }
        invCode = inventory.getInvCode();
        invName = inventory.getInvName();
        String info = "Mal kodu: " + invCode
                + "\nMal adı: " + invName
                + "\nAnbar qalığı: " + inventory.getWhsQty()
                + "\nBrend: " + inventory.getInvBrand() + "\n"
                + inventory.getInfo();
        defaultUomCode = inventory.getDefaultUomCode();
        whsCode = inventory.getWhsCode();
        latestMovements.setOnClickListener(v -> showInventoryHistory(this, invCode, whsCode));
        info = info.replaceAll("; ", "\n");
        info = info.replaceAll("\\\\n", "\n");
        infoText.setText(info);
        playSound(SOUND_SUCCESS);
    }

    public void editAttributes() {
        if (!appUser.isAttributeFlag()) {
            showMessageDialog(getString(R.string.warning), getString(R.string.not_allowed), ic_dialog_alert);
            playSound(SOUND_FAIL);
            return;
        }
        if (invCode != null) {
            Intent intent = new Intent(this, EditAttributesActivity.class);
            intent.putExtra("invCode", invCode);
            intent.putExtra("invName", invName);
            intent.putExtra("defaultUomCode", defaultUomCode);
            startActivity(intent);
        }
    }

    private void getDataByInvCode(String invCode) {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("inv", "info-by-inv-code");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("inv-code", invCode);
            parameters.put("user-id", appUser.getId());
            url = addQueryParameters(url, parameters);
            getInfo(url);
        }).start();
    }

    private void getDataByBarcode(String barcode) {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("inv", "info-by-barcode");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("barcode", barcode);
            parameters.put("user-id", appUser.getId());
            url = addQueryParameters(url, parameters);
            getInfo(url);
        }).start();
    }

    private void searchForKeyword(String keyword) {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("inv", "search");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("keyword", keyword);
            parameters.put("in", (String) searchField.getSelectedItem());
            url = addQueryParameters(url, parameters);
            try {
                List<Inventory> inventoryList = httpClient.getListData(url, "GET", null, Inventory[].class);
                runOnUiThread(() -> {
                    if (inventoryList != null) {
                        showResultListDialog(inventoryList);
                    }
                });
            } catch (CustomException e) {
                logger.logError(e.toString());
                runOnUiThread(() -> showMessageDialog(getString(R.string.error), e.toString(),
                        ic_dialog_alert));
            } finally {
                runOnUiThread(() -> showProgressDialog(false));
            }
        }).start();
    }

    public void editBarcodes() {
        if (invCode != null) {
            Intent intent = new Intent(this, EditBarcodesActivity.class);
            intent.putExtra("invCode", invCode);
            intent.putExtra("invName", invName);
            intent.putExtra("defaultUomCode", defaultUomCode);
            startActivity(intent);
        }
    }

    private static class BrandAdapter extends ArrayAdapter<Brand> implements Filterable {
        List<Brand> list;
        InventoryInfoActivity activity;

        public BrandAdapter(@NonNull Context context, int resourceId, @NonNull List<Brand> objects) {
            super(context, resourceId, objects);
            list = objects;
            activity = (InventoryInfoActivity) context;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Brand getItem(int position) {
            return list.get(position);
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {

                    FilterResults results = new FilterResults();
                    List<Brand> filteredArrayData = new ArrayList<>();
                    constraint = constraint.toString().toLowerCase();

                    for (Brand brand : activity.brandList) {
                        if (brand.getBrandCode()
                                .toLowerCase()
                                .contains(constraint))
                            filteredArrayData.add(brand);
                    }

                    results.count = filteredArrayData.size();
                    results.values = filteredArrayData;
                    return results;
                }

                /** @noinspection unchecked*/
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    list = (List<Brand>) results.values;
                    notifyDataSetChanged();
                }
            };
        }
    }
}