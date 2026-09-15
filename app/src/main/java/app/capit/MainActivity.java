package app.capit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.view.WindowInsetsController;
import android.view.View;
import android.view.Gravity;
import android.content.res.ColorStateList;
import android.view.inputmethod.InputMethodManager;
import static app.capit.CapitUi.*;
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
    private LinearLayout browser;
    private View dashboard;
    private FrameLayout content;
    private TextView section;
    private final LinearLayout[] tabs = new LinearLayout[4];
    private final CapitUi.Icon[] tabIcons = new CapitUi.Icon[4];
    private final TextView[] tabLabels = new TextView[4];
    private boolean filtersReady;
    private boolean atHome = true;
    private String filterError = "Filters are still starting. Try again in a moment.";
    private int dp(float value) { return CapitUi.dp(this, value); }
    private boolean reducedMotion() { return getPreferences(0).getBoolean("reduceMotion", false); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = column(this);
        root.setBackgroundColor(PAPER);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(root);
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(10), dp(16), dp(10));
        ImageView logo = new ImageView(this); logo.setImageResource(R.drawable.ic_capit);
        logo.setPadding(dp(4),dp(4),dp(4),dp(4));logo.setContentDescription("Capit home");
        clickable(logo,Color.TRANSPARENT,16,this::showHome);header.addView(logo,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout brand = column(this);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1);bp.setMarginStart(dp(8));header.addView(brand,bp);
        TextView wordmark=text(this,"capit",26,INK,true);wordmark.setLetterSpacing(-.06f);brand.addView(wordmark);
        section=text(this,"A calmer Instagram",11,MUTED,false);brand.addView(section);
        CapitUi.Icon more=new CapitUi.Icon(this,"more",INK);more.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        more.setPadding(dp(12),dp(12),dp(12),dp(12));more.setContentDescription("More options");
        clickable(more,Color.WHITE,16,this::menu);header.addView(more,new LinearLayout.LayoutParams(dp(48),dp(48)));
        root.addView(header);
        content=new FrameLayout(this);root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        browser=column(this);browser.setBackgroundColor(Color.WHITE);
        status=text(this,"Distraction filters enabled",11,MUTED,false);status.setPadding(dp(20),dp(8),dp(20),dp(8));status.setBackgroundColor(PAPER);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);browser.addView(status);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setProgressTintList(ColorStateList.valueOf(INK));progress.setProgressBackgroundTintList(ColorStateList.valueOf(MINT));
        progress.setVisibility(View.INVISIBLE);browser.addView(progress,new LinearLayout.LayoutParams(-1,dp(2)));
        web=new WebView(this);browser.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        content.addView(browser,new FrameLayout.LayoutParams(-1,-1));
        dashboard=CapitUi.home(this,()->navigate(HOME+"direct/inbox/"),()->navigate(HOME),this::following,this::about);
        content.addView(dashboard,new FrameLayout.LayoutParams(-1,-1));
        View divider=new View(this);divider.setBackgroundColor(LINE);root.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
        LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(12),dp(6),dp(12),dp(6));root.addView(nav);
        addTab(nav,0,"Home","home",this::showHome);
        addTab(nav,1,"Messages","messages",()->navigate(HOME+"direct/inbox/"));
        addTab(nav,2,"Stories","stories",()->navigate(HOME));
        addTab(nav,3,"Following","following",this::following);
        showHome();
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
            filterError="Could not load filters. Please reinstall Capit."; return;
        }
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            filterError="Update Android System WebView or Chrome, then reopen Capit.";
            return; // Do not show an unfiltered Instagram page on unsupported engines.
        }
        WebViewCompat.addDocumentStartJavaScript(web, filter,
                new HashSet<>(Arrays.asList("https://www.instagram.com", "https://instagram.com", "https://*.instagram.com")));
        filtersReady = true;
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
                progress.setProgress(value, !reducedMotion()); progress.setVisibility(value == 100 ? View.INVISIBLE : View.VISIBLE);
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
    }
    private void addTab(LinearLayout nav,int index,String title,String icon,Runnable action) {
        LinearLayout tab=column(this);tab.setGravity(Gravity.CENTER);tab.setPadding(dp(2),dp(8),dp(2),dp(8));tab.setMinimumHeight(dp(62));
        tabs[index]=tab;tabIcons[index]=new CapitUi.Icon(this,icon,MUTED);
        tab.addView(tabIcons[index],new LinearLayout.LayoutParams(dp(23),dp(23)));space(tab,5);
        tabLabels[index]=text(this,title,10,MUTED,true);tabLabels[index].setGravity(Gravity.CENTER);tab.addView(tabLabels[index]);
        tab.setContentDescription(title);clickable(tab,Color.TRANSPARENT,18,action);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(2),0,dp(2),0);nav.addView(tab,lp);
    }
    private void selectTab(int index) {
        for(int i=0;i<tabs.length;i++) {
            if(tabs[i]==null) continue;
            tabs[i].setSelected(i==index);
            // Update the content layer directly: RippleDrawable's generated layer has no stable ID.
            android.graphics.drawable.RippleDrawable bg=(android.graphics.drawable.RippleDrawable)tabs[i].getBackground();
            if(bg.getNumberOfLayers()>0 && bg.getDrawable(0) instanceof android.graphics.drawable.GradientDrawable)
                ((android.graphics.drawable.GradientDrawable)bg.getDrawable(0)).setColor(i==index?MINT:Color.TRANSPARENT);
            tabIcons[i].tint(i==index?INK:MUTED);tabLabels[i].setTextColor(i==index?INK:MUTED);
        }
    }
    private void showHome() {
        if (dashboard==null) return;
        if(web!=null) {
            if(filtersReady && instagram(Uri.parse(web.getUrl()==null?HOME:web.getUrl())))
                web.evaluateJavascript("document.querySelectorAll('video,audio').forEach(m=>m.pause())",null);
            web.onPause();
        }
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(content.getWindowToken(),0);
        atHome=true;browser.setVisibility(View.GONE);dashboard.setVisibility(View.VISIBLE);section.setText("A calmer Instagram");selectTab(0);
        CapitUi.enter(dashboard,reducedMotion());
    }
    private void showBrowser(String url) {
        atHome=false;dashboard.animate().cancel();dashboard.setVisibility(View.GONE);browser.setVisibility(View.VISIBLE);web.onResume();
        int index=url.contains("variant=following")?3:url.contains("/direct/")?1:url.equals(HOME)?2:-1;
        selectTab(index);section.setText(index==3?"Following":index==1?"Messages":index==2?"Stories":"Instagram");
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
        if(!filtersReady) {new AlertDialog.Builder(this).setTitle("A quick update needed").setMessage(filterError).setPositiveButton("OK",null).show();return;}
        if(!getPreferences(0).getBoolean("onboarded",false)) {
            new AlertDialog.Builder(this).setTitle("Your Instagram, a little quieter")
                .setMessage("Sign in on Instagram’s own website. Capit keeps your session on this phone and has no separate server for your messages or password. Filters apply only inside Capit.\n\nFollowing is experimental. Calls and notifications are not supported yet.")
                .setPositiveButton("Continue",(d,w)->{getPreferences(0).edit().putBoolean("onboarded",true).apply();navigate(url);})
                .setNegativeButton("Not now",null).show();return;
        }
        showBrowser(url);
        status.setText(url.contains("variant=following") ? "Following · experimental"
                : url.equals(HOME) ? "Stories · home-feed posts hidden" : "Reels & Explore filters enabled");
        web.loadUrl(url);
    }
    private void following() {
        new AlertDialog.Builder(this).setTitle("Try your Following feed")
                .setMessage("Instagram may not provide this feed on the website for every account. Check that the posts are from people you follow. If it shows suggestions, return to Messages and report it. Capit cannot yet verify the authors against your following list.")
                .setPositiveButton("Try Following", (d, w) -> navigate(HOME + "?variant=following"))
                .setNegativeButton("Cancel", null).show();
    }
    private void menu() {
        new AlertDialog.Builder(this).setTitle("Make yourself at home").setItems(new String[]{"Reload Instagram", "Open a profile", "About Capit", "Clear Instagram session", reducedMotion()?"Reduce motion: on":"Reduce motion: off"}, (d, item) -> {
            if (item == 0) { if(atHome) navigate(HOME+"direct/inbox/"); else web.reload(); }
            if (item == 4) { getPreferences(0).edit().putBoolean("reduceMotion",!reducedMotion()).apply();if(atHome) {dashboard.animate().cancel();dashboard.setAlpha(1);dashboard.setTranslationY(0);} Toast.makeText(this,reducedMotion()?"Reduced motion enabled":"Subtle animations enabled",Toast.LENGTH_SHORT).show(); }
            if (item == 1) {
                EditText name = new EditText(this); name.setHint("Instagram username"); name.setSingleLine(true);
                new AlertDialog.Builder(this).setTitle("Open a profile").setView(name)
                        .setPositiveButton("Open", (dialog, which) -> {
                            String value = name.getText().toString().trim().replaceFirst("^@", "");
                            if (value.matches("[A-Za-z0-9._]{1,30}")) navigate(HOME + value + "/");
                            else Toast.makeText(this, "Enter a valid username", Toast.LENGTH_SHORT).show();
                        }).setNegativeButton("Cancel", null).show();
            }
            if (item == 2) about();
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
    private void about() {
        new AlertDialog.Builder(this).setTitle("Less scrolling. More connection.")
            .setMessage("Capit 0.2 · Free to use\n\nA calmer way to use Instagram. Reels and Explore filters run locally inside Capit. Following is experimental, and Instagram updates may affect filtering.\n\nNo ads, analytics or Capit server. Instagram still processes your account activity. Calls, notifications, camera capture and Facebook sign-in are not supported yet.")
            .setPositiveButton("Sounds good",null).show();
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
    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) return;
        // Explicitly restore dark system icons after the launch screen/WebView initialization.
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) controller.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        } else {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(decor.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
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
    @Override public void onBackPressed() { if(atHome) super.onBackPressed(); else if(web.canGoBack()) web.goBack(); else showHome(); }
    @Override protected void onPause() { if (web != null) web.onPause(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); if (web != null && !atHome) web.onResume(); }
    @Override protected void onDestroy() {
        if (upload != null) { upload.onReceiveValue(null); upload = null; }
        if(dashboard!=null) dashboard.animate().cancel();
        if (web != null) { web.stopLoading(); web.destroy(); }
        super.onDestroy();
    }
}
