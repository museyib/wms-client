package az.inci.bmsanbar.fragment;

import static az.inci.bmsanbar.fragment.StringDataHelper.getStringData;
import static az.inci.bmsanbar.util.UrlConstructor.addQueryParameters;
import static az.inci.bmsanbar.util.UrlConstructor.createUrl;

import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

import az.inci.bmsanbar.R;
import az.inci.bmsanbar.activity.AppBaseActivity;

public class PickReportHelper {
    public static void getPickReportData(AppBaseActivity activity, String reportName) {
        View view = activity.getLayoutInflater().inflate(R.layout.pick_date_dialog, null);
        EditText fromText = view.findViewById(R.id.from_date);
        EditText toText = view.findViewById(R.id.to_date);
        CheckBox actualCheckBox = view.findViewById(R.id.actual);

        fromText.setText(new Date(System.currentTimeMillis()).toString());
        toText.setText(new Date(System.currentTimeMillis()).toString());

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setView(view)
                .setTitle("Tarix intervalı")
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    String startDate = fromText.getText().toString();
                    String endDate = toText.getText().toString();
                    String url = createUrl(reportName, "report");
                    if (actualCheckBox.isChecked()) {
                        url = createUrl(reportName, "report-actual");
                    }
                    Map<String, String> parameters = new HashMap<>();
                    parameters.put("start-date", startDate);
                    parameters.put("end-date", endDate);
                    parameters.put("user-id", activity.appUser.getId());
                    url = addQueryParameters(url, parameters);
                    getStringData(activity, url, "Yığım hesabatı");
                });
        dialogBuilder.show();
    }
}
