package az.inci.wmsclient.activity;

import static android.R.drawable.ic_dialog_alert;
import static android.R.drawable.ic_dialog_info;
import static az.inci.wmsclient.AppConfig.APPROVE_MODE;
import static az.inci.wmsclient.AppConfig.CONFIRM_DELIVERY_MODE;
import static az.inci.wmsclient.AppConfig.INV_ATTRIBUTE_MODE;
import static az.inci.wmsclient.AppConfig.PACK_MODE;
import static az.inci.wmsclient.AppConfig.PICK_MODE;
import static az.inci.wmsclient.AppConfig.PRODUCT_APPROVE_MODE;
import static az.inci.wmsclient.AppConfig.PURCHASE_ORDER_MODE;
import static az.inci.wmsclient.AppConfig.SHIP_MODE;
import static az.inci.wmsclient.util.GlobalParameters.apiVersion;
import static az.inci.wmsclient.util.GlobalParameters.cameraScanning;
import static az.inci.wmsclient.util.GlobalParameters.connectionTimeout;
import static az.inci.wmsclient.util.GlobalParameters.imageUrl;
import static az.inci.wmsclient.util.GlobalParameters.serviceUrl;
import static az.inci.wmsclient.util.UrlConstructor.addQueryParameters;
import static az.inci.wmsclient.util.UrlConstructor.createUrl;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import az.inci.wmsclient.CustomException;
import az.inci.wmsclient.R;
import az.inci.wmsclient.model.User;
import az.inci.wmsclient.model.v2.LoginRequest;

public class MainActivity extends AppBaseActivity {
    private int mode;
    private String userId;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setEdgeToEdge();

