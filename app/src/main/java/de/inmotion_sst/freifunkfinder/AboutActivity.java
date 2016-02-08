package de.inmotion_sst.freifunkfinder;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.gms.common.GoogleApiAvailability;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        WebView webView = (WebView) findViewById(R.id.about_webview);
        // allow zoom
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        // start intent for http/https links to open them e.g. in browser
        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && url.startsWith("http")) {
                    view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                } else {
                    return false;
                }
            }
        });

        AsyncTask<Void, Void, String> loadText = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {

                StringBuilder sb = new StringBuilder();

                try {
                    sb.append("<html>");
                    sb.append("<body>");
                    String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                    sb.append(String.format("Freifunk Community App, v%s\n\n", versionName));

                    String about = loadAboutText();
                    sb.append(about);

                    String googleLicenses = GoogleApiAvailability.getInstance().getOpenSourceSoftwareLicenseInfo(AboutActivity.this);
                    sb.append("<pre><code>");
                    sb.append(googleLicenses);
                    sb.append("</code></pre>");

                    sb.append("</body>");
                    sb.append("</html>");

                    // webview is a bit weird when it comes to string loading...
                    return URLEncoder.encode(sb.toString(), "UTF-8").replace("+", "%20");
                } catch (Exception e) {
                    return e.toString();
                }
            }

            @Override
            protected void onPostExecute(String s) {
                webView.loadData(s, "text/html", "utf-8");
            }
        };
        loadText.execute();
    }

    @NonNull
    private String loadAboutText() throws IOException {
        Resources res = getResources();
        InputStream inputStream = res.openRawResource(R.raw.about_html);
        byte[] b = new byte[inputStream.available()];
        inputStream.read(b);
        return new String(b);
    }
}
