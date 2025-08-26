package az.inci.bmsanbar.activity;

import static android.R.drawable.ic_dialog_alert;
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
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import az.inci.bmsanbar.CustomException;
import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.Doc;
import az.inci.bmsanbar.model.Trx;

public class PickDocActivity extends AppBaseActivity {

    List<Doc> docList;
    ListView docListView;
    ImageButton newDocs;
    ImageButton newDocsByUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pick_doc_layout);
        setEdgeToEdge();
        docListView = findViewById(R.id.doc_list);

        loadFooter();
        loadData();

        docListView.setOnItemClickListener((parent, view, position, id1) -> {
            Intent intent = new Intent(this, PickTrxActivity.class);
            Doc doc = (Doc) parent.getItemAtPosition(position);
            intent.putExtra("trxNo", doc.getTrxNo());
            intent.putExtra("prevTrxNo", doc.getPrevTrxNo());
            intent.putExtra("pickGroup", doc.getPickGroup());
            intent.putExtra("pickArea", doc.getPickArea());
            startActivity(intent);
        });

        newDocs = findViewById(R.id.newDocs);
        newDocs.setOnClickListener(v -> loadTrxFromServer(1));

        newDocsByUserId = findViewById(R.id.newDocsByUserId);
        newDocsByUserId.setOnClickListener(v -> loadTrxFromServer(0));
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
        docList = dbHelper.getPickDocsByPickUser(appUser.getId());
        DocAdapter docAdapter = new DocAdapter(this, docList);
        docListView.setAdapter(docAdapter);
        if (docList.isEmpty())
            findViewById(R.id.header).setVisibility(View.GONE);
        else
            findViewById(R.id.header).setVisibility(View.VISIBLE);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.pick_menu, menu);
        MenuItem attributes = menu.findItem(R.id.inv_attributes);
        attributes.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        attributes.setOnMenuItemClickListener(item1 -> {
            startActivity(new Intent(this, InventoryInfoActivity.class));
            return true;
        });

        MenuItem report = menu.findItem(R.id.pick_report);
        report.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        report.setOnMenuItemClickListener(item1 -> {
            getPickReportData(this, "pick");
            return true;
        });

        menu.findItem(R.id.search).setVisible(false);
        menu.findItem(R.id.doc_list).setVisible(false);

        return true;
    }

    private void loadTrxFromServer(int mode) {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("pick", "get-doc");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("pick-user", appUser.getId());
            parameters.put("mode", String.valueOf(mode));
            url = addQueryParameters(url, parameters);

            try {
                Doc doc = httpClient.getSimpleObject(url, "GET", null, Doc.class);
                runOnUiThread(() -> {
                    if (doc != null) {
                        dbHelper.addPickDoc(doc);
                        for (Trx trx : doc.getTrxList()) {
                            dbHelper.addPickTrx(trx);
                        }
                    } else {
                        showMessageDialog(getString(R.string.info), getString(R.string.no_data),
                                ic_dialog_info);
                        playSound(SOUND_FAIL);
                    }
                    loadData();
                });
            } catch (CustomException e) {
                runOnUiThread(() -> {
                    showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert);
                    playSound(SOUND_FAIL);
                });
            } finally {
                runOnUiThread(() -> showProgressDialog(false));
            }
        }).start();
    }

    class DocAdapter extends ArrayAdapter<Doc> {
        DocAdapter(@NonNull Context context, @NonNull List<Doc> objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Doc doc = getItem(position);

            if (convertView == null)
                convertView = getLayoutInflater()
                        .inflate(R.layout.pick_doc_item_layout, parent, false);

            TextView trxNo = convertView.findViewById(R.id.trx_no);
            TextView itemCount = convertView.findViewById(R.id.item_count);
            TextView pickedItemCount = convertView.findViewById(R.id.picked_item_count);
            TextView pickArea = convertView.findViewById(R.id.pick_area);
            TextView docDesc = convertView.findViewById(R.id.doc_description);
            TextView whsCode = convertView.findViewById(R.id.whs_code);

            assert doc != null;
            trxNo.setText(doc.getPrevTrxNo());
            itemCount.setText(String.valueOf(doc.getItemCount()));
            pickedItemCount.setText(String.valueOf(doc.getPickedItemCount()));
            pickArea.setText(doc.getPickArea());
            docDesc.setText(doc.getDescription());
            whsCode.setText(doc.getWhsCode());

            return convertView;
        }
    }

}
