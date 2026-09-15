package app.capit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.*;
import android.widget.*;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

/** Instagram-only browser. No native JavaScript bridge or credential collection. */
public class MainActivity extends Activity {
    private static final String HOME = "https://www.instagram.com/";
    private WebView web;
    private TextView status;
    private ProgressBar progress;
    private String filter;
    private ValueCallback<Uri[]> upload;
    private final int green = Color.rgb(33, 107, 82);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245, 244, 238));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(root);
        TextView brand = new TextView(this);
        brand.setText("capit  /  Instagram"); brand.setTextSize(24); brand.setTextColor(green);
        brand.setPadding(20, 14, 20, 6); root.addView(brand);
        status = new TextView(this);
        status.setText("Prototype · Reels & Explore filters enabled");
        status.setTextSize(12); status.setPadding(20, 0, 20, 8); root.addView(status);
        LinearLayout nav = new LinearLayout(this);
        root.addView(nav);
        button(nav, "Stories", () -> navigate(HOME));
        button(nav, "Messages", () -> navigate(HOME + "direct/inbox/"));
        button(nav, "Following", this::following);
        button(nav, "More", this::menu);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        root.addView(progress, new LinearLayout.LayoutParams(-1, 5));
        web = new WebView(this);
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        try {
            try (InputStream input = getAssets().open("instagram-filter.js");
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                filter = output.toString("UTF-8");
            }
        } catch (Exception e) {
            status.setText("Could not load filters. Please reinstall Capit."); return;
        }
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            status.setText("Update Android System WebView or Chrome, then reopen Capit.");
            return; // Do not show an unfiltered Instagram page on unsupported engines.
        }
        WebViewCompat.addDocumentStartJavaScript(web, filter,
                new HashSet<>(Arrays.asList("https://www.instagram.com", "https://instagram.com", "https://*.instagram.com")));
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return !instagram(request.getUrl());
                Uri uri = request.getUrl();
                if (blocked(uri)) { status.setText("Reels and Explore are hidden."); return true; }
                if (instagram(uri)) return false;
                external(uri); return true;
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request.isForMainFrame() && (!instagram(request.getUrl()) || blocked(request.getUrl()))) {
                    String page = "<html><body><h2>This page is unavailable in Capit.</h2><p>Use the buttons above to continue.</p></body></html>";
                    return new WebResourceResponse("text/html", "UTF-8",
                            new ByteArrayInputStream(page.getBytes(StandardCharsets.UTF_8)));
                }
                return null;
            }
            @Override public void onPageFinished(WebView view, String url) {
                CookieManager.getInstance().flush();
                if (instagram(Uri.parse(url))) view.evaluateJavascript(filter, null);
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) status.setText("Could not load Instagram. Check your connection, then use More → Reload.");
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                if (request.isForMainFrame()) status.setText("Instagram returned an error. Try More → Reload.");
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int value) {
                progress.setProgress(value); progress.setVisibility(value == 100 ? View.GONE : View.VISIBLE);
            }
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (upload != null) upload.onReceiveValue(null);
                upload = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
                try { startActivityForResult(intent, 10); }
                catch (Exception e) { upload.onReceiveValue(null); upload = null; }
                return true;
            }
            @Override public void onPermissionRequest(PermissionRequest request) {
                request.deny(); // Camera, microphone and calls are outside this prototype.
                runOnUiThread(() -> status.setText("Camera, microphone and calls are not supported in this prototype."));
            }
        });
        if (getPreferences(0).getBoolean("onboarded", false)) navigate(HOME + "direct/inbox/");
        else new AlertDialog.Builder(this).setTitle("Welcome to Capit")
                .setMessage("Use Instagram with fewer distractions. Sign in on Instagram’s own website. Capit does not collect passwords or send your messages to a separate server.\n\nThis is an early prototype. Stories, messages and uploads need testing. Following is experimental; calls and notifications are not supported.\n\nFilters only apply inside Capit.")
                .setCancelable(false).setPositiveButton("Open Instagram", (d, w) -> {
                    getPreferences(0).edit().putBoolean("onboarded", true).apply();
                    navigate(HOME + "direct/inbox/");
                }).show();
    }
    private void button(LinearLayout nav, String title, Runnable action) {
        Button b = new Button(this); b.setText(title); b.setTextSize(11); b.setAllCaps(false);
        b.setMinWidth(0); b.setMinimumWidth(0); b.setPadding(0, 0, 0, 0);
        nav.addView(b, new LinearLayout.LayoutParams(0, -2, 1)); b.setOnClickListener(v -> action.run());
    }
    private static boolean instagram(Uri uri) {
        String host = uri.getHost();
        return "https".equalsIgnoreCase(uri.getScheme()) && host != null &&
                (host.equalsIgnoreCase("instagram.com") || host.toLowerCase(java.util.Locale.ROOT).endsWith(".instagram.com"));
    }
    private static boolean blocked(Uri uri) {
        if (!instagram(uri)) return false;
        String path = uri.getPath();
        return path != null && path.toLowerCase(java.util.Locale.ROOT).matches("^/(reel|reels|explore)(/.*)?$|^/[^/]+/reels(/.*)?$");
    }
    private void navigate(String url) {
        status.setText(url.contains("variant=following") ? "Following · experimental, verify posts on your account"
                : url.endsWith("instagram.com/") ? "Stories · home-feed posts hidden" : "Messages · distraction filters enabled");
        web.loadUrl(url);
    }
    private void following() {
        new AlertDialog.Builder(this).setTitle("Try your Following feed")
                .setMessage("Instagram may not provide this feed on the website for every account. Check that the posts are from people you follow. If it shows suggestions, return to Messages and report it. Capit cannot yet verify the authors against your following list.")
                .setPositiveButton("Try Following", (d, w) -> navigate(HOME + "?variant=following"))
                .setNegativeButton("Cancel", null).show();
    }
    private void menu() {
        new AlertDialog.Builder(this).setTitle("Capit prototype").setItems(new String[]{"Reload", "Open a profile", "About & limitations", "Clear Instagram session"}, (d, item) -> {
            if (item == 0) web.reload();
            if (item == 1) {
                EditText name = new EditText(this); name.setHint("Instagram username"); name.setSingleLine(true);
                new AlertDialog.Builder(this).setTitle("Open a profile").setView(name)
                        .setPositiveButton("Open", (dialog, which) -> {
                            String value = name.getText().toString().trim().replaceFirst("^@", "");
                            if (value.matches("[A-Za-z0-9._]{1,30}")) navigate(HOME + value + "/");
                            else Toast.makeText(this, "Enter a valid username", Toast.LENGTH_SHORT).show();
                        }).setNegativeButton("Cancel", null).show();
            }
            if (item == 2) new AlertDialog.Builder(this).setTitle("Less scrolling. More connection.")
                    .setMessage("Capit 0.1 · Free prototype\n\nReels and Explore routes are blocked; matching links and cards are hidden. The standard home feed is hidden to leave room for Stories. Following is experimental.\n\nInstagram can change its pages and break filters. Calls, camera capture, push notifications and Facebook sign-in are not supported. No analytics, advertising or Capit backend. Instagram still processes data under its own policies.")
                    .setPositiveButton("Done", null).show();
            if (item == 3) new AlertDialog.Builder(this).setTitle("Clear your session?")
                    .setMessage("This signs you out of Instagram inside Capit and clears its local website storage.")
                    .setPositiveButton("Clear", (dialog, which) -> {
                        web.stopLoading(); web.loadUrl("about:blank");
                        CookieManager.getInstance().removeAllCookies(done -> {
                            CookieManager.getInstance().flush(); WebStorage.getInstance().deleteAllData();
                            web.clearCache(true); web.clearHistory(); navigate(HOME + "accounts/login/");
                        });
                    }).setNegativeButton("Cancel", null).show();
        }).show();
    }
    private void external(Uri uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme())) { status.setText("This link type is not supported in Capit."); return; }
        new AlertDialog.Builder(this).setTitle("Leave Capit?")
                .setMessage("Open " + uri.getHost() + " in your browser? Capit’s filters will not apply there.")
                .setPositiveButton("Open browser", (d, w) -> {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                    catch (Exception e) { status.setText("No browser is available for this link."); }
                }).setNegativeButton("Stay here", null).show();
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != 10 || upload == null) return;
        Uri[] files = null;
        if (result == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                files = new Uri[data.getClipData().getItemCount()];
                for (int i = 0; i < files.length; i++) files[i] = data.getClipData().getItemAt(i).getUri();
            } else if (data.getData() != null) files = new Uri[]{data.getData()};
        }
        upload.onReceiveValue(files); upload = null;
    }
    @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
    @Override protected void onPause() { if (web != null) web.onPause(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }
    @Override protected void onDestroy() {
        if (upload != null) { upload.onReceiveValue(null); upload = null; }
        if (web != null) { web.stopLoading(); web.destroy(); }
        super.onDestroy();
    }
}
