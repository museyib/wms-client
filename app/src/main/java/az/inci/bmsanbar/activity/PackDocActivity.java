package az.inci.bmsanbar.activity;

import static android.R.drawable.ic_dialog_info;
import static az.inci.bmsanbar.fragment.PickReportHelper.getPickReportData;
import static az.inci.bmsanbar.util.UrlConstructor.addQueryParameters;
import static az.inci.bmsanbar.util.UrlConstructor.createUrl;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import az.inci.bmsanbar.CustomException;
import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.Doc;
import az.inci.bmsanbar.model.Trx;

public class PackDocActivity extends AppBaseActivity implements SearchView.OnQueryTextListener {

    List<Doc> docList;
    ListView docListView;
    ImageButton newDocs;
    private SearchView searchView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pack_dock_layout);
        setEdgeToEdge();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                if (!searchView.isIconified())
                    searchView.setIconified(true);
                else
                    finish();
            }
        });

        docListView = findViewById(R.id.doc_list);
        docListView.setOnItemClickListener((parent, view, position, id1) -> {
            Intent intent = new Intent(this, PackTrxActivity.class);
            Doc doc = (Doc) view.getTag();
            intent.putExtra("trxNo", doc.getTrxNo());
            intent.putExtra("orderTrxNo", doc.getPrevTrxNo());
            intent.putExtra("bpName", doc.getBpName());
            intent.putExtra("notes", doc.getNotes());
            intent.putExtra("activeSeconds", doc.getActiveSeconds());
            startActivity(intent);
        });

        loadFooter();
        loadData();

        newDocs = findViewById(R.id.newDocs);
        newDocs.setOnClickListener(v -> loadTrxFromServer(1));

    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void loadData() {
        docList = dbHelper.getPackDocsByApproveUser(appUser.getId());
        DocAdapter docAdapter = new DocAdapter(this, R.layout.pack_doc_item_layout, docList);
        docListView.setAdapter(docAdapter);
        if (docList.isEmpty())
            findViewById(R.id.header).setVisibility(View.GONE);
        else
            findViewById(R.id.header).setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        DocAdapter adapter = (DocAdapter) docListView.getAdapter();
        if (adapter != null)
            adapter.getFilter().filter(s);
        return true;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.pick_menu, menu);

        MenuItem item = menu.findItem(R.id.inv_attributes);
        item.setOnMenuItemClickListener(item1 -> {
            startActivity(new Intent(this, InventoryInfoActivity.class));
            return true;
        });

        MenuItem report = menu.findItem(R.id.pick_report);
        report.setOnMenuItemClickListener(item1 -> {
            getPickReportData(this, "pack");
            return true;
        });

        MenuItem docList = menu.findItem(R.id.doc_list);
        docList.setOnMenuItemClickListener(item1 -> {
            startActivity(new Intent(this, WaitingPackDocActivity.class));
            return true;
        });

        MenuItem searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setOnQueryTextListener(this);
            searchView.setActivated(true);
        }
        return true;
    }

    private void loadTrxFromServer(int mode) {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("pack", "get-doc");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("approve-user", appUser.getId());
            parameters.put("mode", String.valueOf(mode));
            url = addQueryParameters(url, parameters);
            try {
                Doc doc = httpClient.getSimpleObject(url, "GET", null, Doc.class);
                runOnUiThread(() -> {
                    if (doc != null) {
                        dbHelper.addPackDoc(doc);
                        for (Trx trx : doc.getTrxList()) {
                            dbHelper.addPackTrx(trx);
                        }
                    } else {
                        showMessageDialog(getString(R.string.info), getString(R.string.no_data),
                                ic_dialog_info);
                        playSound(SOUND_FAIL);
                    }
                    loadData();
                });
            } catch (CustomException e) {
                logger.logError(e.toString());
                runOnUiThread(() -> {
                    showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_info);
                    playSound(SOUND_FAIL);
                });
            } finally {
                runOnUiThread(() -> showProgressDialog(false));
            }
        }).start();
    }

    class DocAdapter extends ArrayAdapter<Doc> implements Filterable {
        PackDocActivity activity;
        List<Doc> list;

        DocAdapter(@NonNull Context context, int resourceId, @NonNull List<Doc> objects) {
            super(context, resourceId, objects);
            list = objects;
            activity = (PackDocActivity) context;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Doc doc = list.get(position);

            if (convertView == null)
                convertView = getLayoutInflater()
                        .inflate(R.layout.pack_doc_item_layout, parent, false);

            TextView trxNo = convertView.findViewById(R.id.trx_no);
            TextView itemCount = convertView.findViewById(R.id.item_count);
            TextView docDesc = convertView.findViewById(R.id.doc_description);
            TextView pickedItemCount = convertView.findViewById(R.id.picked_item_count);
            TextView bpName = convertView.findViewById(R.id.bp_name);
            TextView sbeName = convertView.findViewById(R.id.sbe_name);
            TextView whsCode = convertView.findViewById(R.id.whs_code);

            trxNo.setText(doc.getPrevTrxNo());
            itemCount.setText(String.valueOf(doc.getItemCount()));
            docDesc.setText(doc.getDescription());
            pickedItemCount.setText(String.valueOf(doc.getPickedItemCount()));
            bpName.setText(doc.getBpName());
            sbeName.setText(doc.getSbeName());
            whsCode.setText(doc.getWhsCode());
            convertView.setTag(doc);

            return convertView;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    List<Doc> filteredArrayData = new ArrayList<>();
                    constraint = constraint.toString().toLowerCase();

                    for (Doc doc : activity.docList) {
                        if (doc.getTrxNo()
                                .concat(doc.getPrevTrxNo())
                                .concat(doc.getBpName())
                                .concat(doc.getSbeName())
                                .concat(doc.getDescription())
                                .toLowerCase()
                                .contains(constraint))
                            filteredArrayData.add(doc);
                    }

                    results.count = filteredArrayData.size();
                    results.values = filteredArrayData;
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    list = (List<Doc>) results.values;
                    notifyDataSetChanged();
                }
            };
        }
    }
}