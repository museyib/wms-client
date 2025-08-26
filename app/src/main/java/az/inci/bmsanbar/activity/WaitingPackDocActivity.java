package az.inci.bmsanbar.activity;

import static android.R.drawable.ic_dialog_alert;
import static az.inci.bmsanbar.util.UrlConstructor.addQueryParameters;
import static az.inci.bmsanbar.util.UrlConstructor.createUrl;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

public class WaitingPackDocActivity extends AppBaseActivity {

    List<Doc> docList;
    ListView docListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_pack_doc);
        setEdgeToEdge();
        docListView = findViewById(R.id.doc_list);
        docListView.setOnItemClickListener((adapterView, view, i, l) -> {
            Doc doc = (Doc) view.getTag();
            Intent intent = new Intent(this, WaitingPackTrxActivity.class);
            intent.putExtra("trxNo", doc.getTrxNo());
            intent.putExtra("orderTrxNo", doc.getPrevTrxNo());
            intent.putExtra("bpName", doc.getBpName());
            startActivity(intent);
        });
        loadFooter();
    }

    @Override
    public void loadData() {
        DocAdapter docAdapter = new DocAdapter(this, R.layout.pack_doc_item_layout, docList);
        docListView.setAdapter(docAdapter);
        if (docList.isEmpty()) {
            findViewById(R.id.header).setVisibility(View.GONE);
        } else {
            findViewById(R.id.header).setVisibility(View.VISIBLE);
        }
    }

    public void getNewDocs() {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("pack", "waiting-docs");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("user-id", appUser.getId());
            url = addQueryParameters(url, parameters);
            try {
                docList = httpClient.getListData(url, "GET", null, Doc[].class);
                if (docList != null) {
                    runOnUiThread(this::loadData);
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
    protected void onResume() {
        super.onResume();
        getNewDocs();
    }

    class DocAdapter extends ArrayAdapter<Doc> {
        List<Doc> list;

        DocAdapter(@NonNull Context context, int resourceId, @NonNull List<Doc> objects) {
            super(context, resourceId, objects);
            list = objects;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Doc doc = list.get(position);

            if (convertView == null) {
                convertView = getLayoutInflater()
                        .inflate(R.layout.pack_doc_item_layout, parent, false);
            }

            TextView trxNo = convertView.findViewById(R.id.trx_no);
            TextView itemCount = convertView.findViewById(R.id.item_count);
            TextView docDesc = convertView.findViewById(R.id.doc_description);
            TextView pickedItemCount = convertView.findViewById(R.id.picked_item_count);
            TextView bpName = convertView.findViewById(R.id.bp_name);
            TextView sbeName = convertView.findViewById(R.id.sbe_name);

            assert doc != null;
            trxNo.setText(doc.getPrevTrxNo());
            itemCount.setText(String.valueOf(doc.getItemCount()));
            docDesc.setText((!doc.getDescription().equals("null")) ? doc.getDescription() : "");
            pickedItemCount.setText(String.valueOf(doc.getPickedItemCount()));
            bpName.setText(doc.getBpName());
            sbeName.setText(doc.getSbeName());
            convertView.setTag(doc);

            return convertView;
        }
    }
}