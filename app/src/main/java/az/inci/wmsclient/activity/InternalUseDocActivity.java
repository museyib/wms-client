package az.inci.wmsclient.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;

import az.inci.wmsclient.AppConfig;
import az.inci.wmsclient.R;
import az.inci.wmsclient.model.Doc;

public class InternalUseDocActivity extends AppBaseActivity {

    ListView docListView;
    ImageButton add;

    List<Doc> docList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.internal_use_doc_layout);
        setEdgeToEdge();
        setTitle("Daxili istifadə");

        docListView = findViewById(R.id.doc_list);
        add = findViewById(R.id.add);

        add.setOnClickListener(v -> {
            Intent intent = new Intent(this, InternalUseTrxActivity.class);
            intent.putExtra("mode", AppConfig.NEW_MODE);
            startActivity(intent);
        });

        docListView.setOnItemClickListener((parent, view, position, id) -> {
            Doc doc = (Doc) parent.getItemAtPosition(position);
            Intent intent = new Intent(this, InternalUseTrxActivity.class);
            intent.putExtra("trxNo", doc.getTrxNo());
            intent.putExtra("notes", doc.getNotes());
            intent.putExtra("trxTypeId", doc.getTrxTypeId());
            intent.putExtra("whsCode", doc.getWhsCode());
            intent.putExtra("whsName", doc.getWhsName());
            intent.putExtra("expCenterCode", doc.getExpCenterCode());
            intent.putExtra("expCenterName", doc.getExpCenterName());
            intent.putExtra("amount", doc.getAmount());
            startActivity(intent);
        });
        AdapterView.OnItemLongClickListener itemLongClickListener = (parent, view, position, id) -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                    InternalUseDocActivity.this);
            dialogBuilder.setMessage("Silmək istəyirsiniz?")
                    .setPositiveButton("Bəli", (dialog1, which) -> {
                        Doc doc = (Doc) parent.getItemAtPosition(position);
                        dbHelper.deleteInternalUseDoc(doc.getTrxNo());
                        InternalUseDocActivity.this.loadData();
                    })
                    .setNegativeButton("Xeyr", null)
                    .create();
            dialogBuilder.show();
            return true;
        };

        docListView.setOnItemLongClickListener(itemLongClickListener);

        loadData();

        loadFooter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
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

        menu.findItem(R.id.pick_report).setVisible(false);
        menu.findItem(R.id.search).setVisible(false);
        menu.findItem(R.id.doc_list).setVisible(false);

        return true;
    }

    public void loadData() {
        docList = dbHelper.getInternalUseDocList();
        if (docList.isEmpty()) {
            findViewById(R.id.doc_list_scroll).setVisibility(View.INVISIBLE);
        } else {
            findViewById(R.id.doc_list_scroll).setVisibility(View.VISIBLE);
            DocAdapter adapter = new DocAdapter(this, R.layout.internal_use_doc_item_layout,
                    docList);
            docListView.setAdapter(adapter);
        }
    }


    class DocAdapter extends ArrayAdapter<Doc> {

        InternalUseDocActivity activity;
        List<Doc> list;

        DocAdapter(@NonNull Context context, int resourceId, @NonNull List<Doc> objects) {
            super(context, resourceId, objects);
            list = objects;
            activity = (InternalUseDocActivity) context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Doc doc = list.get(position);

            if (convertView == null) {
                convertView = getLayoutInflater()
                        .inflate(R.layout.internal_use_doc_item_layout, parent,
                                false);
            }

            TextView trxNo = convertView.findViewById(R.id.trx_no);
            TextView whsCode = convertView.findViewById(R.id.whs_code);
            TextView amount = convertView.findViewById(R.id.amount);

            trxNo.setText(doc.getTrxNo());
            whsCode.setText(doc.getWhsCode());
            amount.setText(String.format(Locale.getDefault(), "%.3f", doc.getAmount()));
            convertView.setTag(doc);

            return convertView;
        }
    }
}