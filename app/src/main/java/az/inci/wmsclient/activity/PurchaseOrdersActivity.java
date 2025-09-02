package az.inci.wmsclient.activity;

import static android.R.drawable.ic_dialog_alert;
import static az.inci.wmsclient.util.UrlConstructor.addQueryParameters;
import static az.inci.wmsclient.util.UrlConstructor.createUrl;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import az.inci.wmsclient.CustomException;
import az.inci.wmsclient.R;
import az.inci.wmsclient.model.v3.PurchaseDoc;

public class PurchaseOrdersActivity extends AppBaseActivity implements SearchView.OnQueryTextListener {

    private List<PurchaseDoc> docList;
    private RecyclerView dataListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_orders);
        setEdgeToEdge();
        dataListView = findViewById(R.id.data_list);
        Button refresh = findViewById(R.id.refresh);
        dataListView.setLayoutManager(new LinearLayoutManager(this));

        refresh.setOnClickListener(v -> {
            getDocList();
        });
        loadFooter();
        getDocList();
    }

    private void getDocList() {
        showProgressDialog(true);
        new Thread(() -> {
            Map<String, String> parameters = new HashMap<>();
            parameters.put("user-id", appUser.getId());
            String url = addQueryParameters(createUrl("purchase", "doc"), parameters);
            try {
                docList = httpClient.getListData(url, "GET", null, PurchaseDoc[].class);
                if (docList != null) {
                    runOnUiThread(() -> {
                        PurchaseDocAdapter adapter = new PurchaseDocAdapter(this, docList);
                        dataListView.setAdapter(adapter);
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

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        PurchaseDocAdapter adapter = (PurchaseDocAdapter) dataListView.getAdapter();
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

    private class PurchaseDocAdapter extends RecyclerView.Adapter<PurchaseDocAdapter.ViewHolder> implements Filterable {
        private final Context context;
        private List<PurchaseDoc> localDataList;
        private View itemView;

        public PurchaseDocAdapter(Context context, List<PurchaseDoc> localDataList) {
            this.context = context;

            this.localDataList = localDataList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            itemView = getLayoutInflater().inflate(R.layout.purchase_order_item, parent, false);

            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PurchaseDoc doc = localDataList.get(position);
            holder.trxNo.setText(doc.getTrxNo());
            holder.trxDate.setText(doc.getTrxDate());
            holder.seller.setText(String.format("%s - %s", doc.getBpCode(), doc.getBpName()));
            holder.sbe.setText(String.format("%s - %s", doc.getSbeCode(), doc.getSbeName()));
            holder.description.setText(doc.getDescription());
            holder.whs.setText(doc.getWhsCode());

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, PurchaseTrxActivity.class);
                intent.putExtra("trxNo", doc.getTrxNo());
                context.startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return localDataList.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    List<PurchaseDoc> filteredData = new ArrayList<>();
                    constraint = constraint.toString().toLowerCase();


                    for (PurchaseDoc doc : docList) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(doc.getBpName());
                        sb.append(doc.getSbeName());
                        sb.append(doc.getBpCode());
                        sb.append(doc.getSbeCode());
                        sb.append(doc.getDescription());
                        if (sb.toString().contains(constraint)) {
                            filteredData.add(doc);
                        }
                    }

                    filterResults.count = filteredData.size();
                    filterResults.values = filteredData;
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    localDataList = (List<PurchaseDoc>) results.values;
                    notifyDataSetChanged();
                }
            };
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView trxNo;
            TextView trxDate;
            TextView seller;
            TextView sbe;
            TextView description;
            TextView whs;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);

                trxNo = itemView.findViewById(R.id.trx_no);
                trxDate = itemView.findViewById(R.id.trx_date);
                seller = itemView.findViewById(R.id.seller);
                sbe = itemView.findViewById(R.id.sbe);
                description = itemView.findViewById(R.id.description);
                whs = itemView.findViewById(R.id.whs);
            }
        }
    }
}