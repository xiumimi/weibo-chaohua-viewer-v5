package com.local.weibocalendar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String UID = "7305739004";
    private static final String LOGIN_URL = "https://weibo.com/login.php";
    private static final String HOME_URL = "https://www.weibo.com/u/" + UID;
    private static final String SEARCH_URL =
            "https://www.weibo.com/u/" + UID + "?profile_ftype=1&is_all=1&is_search=1&key_word={query}";

    private Calendar selected = Calendar.getInstance(Locale.CHINA);
    private WebView webView;
    private LinearLayout calendarPanel;
    private TextView statusText;
    private Button toggleCalendarButton;
    private boolean calendarVisible = true;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(247, 245, 239));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setPadding(dp(10), dp(8), dp(10), dp(6));
        toolbar.setBackgroundColor(Color.rgb(247, 245, 239));
        root.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        statusText = new TextView(this);
        statusText.setText("选择日期后搜索：" + formatDotDate());
        statusText.setTextColor(Color.rgb(32, 33, 36));
        statusText.setTextSize(15);
        statusText.setGravity(android.view.Gravity.CENTER_VERTICAL);
        toolbar.addView(statusText, new LinearLayout.LayoutParams(
                0,
                dp(42),
                1));

        toggleCalendarButton = new Button(this);
        toggleCalendarButton.setText("收起日历");
        toggleCalendarButton.setAllCaps(false);
        toggleCalendarButton.setTextSize(13);
        toggleCalendarButton.setOnClickListener(v -> setCalendarVisible(!calendarVisible));
        toolbar.addView(toggleCalendarButton, new LinearLayout.LayoutParams(
                dp(104),
                dp(42)));

        calendarPanel = new LinearLayout(this);
        calendarPanel.setOrientation(LinearLayout.VERTICAL);
        calendarPanel.setBackgroundColor(Color.rgb(247, 245, 239));
        root.addView(calendarPanel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        CalendarView calendarView = new CalendarView(this);
        calendarView.setDate(selected.getTimeInMillis(), false, true);
        calendarView.setBackgroundColor(Color.rgb(247, 245, 239));
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selected.set(year, month, dayOfMonth, 0, 0, 0);
            statusText.setText("正在搜索：" + formatDotDate());
            openSelectedDate();
        });
        calendarPanel.addView(calendarView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(310)));

        webView = new WebView(this);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setUserAgentString(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                return false;
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(view, request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(view, Uri.parse(url));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                simplifyWeiboPage();
            }
        });

        webView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY > dp(24) && calendarVisible) {
                setCalendarVisible(false);
            }
        });

        root.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1));

        setContentView(root);
        webView.loadUrl(LOGIN_URL);
    }

    private boolean handleUrl(WebView view, Uri uri) {
        String scheme = uri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            String url = uri.toString();
            if (url.contains("sinaweibo://")
                    || url.contains("weibo://")
                    || url.contains("download")
                    || url.contains("scheme=sinaweibo")) {
                return true;
            }
            view.loadUrl(uri.toString());
            return true;
        }
        return true;
    }

    private void openSelectedDate() {
        String query = formatDotDate();
        String url = SEARCH_URL.replace("{query}", encode(query));
        setCalendarVisible(false);
        webView.loadUrl(url);
    }

    private void setCalendarVisible(boolean visible) {
        calendarVisible = visible;
        calendarPanel.setVisibility(visible ? android.view.View.VISIBLE : android.view.View.GONE);
        toggleCalendarButton.setText(visible ? "收起日历" : "展开日历");
    }

    private String formatDotDate() {
        return new SimpleDateFormat("yyyy.M.d", Locale.CHINA).format(selected.getTime());
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }

    private void simplifyWeiboPage() {
        String js = "(function(){"
                + "var css='header,nav,aside,[class*=Side],[class*=sidebar],[class*=woo-box-flex][role=navigation]{display:none!important;}'"
                + "+'body{background:#fff!important;}';"
                + "var style=document.getElementById('chaohua-clean-style');"
                + "if(!style){style=document.createElement('style');style.id='chaohua-clean-style';document.head.appendChild(style);}"
                + "style.textContent=css;"
                + "var mark=document.getElementById('chaohua-date-marker');if(mark)mark.remove();"
                + "})();";
        webView.evaluateJavascript(js, null);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
