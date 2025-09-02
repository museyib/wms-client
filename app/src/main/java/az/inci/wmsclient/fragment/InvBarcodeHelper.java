package az.inci.wmsclient.fragment;

import static android.R.drawable.ic_dialog_alert;
import static android.R.drawable.ic_dialog_info;
import static az.inci.wmsclient.activity.AppBaseActivity.SOUND_FAIL;
import static az.inci.wmsclient.util.UrlConstructor.addQueryParameters;
import static az.inci.wmsclient.util.UrlConstructor.createUrl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import az.inci.wmsclient.CustomException;
import az.inci.wmsclient.R;
import az.inci.wmsclient.activity.AppBaseActivity;
import az.inci.wmsclient.model.InvBarcode;
import az.inci.wmsclient.util.HttpClient;

public class InvBarcodeHelper {
    public static void requestInvBarcode(AppBaseActivity activity, String barcode, Consumer<InvBarcode> consumer) {
        activity.showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("inv", "inv-barcode");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("barcode", barcode);
            url = addQueryParameters(url, parameters);

            try {
                HttpClient httpClient = HttpClient.getInstance(activity);
                InvBarcode invBarcode = httpClient.getSimpleObject(url, "GET", null, InvBarcode.class);

                if (invBarcode != null) activity.runOnUiThread(() -> {
                    if (invBarcode.getBarcode() == null) {
                        activity.showMessageDialog(activity.getString(R.string.info), activity.getString(R.string.good_not_found), ic_dialog_info);
                        activity.playSound(SOUND_FAIL);
                    } else consumer.accept(invBarcode);
                });
            } catch (CustomException e) {
                activity.runOnUiThread(() -> {
                    activity.showMessageDialog(activity.getString(R.string.error), e.toString(), ic_dialog_alert);
                    activity.playSound(SOUND_FAIL);
                });
            } finally {
                activity.runOnUiThread(() -> activity.showProgressDialog(false));
            }
        }).start();
    }
}
