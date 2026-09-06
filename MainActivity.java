package com.ahwazgolden.calculator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.view.View;
import android.widget.ProgressBar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private WebView webView;
    private ProgressBar progressBar;
    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    // نگاشت دامنه‌ی هر پیام‌رسان به پکیج اپ آن؛ برای افزودن اپ جدید فقط یک خط اضافه کنید
    private static final Map<String, String> MESSENGER_PACKAGES = new HashMap<String, String>() {{
        put("ble.ir", "ir.nasim");          // بله
        put("rubika.ir", "ir.resaneh1.iptv"); // روبیکا
    }};

    @SuppressLint({"SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // آدرس‌های عادی سایت داخل همین وب‌ویو باز شوند، مگر اینکه لینک یک پیام‌رسان شناخته‌شده باشد
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    if (tryOpenMessengerApp(url)) {
                        return true;
                    }
                    view.loadUrl(url);
                    return true;
                }
                if (url.startsWith("file://")) {
                    view.loadUrl(url);
                    return true;
                }

                // سایر آدرس‌ها (مثل intent:// برای باز کردن اپ بله) با Intent جدا هندل شوند
                try {
                    Intent intent;
                    if (url.startsWith("intent://")) {
                        intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    } else {
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    }

                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        // اپ مقصد (مثلاً بله) نصب نیست؛ اگر لینک fallback داشت، همان را باز کن
                        String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                        if (fallbackUrl != null) {
                            view.loadUrl(fallbackUrl);
                        }
                    }
                } catch (java.net.URISyntaxException | ActivityNotFoundException e) {
                    // لینک نامعتبر یا اپی برای بازکردنش نبود؛ نادیده گرفته می‌شود
                }
                return true;
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        webView.loadUrl("file:///calculator.html");

        // درخواست اجازه نوتیفیکیشن
        requestNotificationPermission();
    }

    /**
     * اگر لینک متعلق به یکی از پیام‌رسان‌های شناخته‌شده (بله، روبیکا و...) باشد
     * و اپ آن روی گوشی نصب باشد، مستقیم همان اپ باز می‌شود.
     * @return true اگر اپ باز شد (دیگر نیازی به لود شدن در وب‌ویو نیست)
     */
    private boolean tryOpenMessengerApp(String url) {
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host == null) return false;

            for (Map.Entry<String, String> entry : MESSENGER_PACKAGES.entrySet()) {
                if (host.equals(entry.getKey()) || host.endsWith("." + entry.getKey())) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.setPackage(entry.getValue());
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                        return true;
                    }
                    return false; // اپ نصب نیست؛ بگذار در وب‌ویو باز شود
                }
            }
        } catch (Exception e) {
            // نادیده گرفته می‌شود، لینک به‌صورت عادی در وب‌ویو باز خواهد شد
        }
        return false;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // نمایش توضیح قبل از درخواست
                new AlertDialog.Builder(this)
                    .setTitle("فعال‌سازی اعلان‌ها")
                    .setMessage("برای دریافت آخرین قیمت‌های طلا و اطلاعیه‌های مهم، لطفاً اجازه نمایش اعلان را بدهید.")
                    .setPositiveButton("فعال کردن", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                ActivityCompat.requestPermissions(
                                    MainActivity.this,
                                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                                    NOTIFICATION_PERMISSION_CODE
                                );
                            }
                        }
                    })
                    .setNegativeButton("بعداً", null)
                    .setCancelable(true)
                    .show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
