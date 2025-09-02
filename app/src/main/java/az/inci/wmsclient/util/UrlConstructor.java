package az.inci.wmsclient.util;

import static az.inci.wmsclient.util.GlobalParameters.apiVersion;
import static az.inci.wmsclient.util.GlobalParameters.serviceUrl;

import java.util.Map;

import okhttp3.HttpUrl;

public class UrlConstructor {
    public static String createUrl(String... value) {
        StringBuilder sb = new StringBuilder();
        sb.append(serviceUrl).append("/").append(apiVersion);
        for (String s : value) {
            sb.append("/").append(s);
        }
        return sb.toString();
    }

    public static String addQueryParameters(String url, Map<String, String> requestParameters) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null)
            return url;
        HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
        for (Map.Entry<String, String> entry : requestParameters.entrySet()) {
            urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
        }
        return urlBuilder.build().toString();
    }
}