        userId = preferences.getString("last_login_id", "");
        password = preferences.getString("last_login_password", "");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConfig();
    }

    private void loadConfig() {

        Properties properties = new Properties();
        try {
            properties.load(getAssets().open("app.properties"));
        } catch (IOException e) {
            apiVersion = "v4";
        }
        serviceUrl = preferences.getString("service_url", "http://185.129.0.46:8022");
        imageUrl = preferences.getString("image_url", "http://185.129.0.46:8025");
        connectionTimeout = Integer.parseInt(preferences.getString("connection_timeout", "5"));
        cameraScanning = preferences.getBoolean("camera_scanning", false);
        apiVersion = properties.getProperty("app.api-version");
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        MenuItem itemSettings = menu.findItem(R.id.settings);
        itemSettings.setOnMenuItemClickListener(item1 -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        });

        MenuItem itemUpdate = menu.findItem(R.id.update);
        itemUpdate.setOnMenuItemClickListener(item1 -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.update_version)
                    .setMessage(R.string.want_to_update)
                    .setNegativeButton(R.string.yes,
                            (dialogInterface, i) -> checkForNewVersion())
                    .setPositiveButton(R.string.no, null);
            AlertDialog dialog = dialogBuilder.create();

            dialog.show();
            return true;
        });
        MenuItem itemInfo = menu.findItem(R.id.inv_attributes);
        itemInfo.setOnMenuItemClickListener(item1 -> {
            showLoginDialog(INV_ATTRIBUTE_MODE);
            return true;
        });
        MenuItem deiceInfo = menu.findItem(R.id.device_info);
        deiceInfo.setOnMenuItemClickListener(item1 -> {
            showMessageDialog(getString(R.string.device_info), getDeviceIdString(), ic_dialog_info);
            return true;
        });
        MenuItem logs = menu.findItem(R.id.logs);
        logs.setOnMenuItemClickListener(item1 -> {
            startActivity(new Intent(MainActivity.this, LogViewActivity.class));
            return true;
        });
        return true;
    }

    public void openPickingDocs(View view) {
        showLoginDialog(PICK_MODE);
    }

    public void openPackingDocs(View view) {
        showLoginDialog(PACK_MODE);
    }

    public void openShippingDocs(View view) {
        showLoginDialog(SHIP_MODE);
    }

    public void openInvApproving(View view) {
        showLoginDialog(APPROVE_MODE);
    }

    public void openProductApproving(View view) {
        showLoginDialog(PRODUCT_APPROVE_MODE);
    }

    public void openConfirmDelivery(View view) {
        showLoginDialog(CONFIRM_DELIVERY_MODE);
    }

    public void openPurchaseOrders(View view) {
        showLoginDialog(PURCHASE_ORDER_MODE);
    }

    private void showLoginDialog(int mode) {
        this.mode = mode;
        View view = getLayoutInflater().inflate(R.layout.login_page, null);

        EditText idEdit = view.findViewById(R.id.id_edit);
        EditText passwordEdit = view.findViewById(R.id.password_edit);
        CheckBox fromServerCheck = view.findViewById(R.id.from_server_check);

        AtomicBoolean loginViaServer = new AtomicBoolean(false);
        fromServerCheck.setOnCheckedChangeListener(
                (buttonView, isChecked) -> loginViaServer.set(isChecked));

        idEdit.setText(userId);
        idEdit.selectAll();
        passwordEdit.setText(password);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setTitle(R.string.enter)
                .setView(view)
                .setPositiveButton(R.string.enter, (dialog, which) -> {
                    userId = idEdit.getText().toString().toUpperCase();
                    password = passwordEdit.getText().toString();

                    if (userId.isEmpty() || password.isEmpty()) {
                        showToastMessage(getString(R.string.username_or_password_not_entered));
                        showLoginDialog(mode);
                        playSound(SOUND_FAIL);
                    } else {
                        loginViaServer();
                    }
                });

        AlertDialog loginDialog = dialogBuilder.create();
        Objects.requireNonNull(loginDialog.getWindow())
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        loginDialog.show();
    }

    private void attemptLogin() {
        preferences.edit().putString("last_login_id", userId).apply();
        preferences.edit().putString("last_login_password", password).apply();
        Class<?> aClass;
        switch (mode) {
            case PICK_MODE:
                if (!appUser.isPickFlag()) {
                    showMessageDialog(getString(R.string.warning),
                            getString(R.string.not_allowed),
                            ic_dialog_alert);
                    playSound(SOUND_FAIL);
                    return;
                }
                aClass = PickDocActivity.class;
                break;
            case PACK_MODE:
                if (!appUser.isPackFlag()) {
                    showMessageDialog(getString(R.string.warning),
                            getString(R.string.not_allowed),
                            ic_dialog_alert);
                    playSound(SOUND_FAIL);
                    return;
                }
                aClass = PackDocActivity.class;
                break;
            case SHIP_MODE:
                if (!appUser.isLoadingFlag()) {
                    showMessageDialog(getString(R.string.warning),
                            getString(R.string.not_allowed),
                            ic_dialog_alert);
                    playSound(SOUND_FAIL);
                    return;
                }
                aClass = ShipDocActivity.class;
                break;
            case APPROVE_MODE:
                if (!appUser.isApproveFlag()) {
                    showMessageDialog(getString(R.string.warning),
                            getString(R.string.not_allowed),
                            ic_dialog_alert);
                    playSound(SOUND_FAIL);
                    return;
                }
                aClass = InternalUseDocActivity.class;
                break;
            case PRODUCT_APPROVE_MODE:
                if (!(appUser.isApproveFlag() || appUser.isApprovePrdFlag())) {
                    showMessageDialog(getString(R.string.warning),
                            getString(R.string.not_allowed),
                            ic_dialog_alert);
                    playSound(SOUND_FAIL);
                    return;
                }
                aClass = ProductApproveDocActivity.class;
                break;
            case INV_ATTRIBUTE_MODE:
                aClass = InventoryInfoActivity.class;
                break;
            case CONFIRM_DELIVERY_MODE:
                if (!appUser.isLoadingFlag()) {
                    showMessageDialog(getString(R.string.warning),
                            getString(R.string.not_allowed),
                            ic_dialog_alert);
                    playSound(SOUND_FAIL);
                    return;
                }
                aClass = ConfirmDeliveryActivity.class;
                break;
            case PURCHASE_ORDER_MODE:
                if (!appUser.isPurchaseOrdersFlag()) {
                    showMessageDialog(getString(R.string.warning),
                            getString(R.string.not_allowed),
                            ic_dialog_alert);
                    playSound(SOUND_FAIL);
                    return;
                }
                aClass = PurchaseOrdersActivity.class;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + mode);
        }
        Intent intent = new Intent(MainActivity.this, aClass);
        startActivity(intent);
    }

    private void loginViaServer() {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("user", "login");
            LoginRequest request = new LoginRequest();
            request.setUserId(userId);
            request.setPassword(password);
            try {
                appUser = httpClient.getSimpleObject(url, "POST", request, User.class);
                runOnUiThread(() -> {
                    if (appUser != null) {
                        appUser.setId(appUser.getId().toUpperCase());
                        attemptLogin();
                    }
                });
            } catch (CustomException e) {
                logger.logError(e.toString());
                runOnUiThread(() -> showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert));
            } finally {
                runOnUiThread(() -> showProgressDialog(false));
            }
        }).start();
    }

    private void checkForNewVersion() {
        showProgressDialog(true);

        new Thread(() -> {
            int version;
            try {
                version = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                version = 0;
            }
            String url = createUrl("app-version", "check");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("app-name", "WMSClient");
            parameters.put("current-version", String.valueOf(version));
            url = addQueryParameters(url, parameters);
            try {
                Boolean newVersionAvailable = httpClient.getSimpleObject(url, "GET", null, Boolean.class);
                runOnUiThread(() -> {
                    if (newVersionAvailable != null && newVersionAvailable)
                        getApkFile();
                    else
                        showMessageDialog(getString(R.string.info), getString(R.string.no_new_version), ic_dialog_info);
                });
            } catch (CustomException e) {
                logger.logError(e.toString());
                runOnUiThread(() -> showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert));
            } finally {
                runOnUiThread(() -> showProgressDialog(false));
            }
        }).start();
    }

    private void getApkFile() {
        showProgressDialog(true);
        new Thread(() -> {
            String url = createUrl("download");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("file-name", "WMSClient");
            url = addQueryParameters(url, parameters);
            try {
                String bytes = httpClient.getSimpleObject(url, "GET", null, String.class);
                if (bytes != null) {
                    byte[] fileBytes = android.util.Base64.decode(bytes, Base64.DEFAULT);
                    if (fileBytes != null)
                        runOnUiThread(() -> installApp(fileBytes));
                }
            } catch (CustomException e) {
                logger.logError(e.toString());
                runOnUiThread(() -> showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert));
            } finally {
                runOnUiThread(() -> showProgressDialog(false));
            }
        }).start();
    }

    private void installApp(byte[] bytes) {
        File file = new File(
                Environment.getExternalStorageDirectory().getPath() + "/WMSClient.apk");
        if (!file.exists()) {
            try {
                boolean newFile = file.createNewFile();
                if (!newFile) {
                    showMessageDialog(getString(R.string.info),
                            getString(R.string.error_occurred),
                            ic_dialog_info);
                    return;
                }
            } catch (IOException e) {
                showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert);
                return;
            }
        }

        try (FileOutputStream stream = new FileOutputStream(file)) {
            stream.write(bytes);
        } catch (Exception e) {
            showMessageDialog(getString(R.string.error), e.toString(), ic_dialog_alert);
            return;
        }

        Intent installIntent;
        Uri uri;
        installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        uri = FileProvider.getUriForFile(this, "az.inci.wmsclient.provider", file);
        installIntent.setData(uri);
        installIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(installIntent);
    }
}
