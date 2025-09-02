package az.inci.wmsclient.activity;

import static android.R.drawable.ic_dialog_alert;
import static az.inci.wmsclient.fragment.StringDataHelper.getStringData;
import static az.inci.wmsclient.util.UrlConstructor.addQueryParameters;
import static az.inci.wmsclient.util.UrlConstructor.createUrl;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import az.inci.wmsclient.CustomException;
import az.inci.wmsclient.R;
import az.inci.wmsclient.model.Trx;

public class WaitingPackTrxActivity extends AppBaseActivity {

    List<Trx> trxList;
    ListView trxListView;
    String trxNo;
    String orderTrxNo;
    String bpName;
    private DecimalFormat decimalFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_pack_trx);
        setEdgeToEdge();
        decimalFormat = new DecimalFormat();
        decimalFormat.setGroupingUsed(false);
        trxListView = findViewById(R.id.trx_list);

        loadFooter();

        Intent intent = getIntent();
        trxNo = intent.getStringExtra("trxNo");
        orderTrxNo = intent.getStringExtra("orderTrxNo");
        bpName = intent.getStringExtra("bpName");
        setTitle(orderTrxNo + ": " + bpName);

        trxListView.setOnItemClickListener((parent, view, position, id) -> {
            Trx trx = (Trx) view.getTag();
            showInfoDialog(trx);
        });

        getData();
    }

    private void showInfoDialog(Trx trx) {
        String info = trx.getNotes().replaceAll("; ", "\n");
        info = info.replaceAll("\\\\n", "\n");
        info += "\n\nÖlçü vahidi: " + trx.getUom();
        info += "\n\nBrend: " + trx.getInvBrand();
        if (!TextUtils.isEmpty(trx.getPickUser())) {
            info += "\n\nYığan: " + trx.getPickUser();
        } else {
            info += "\n\nYığım qrupu: " + trx.getPickGroup();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Məlumat");
        builder.setMessage(info);
        builder.setPositiveButton("Şəkil", (dialog, which) -> {
            Intent photoIntent = new Intent(this, PhotoActivity.class);
            photoIntent.putExtra("invCode", trx.getInvCode());
            photoIntent.putExtra("notes", trx.getNotes());
            startActivity(photoIntent);
        });
        builder.setNeutralButton("Say", (dialog, which) -> {
            String url = createUrl("inv", "qty");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("whs-code", trx.getWhsCode());
            parameters.put("inv-code", trx.getInvCode());
            url = addQueryParameters(url, parameters);
            getStringData(this, url, "Anbarda say");
        });
        builder.show();
    }

    public void getData() {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("pack", "waiting-doc-items");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("trx-no", trxNo);
            url = addQueryParameters(url, parameters);
            try {
                trxList = httpClient.getListData(url, "GET", null, Trx[].class);
                if (trxList != null) runOnUiThread(this::loadData);
            } catch (CustomException e) {
                logger.logError(e.toString());
                runOnUiThread(() -> showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert));
            } finally {
                runOnUiThread(() -> showProgressDialog(false));
            }
        }).start();
    }

    @Override
    protected void loadData() {
        if (!trxList.isEmpty()) {
            TrxAdapter trxAdapter = new TrxAdapter(this, R.layout.pack_trx_item_layout, trxList);
            trxListView.setAdapter(trxAdapter);
        }
    }

    class TrxAdapter extends ArrayAdapter<Trx> {
        WaitingPackTrxActivity activity;
        List<Trx> list;

        TrxAdapter(@NonNull Context context, int resourceId, @NonNull List<Trx> objects) {
            super(context, resourceId, objects);
            list = objects;
            activity = (WaitingPackTrxActivity) context;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Trx trx = list.get(position);

            if (convertView == null) {
                convertView = getLayoutInflater()
                        .inflate(R.layout.waiting_pack_trx_item_layout, parent,
                                false);
            }

            switch (trx.getPickStatus()) {
                case "P":
                    convertView.setBackgroundColor(Color.GREEN);
                    break;
                case "C":
                    convertView.setBackgroundColor(Color.RED);
                    break;
                case "I":
                    convertView.setBackgroundColor(Color.YELLOW);
                    break;
                case "A":
                    convertView.setBackgroundColor(Color.CYAN);
                    break;
                case "R":
                    convertView.setBackgroundColor(Color.LTGRAY);
                    break;
            }

            TextView invCode = convertView.findViewById(R.id.inv_code);
            TextView invName = convertView.findViewById(R.id.inv_name);
            TextView invBrandView = convertView.findViewById(R.id.inv_brand);
            TextView qty = convertView.findViewById(R.id.qty);
            TextView pickedQty = convertView.findViewById(R.id.picked);
            TextView status = convertView.findViewById(R.id.status);

            invCode.setText(trx.getInvCode());
            invName.setText(trx.getInvName());
            invBrandView.setText(trx.getInvBrand());
            qty.setText(activity.decimalFormat.format(trx.getQty()));
            pickedQty.setText(activity.decimalFormat.format(trx.getPickedQty()));
            status.setText(trx.getPickStatus());
            convertView.setTag(trx);

            return convertView;
        }
    }
}