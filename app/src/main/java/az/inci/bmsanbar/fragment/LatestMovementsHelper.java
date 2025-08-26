package az.inci.bmsanbar.fragment;

import static android.R.drawable.ic_dialog_alert;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static az.inci.bmsanbar.util.UrlConstructor.addQueryParameters;
import static az.inci.bmsanbar.util.UrlConstructor.createUrl;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import az.inci.bmsanbar.CustomException;
import az.inci.bmsanbar.R;
import az.inci.bmsanbar.activity.AppBaseActivity;
import az.inci.bmsanbar.model.v3.LatestMovementItem;
import az.inci.bmsanbar.util.HttpClient;
import az.inci.bmsanbar.util.Logger;

public class LatestMovementsHelper {
    public static void showInventoryHistory(String invCode, String whsCode, AppBaseActivity activity) {
        LayoutInflater layoutInflater = LayoutInflater.from(activity);
        View view = layoutInflater.inflate(R.layout.result_list_dialog, null);
        Spinner spinner = view.findViewById(R.id.top_spinner);
        spinner.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, Arrays.asList(5, 10, 15, 20, 25, 30)));
        spinner.setVisibility(VISIBLE);
        ListView listView = view.findViewById(R.id.result_list);
        SearchView searchView = view.findViewById(R.id.search);
        searchView.setVisibility(GONE);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        AlertDialog dialog = dialogBuilder.setTitle("Son hərəkət tarixçəsi").setView(view).create();
        dialog.show();

        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            boolean spinnerChanged = false;
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinnerChanged)
                    refreshLatestMovements(activity, invCode, whsCode, (int) parent.getItemAtPosition(position), listView);
                else spinnerChanged = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        refreshLatestMovements(activity, invCode, whsCode, 5, listView);
    }
    public static void showInventoryHistory(AppBaseActivity activity, String invCode, String whsCode) {
        LayoutInflater layoutInflater = LayoutInflater.from(activity);
        View view = layoutInflater.inflate(R.layout.result_list_dialog, null);
        Spinner spinner = view.findViewById(R.id.top_spinner);
        spinner.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, Arrays.asList(5, 10, 15, 20, 25, 30)));
        spinner.setVisibility(VISIBLE);
        ListView listView = view.findViewById(R.id.result_list);
        SearchView searchView = view.findViewById(R.id.search);
        searchView.setVisibility(GONE);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        AlertDialog dialog = dialogBuilder.setTitle("Son hərəkət tarixçəsi").setView(view).create();
        dialog.show();

        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            boolean spinnerChanged = false;
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinnerChanged)
                    refreshLatestMovements(activity, invCode, whsCode, (int) parent.getItemAtPosition(position), listView);
                else spinnerChanged = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        refreshLatestMovements(activity, invCode, whsCode, 5, listView);
    }

    private static void refreshLatestMovements(AppBaseActivity activity, String invCode, String whsCode, int top, ListView listView) {
        Logger logger = new Logger(activity);
        activity.showProgressDialog(true);
        new Thread(() -> {
            Map<String, String> parameters = new HashMap<>();
            parameters.put("inv-code", invCode);
            parameters.put("whs-code", whsCode);
            parameters.put("top", String.valueOf(top));
            String url = addQueryParameters(createUrl("inv", "latest-movements"), parameters);
            try {
                HttpClient httpClient = HttpClient.getInstance(activity);
                List<LatestMovementItem> latestMovementItems = httpClient.getListData(url, "GET", null, LatestMovementItem[].class);
                activity.runOnUiThread(() -> {
                    MovementAdapter adapter = (MovementAdapter) listView.getAdapter();
                    if (adapter == null) {
                        adapter = new MovementAdapter(activity, R.layout.latest_movements_layout_item, latestMovementItems);
                        listView.setAdapter(adapter);
                    } else {
                        adapter.clear();
                        adapter.addAll(latestMovementItems);
                        adapter.notifyDataSetChanged();
                    }
                });
            } catch (CustomException e) {
                logger.logError(e.toString());
                activity.runOnUiThread(() -> activity.showMessageDialog(activity.getString(R.string.error), e.toString(), ic_dialog_alert));
            } finally {
                activity.runOnUiThread(() -> activity.showProgressDialog(false));
            }
        }).start();
    }

    private static class MovementAdapter extends ArrayAdapter<LatestMovementItem> {
        private final LayoutInflater layoutInflater;
        private final List<LatestMovementItem> latestMovementItems;
        private final int resource;

        public MovementAdapter(@NonNull Context context, int resource, @NonNull List<LatestMovementItem> objects) {
            super(context, resource, objects);
            this.layoutInflater = LayoutInflater.from(context);
            this.resource = resource;
            this.latestMovementItems = objects;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable @org.jetbrains.annotations.Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null)
                convertView = layoutInflater.inflate(resource, parent, false);
            MovementAdapter.ViewHolder viewHolder = (MovementAdapter.ViewHolder) convertView.getTag();
            if (viewHolder == null) {
                viewHolder = new ViewHolder();
                viewHolder.trxNo = convertView.findViewById(R.id.trx_no);
                viewHolder.trxDate = convertView.findViewById(R.id.trx_date);
                viewHolder.quantity = convertView.findViewById(R.id.quantity);
            }
            LatestMovementItem item = latestMovementItems.get(position);
            convertView.setTag(viewHolder);

            viewHolder.trxNo.setText(item.getTrxNo());
            viewHolder.trxDate.setText(item.getTrxDate());
            viewHolder.quantity.setText(String.valueOf(item.getQuantity()));

            return convertView;
        }

        static class ViewHolder {
            TextView trxNo;
            TextView trxDate;
            TextView quantity;
        }
    }
}
