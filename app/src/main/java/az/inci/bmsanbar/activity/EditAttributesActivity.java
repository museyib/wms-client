package az.inci.bmsanbar.activity;

import static android.R.drawable.ic_dialog_alert;
import static az.inci.bmsanbar.util.UrlConstructor.addQueryParameters;
import static az.inci.bmsanbar.util.UrlConstructor.createUrl;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import az.inci.bmsanbar.CustomException;
import az.inci.bmsanbar.R;
import az.inci.bmsanbar.model.InvAttribute;
import az.inci.bmsanbar.model.v2.ResponseMessage;

public class EditAttributesActivity extends AppBaseActivity {

    ListView attributeListView;
    String invCode;
    String invName;
    List<InvAttribute> attributeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_attributes);
        setEdgeToEdge();

        attributeListView = findViewById(R.id.attribute_list);

        invCode = getIntent().getStringExtra("invCode");
        invName = getIntent().getStringExtra("invName");
        setTitle(invName);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    public void loadData() {
        showProgressDialog(true);
        new Thread(() -> {

            String url = createUrl("inv", "attribute-list-by-whs");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("inv-code", invCode);
            parameters.put("user-id", appUser.getId());
            url = addQueryParameters(url, parameters);
            try {
                attributeList = httpClient.getListData(url, "GET", null, InvAttribute[].class);
                if (attributeList != null) runOnUiThread(this::updatePage);
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

    public void updatePage() {
        if (!attributeList.isEmpty()) {
            findViewById(R.id.save).setOnClickListener(v -> updateAttributes());
        }
        AttributeAdapter adapter = new AttributeAdapter(this, attributeList);
        attributeListView.setAdapter(adapter);

        AdapterView.OnItemClickListener itemClickListener = (parent, view, position, id) -> {
            View dialog = LayoutInflater.from(EditAttributesActivity.this)
                    .inflate(R.layout.edit_attribute_dialog, parent, false);
            InvAttribute attribute = attributeList.get(position);
            EditAttributesActivity.this.showEditAttributeDialog(attribute, dialog);
        };
        attributeListView.setOnItemClickListener(itemClickListener);
    }

    private void showEditAttributeDialog(InvAttribute attribute, View dialog) {
        TextView nameText = dialog.findViewById(R.id.attribute_name);
        EditText valueEdit = dialog.findViewById(R.id.attribute_value);
        nameText.setText(attribute.getAttributeName());
        valueEdit.setText(attribute.getAttributeValue());
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(dialog).setPositiveButton("OK", (dialog1, which) -> {
            attribute.setAttributeValue(valueEdit.getText().toString());
            updatePage();
        }).create();
        dialogBuilder.show();
    }

    private void updateAttributes() {
        showProgressDialog(true);
        new Thread(() -> {

            String url = createUrl("inv", "update-attributes");
            try {
                ResponseMessage message = httpClient.executeUpdate(url, attributeList);
                runOnUiThread(() -> {
                    showMessageDialog(message.getTitle(), message.getBody(), message.getIconId());
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

    private class AttributeAdapter extends ArrayAdapter<InvAttribute> {
        Context context;
        List<InvAttribute> attributeList;

        public AttributeAdapter(@NonNull Context context, @NonNull List<InvAttribute> objects) {
            super(context, 0, objects);
            this.context = context;
            attributeList = objects;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            InvAttribute attribute = attributeList.get(position);
            if (convertView == null) {
                convertView = getLayoutInflater()
                        .inflate(R.layout.attribute_item, parent, false);
            }

            ViewHolder holder = new ViewHolder();
            holder.nameEdit = convertView.findViewById(R.id.attribute_name);
            holder.valueCheck = convertView.findViewById(R.id.attribute_value_check);
            holder.valueEdit = convertView.findViewById(R.id.attribute_value);
            holder.nameEdit.setText(attribute.getAttributeName());

            if (attribute.getAttributeType().equals("BIT")) {
                holder.valueCheck.setChecked(attribute.getAttributeValue().equals("1"));
                holder.valueCheck.setOnCheckedChangeListener(
                        (buttonView, isChecked) -> attribute.setAttributeValue(
                                String.valueOf(isChecked ? 1 : 0)));
                holder.valueCheck.setVisibility(View.VISIBLE);
                holder.valueEdit.setVisibility(View.GONE);
            } else {
                holder.valueEdit.setText(attribute.getAttributeValue());
                holder.valueEdit.setVisibility(View.VISIBLE);
                holder.valueCheck.setVisibility(View.GONE);
            }

            if (((EditAttributesActivity) context).appUser.isLocationFlag() &&
                    (attribute.getAttributeId().equals("AT010") ||
                            attribute.getAttributeId().equals("AT011")) ||
                    !((EditAttributesActivity) context).appUser.isLocationFlag()) {
                convertView.setVisibility(View.VISIBLE);
            } else {
                convertView.setVisibility(View.GONE);
            }

            return convertView;
        }

        private class ViewHolder {
            TextView nameEdit;
            TextView valueEdit;
            CheckBox valueCheck;
        }
    }
}