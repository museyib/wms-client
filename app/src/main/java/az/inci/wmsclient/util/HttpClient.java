package az.inci.wmsclient.util;

import static android.R.drawable.ic_dialog_alert;
import static android.R.drawable.ic_dialog_info;
import static az.inci.wmsclient.util.GlobalParameters.connectionTimeout;
import static az.inci.wmsclient.util.GlobalParameters.jwt;
import static az.inci.wmsclient.util.UrlConstructor.createUrl;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import az.inci.wmsclient.CustomException;
import az.inci.wmsclient.model.v2.CustomResponse;
import az.inci.wmsclient.model.v2.ResponseMessage;
import az.inci.wmsclient.security.AuthenticationRequest;
import az.inci.wmsclient.security.AuthenticationResponse;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HttpClient {
    private final SharedPreferences preferences;
    private final Logger logger;
    private final Gson gson = new Gson();
    private final Type responseType = new TypeToken<CustomResponse>() {}.getType();

    private final String username;
    private final String password;
    private final String secretKey;

    private static HttpClient instance;

    public static synchronized HttpClient getInstance(Context context) {
        if (instance == null) instance = new HttpClient(context);
        return instance;
    }

    public HttpClient(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        jwt = preferences.getString("jwt", "");
        logger = new Logger(context);


        Properties properties = new Properties();
        try {
            properties.load(context.getAssets().open("app.properties"));
        } catch (IOException ignored) {
        }

        username = properties.getProperty("app.username");
        password = properties.getProperty("app.password");
        secretKey = properties.getProperty("app.secret-key");
    }


    public String getNewToken() throws CustomException {
        AuthenticationRequest request = new AuthenticationRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setSecretKey(secretKey);
        String url = createUrl("authenticate");
        AuthenticationResponse authenticationResponse;
        authenticationResponse = getSimpleObject(url, "POST", request, AuthenticationResponse.class);
        return authenticationResponse.getToken();
    }

    private Response sendRequest(URL url, String method, @Nullable Object requestBodyData) throws IOException, CustomException {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        clientBuilder.connectTimeout(connectionTimeout, TimeUnit.SECONDS);
        OkHttpClient httpClient = clientBuilder.build();

        Request request;

        if (method.equals("POST")) {
            RequestBody requestBody = RequestBody.create(MediaType.get("application/json;charset=UTF-8"), new Gson().toJson(requestBodyData));
            request = new Request.Builder().post(requestBody).header("Authorization", "Bearer " + jwt).url(url).build();
        } else {
            request = new Request.Builder().get().header("Authorization", "Bearer " + jwt).url(url).build();
        }

        Response response = httpClient.newCall(request).execute();
        if (response.code() == 403) {
            jwt = getNewToken();
            preferences.edit().putString("jwt", jwt).apply();
            response = sendRequest(url, "POST", requestBodyData);
        }
        return response;
    }

    public <T> T getSimpleObject(String url, String method, Object request, Class<T> tClass) throws CustomException {
        try (Response httpResponse = sendRequest(new URL(url), method, request)) {
            if (httpResponse.code() == 200) {
                ResponseBody responseBody = httpResponse.body();
                CustomResponse response = gson.fromJson(Objects.requireNonNull(responseBody).string(), responseType);
                if (response.getStatusCode() == 0)
                    return gson.fromJson(gson.toJson(response.getData()), tClass);
                else if (response.getStatusCode() == 2) {
                    logger.logError(response.getDeveloperMessage());
                    throw new CustomException(response.getDeveloperMessage());
                } else {
                    logger.logError(response.getDeveloperMessage() + ": " + response.getSystemMessage());
                    throw new CustomException(response.getDeveloperMessage() + ": " + response.getSystemMessage());
                }
            } else {
                String message = httpResponse.toString();
                logger.logError(message);
                throw new CustomException(message);
            }
        } catch (IOException e) {
            logger.logError(e.toString());
            throw new CustomException(e);
        }
    }

    public <T> List<T> getListData(String url, String method, Object request, Class<T[]> tClass) throws CustomException {
        try (Response httpResponse = sendRequest(new URL(url), method, request)) {
            if (httpResponse.code() == 200) {
                ResponseBody responseBody = httpResponse.body();
                CustomResponse response = gson.fromJson(Objects.requireNonNull(responseBody).string(), responseType);
                if (response.getStatusCode() == 0)
                    return new ArrayList<>(Arrays.asList(gson.fromJson(gson.toJson(response.getData()), tClass)));
                else if (response.getStatusCode() == 2) {
                    logger.logError(response.getDeveloperMessage());
                    throw new CustomException(response.getDeveloperMessage());
                } else {
                    logger.logError(response.getDeveloperMessage() + ": " + response.getSystemMessage());
                    throw new CustomException(response.getDeveloperMessage() + ": " + response.getSystemMessage());
                }
            } else {
                String message = httpResponse.toString();
                logger.logError(message);
                throw new CustomException(message);
            }
        } catch (IOException e) {
            logger.logError(e.toString());
            throw new CustomException(e);
        }
    }

    public ResponseMessage executeUpdate(String urlString, Object requestData) throws CustomException {
        int statusCode;
        String title;
        String message;
        int iconId;

        try (Response httpResponse = sendRequest(new URL(urlString), "POST", requestData)) {
            if (httpResponse.code() == 200) {
                ResponseBody responseBody = httpResponse.body();
                CustomResponse response = gson.fromJson(Objects.requireNonNull(responseBody).string(), responseType);
                statusCode = response.getStatusCode();

                if (statusCode == 0) {
                    title = "Info";
                    message = response.getDeveloperMessage();
                    iconId = ic_dialog_info;
                } else if (statusCode == 2) {
                    title = "Xəta";
                    message = response.getDeveloperMessage();
                    iconId = ic_dialog_alert;
                } else {
                    title = "Xəta";
                    message = response.getDeveloperMessage() + ": " + response.getSystemMessage();
                    iconId = ic_dialog_alert;
                }
            } else {
                statusCode = httpResponse.code();
                title = "Xəta";
                message = httpResponse.toString();
                iconId = ic_dialog_alert;
            }
        } catch (IOException e) {
            logger.logError(e.toString());
            throw new CustomException(e);
        }

        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setStatusCode(statusCode);
        responseMessage.setTitle(title);
        responseMessage.setBody(message);
        responseMessage.setIconId(iconId);
        return responseMessage;
    }
}
