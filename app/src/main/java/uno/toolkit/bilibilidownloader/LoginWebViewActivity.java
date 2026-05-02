package uno.toolkit.bilibilidownloader;

import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginWebViewActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                CookieManager cookieManager = CookieManager.getInstance();
                String cookieStr = cookieManager.getCookie("https://bilibili.com");

                if (cookieStr != null && cookieStr.contains("SESSDATA")) {
                    Utils.saveCookie(LoginWebViewActivity.this, cookieStr);
                    Toast.makeText(LoginWebViewActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });

        webView.loadUrl("https://passport.bilibili.com/login");
    }
}