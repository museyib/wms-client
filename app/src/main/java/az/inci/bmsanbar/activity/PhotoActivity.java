package az.inci.bmsanbar.activity;

import static az.inci.bmsanbar.util.GlobalParameters.imageUrl;

import android.os.Bundle;
import android.webkit.WebView;

import az.inci.bmsanbar.R;

public class PhotoActivity extends AppBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_photo);
        setEdgeToEdge();
        WebView webView = findViewById(R.id.photo_view);
        webView.getSettings().setBuiltInZoomControls(true);

        String invCode = getIntent().getStringExtra("invCode");
        String imgUrl = imageUrl + "/" + invCode + ".jpg";
        String htmlCode = "<html><head><style>img {max-width: 100%}" +
                "</style></head><body><img src='" + imgUrl + "'/></body></html>";
        webView.loadData(htmlCode, "text/html", "UTF-8");
    }
}