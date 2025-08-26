package az.inci.bmsanbar.fragment;

import static android.R.drawable.ic_dialog_alert;
import static android.R.drawable.ic_dialog_info;
import static az.inci.bmsanbar.activity.AppBaseActivity.SOUND_FAIL;
import static az.inci.bmsanbar.util.UrlConstructor.addQueryParameters;
import static az.inci.bmsanbar.util.UrlConstructor.createUrl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import az.inci.bmsanbar.CustomException;
import az.inci.bmsanbar.R;
import az.inci.bmsanbar.activity.AppBaseActivity;
import az.inci.bmsanbar.model.InvBarcode;
import az.inci.bmsanbar.util.HttpClient;

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
