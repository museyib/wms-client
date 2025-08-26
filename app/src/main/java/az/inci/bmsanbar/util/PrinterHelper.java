package az.inci.bmsanbar.util;

import android.content.Context;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import az.inci.bmsanbar.R;

public class PrinterHelper {
    private final Context context;

    public PrinterHelper(Context context) {
        this.context = context;
    }

    public void print(String html) {
        WebView webView = new WebView(context);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                createWebPrintJob(view);
            }
        });

        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null);
    }

    private void createWebPrintJob(WebView webView) {
        PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);

        String jobName = context.getString(R.string.app_name) + " Document";

        PrintDocumentAdapter printAdapter;
        PrintAttributes.Builder builder = new PrintAttributes.Builder().setMediaSize(
                PrintAttributes.MediaSize.ISO_A4);

        printAdapter = webView.createPrintDocumentAdapter(jobName);

        builder.setDuplexMode(PrintAttributes.DUPLEX_MODE_LONG_EDGE);
        printManager.print(jobName, printAdapter, builder.build());
    }
}
