package az.inci.bmsanbar.fragment;

import static android.R.drawable.ic_dialog_alert;
import static android.R.drawable.ic_dialog_info;

import az.inci.bmsanbar.CustomException;
import az.inci.bmsanbar.R;
import az.inci.bmsanbar.activity.AppBaseActivity;
import az.inci.bmsanbar.util.HttpClient;

public class StringDataHelper {
    public static void getStringData(AppBaseActivity activity, String url, String title) {
        activity.showProgressDialog(true);
        new Thread(() -> {
            try {
                HttpClient httpClient = HttpClient.getInstance(activity);
                String result = httpClient.getSimpleObject(url, "GET", null, String.class);
                activity.runOnUiThread(() -> activity.showMessageDialog(title, result, ic_dialog_info));
            } catch (CustomException e) {
                activity.runOnUiThread(() -> activity.showMessageDialog(activity.getString(R.string.error), e.toString(), ic_dialog_alert));
            } finally {
                activity.runOnUiThread(() -> activity.showProgressDialog(false));
            }
        }).start();
    }
}
