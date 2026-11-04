package com.vn30.floatingpanel;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;

public class FloatingPanelService extends Service {
    public static final String EXTRA_URL = "page_url";
    private static final String CHANNEL = "vn30_floating_panel";
    private static final String PREFS = "vn30_panel_settings";
    private static final String KEY_URL = "page_url";
    private static final String KEY_X = "window_x";
    private static final String KEY_Y = "window_y";
    private static final String KEY_WIDTH = "window_width";
    private static final String KEY_HEIGHT = "window_height";
    private static final String DEFAULT_URL = "http://127.0.0.1:8000/index_mini.html";
    private static final int SAVE_DELAY_MS = 250;

    private final Handler saveHandler = new Handler(Looper.getMainLooper());
    private final Runnable saveWindowRunnable = this::saveWindowState;

    private WindowManager windowManager;
    private View overlay;
    private LinearLayout bar;
    private WebView webView;
    private View resizeHandle;
    private WindowManager.LayoutParams params;
    private int normalX, normalY, normalWidth, normalHeight;
    private boolean minimized = false;
    private boolean fullscreen = false;
    private Button minimizeButton, maximizeButton;
    private float downX, downY;
    private int startX, startY;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String url = intent != null ? intent.getStringExtra(EXTRA_URL) : null;
        if (url == null || url.trim().isEmpty()) {
            url = getSharedPreferences(PREFS, MODE_PRIVATE)
                    .getString(KEY_URL, DEFAULT_URL);
        }
        if (overlay == null) {
            createOverlay(url);
        } else if (webView != null && url != null) {
            webView.loadUrl(url);
        }
        return START_STICKY;
    }

    private void createOverlay(String url) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        int defaultWidth = dp(520);
        int defaultHeight = dp(410);
        int minWidth = Math.min(dp(320), screenWidth);
        int minHeight = Math.min(dp(230), screenHeight);
        int width = clamp(prefs.getInt(KEY_WIDTH, defaultWidth), minWidth, screenWidth);
        int height = clamp(prefs.getInt(KEY_HEIGHT, defaultHeight), minHeight, screenHeight);
        int x = clamp(prefs.getInt(KEY_X, dp(18)), 0, Math.max(0, screenWidth - width));
        int y = clamp(prefs.getInt(KEY_Y, dp(90)), 0, Math.max(0, screenHeight - height));

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackground(roundRect(Color.BLACK, Color.BLACK, dp(3)));
        shell.setClipToOutline(true);

        bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(2), 0, dp(2), 0);
        bar.setBackgroundColor(Color.BLACK);

        View dragArea = new View(this);
        bar.addView(dragArea, new LinearLayout.LayoutParams(0, dp(30), 1));

        minimizeButton = smallButton("－");
        maximizeButton = smallButton("□");
        Button reload = smallButton("↻");
        Button close = smallButton("×");
        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(dp(31), dp(27));
        buttonParams.setMargins(dp(1), 0, dp(1), 0);
        bar.addView(minimizeButton, new LinearLayout.LayoutParams(buttonParams));
        bar.addView(maximizeButton, new LinearLayout.LayoutParams(buttonParams));
        bar.addView(reload, new LinearLayout.LayoutParams(buttonParams));
        bar.addView(close, new LinearLayout.LayoutParams(buttonParams));
        shell.addView(bar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(30)));

        webView = new WebView(this);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setBackgroundColor(Color.TRANSPARENT);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        if (Build.VERSION.SDK_INT >= 29) {
            settings.setForceDark(WebSettings.FORCE_DARK_OFF);
        }
        if (Build.VERSION.SDK_INT >= 33) {
            settings.setAlgorithmicDarkeningAllowed(false);
        }
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        shell.addView(webView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        resizeHandle = new View(this);
        resizeHandle.setBackgroundColor(Color.BLACK);
        shell.addView(resizeHandle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(4)));

        overlay = shell;
        int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(width, height, overlayType,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        // Cho phép WebView nhận focus và mở bàn phím Android.
        params.softInputMode =
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = x;
        params.y = y;
        normalX = x;
        normalY = y;
        normalWidth = width;
        normalHeight = height;
        windowManager.addView(overlay, params);

        dragArea.setOnTouchListener(this::handleDrag);
        minimizeButton.setOnClickListener(v -> toggleMinimize());
        maximizeButton.setOnClickListener(v -> toggleFullscreen());
        reload.setOnClickListener(v -> {
            if (webView != null) webView.reload();
        });
        close.setOnClickListener(v -> stopSelf());
        resizeHandle.setOnTouchListener(this::handleResize);

        webView.loadUrl(url);
    }

    private int screenW() {
        return getResources().getDisplayMetrics().widthPixels;
    }

    private int screenH() {
        return getResources().getDisplayMetrics().heightPixels;
    }

    private void saveNormalGeometry() {
        normalX = params.x;
        normalY = params.y;
        normalWidth = params.width;
        normalHeight = params.height;
    }

    private void restoreNormalGeometry() {
        params.width = Math.min(normalWidth, screenW());
        params.height = Math.min(normalHeight, screenH());
        params.x = clamp(normalX, 0, Math.max(0, screenW() - params.width));
        params.y = clamp(normalY, 0, Math.max(0, screenH() - params.height));
    }

    private void toggleMinimize() {
        if (!minimized) {
            if (!fullscreen) saveNormalGeometry();

            params.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            webView.setVisibility(View.GONE);
            resizeHandle.setVisibility(View.GONE);
            params.width = dp(150);
            params.height = dp(30);
            minimized = true;
            minimizeButton.setText("□");
        } else {
            params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            minimized = false;
            minimizeButton.setText("－");
            webView.setVisibility(View.VISIBLE);

            if (fullscreen) {
                params.x = 0;
                params.y = 0;
                params.width = screenW();
                params.height = screenH();
                resizeHandle.setVisibility(View.GONE);
            } else {
                restoreNormalGeometry();
                resizeHandle.setVisibility(View.VISIBLE);
            }
        }

        windowManager.updateViewLayout(overlay, params);
        scheduleSaveWindowState();
    }

    private void toggleFullscreen() {
        if (!fullscreen) {
            if (minimized) {
                minimized = false;
                minimizeButton.setText("－");
                webView.setVisibility(View.VISIBLE);
                params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            } else {
                saveNormalGeometry();
            }

            fullscreen = true;
            params.x = 0;
            params.y = 0;
            params.width = screenW();
            params.height = screenH();
            webView.setVisibility(View.VISIBLE);
            resizeHandle.setVisibility(View.GONE);
            maximizeButton.setText("❐");
        } else {
            fullscreen = false;
            minimized = false;
            params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            restoreNormalGeometry();
            webView.setVisibility(View.VISIBLE);
            resizeHandle.setVisibility(View.VISIBLE);
            minimizeButton.setText("－");
            maximizeButton.setText("□");
        }

        windowManager.updateViewLayout(overlay, params);
        scheduleSaveWindowState();
    }

    private boolean handleDrag(View v, MotionEvent event) {
        if (fullscreen) return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getRawX();
                downY = event.getRawY();
                startX = params.x;
                startY = params.y;
                return true;
            case MotionEvent.ACTION_MOVE:
                params.x = startX + (int) (event.getRawX() - downX);
                params.y = startY + (int) (event.getRawY() - downY);
                normalX = params.x;
                normalY = params.y;
                windowManager.updateViewLayout(overlay, params);
                scheduleSaveWindowState();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                scheduleSaveWindowState();
                return true;
            default:
                return false;
        }
    }

    private boolean handleResize(View v, MotionEvent event) {
        if (minimized || fullscreen) return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getRawX();
                downY = event.getRawY();
                startX = params.width;
                startY = params.height;
                return true;
            case MotionEvent.ACTION_MOVE:
                params.width = Math.max(dp(320),
                        startX + (int) (event.getRawX() - downX));
                params.height = Math.max(dp(230),
                        startY + (int) (event.getRawY() - downY));
                normalWidth = params.width;
                normalHeight = params.height;
                windowManager.updateViewLayout(overlay, params);
                scheduleSaveWindowState();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                scheduleSaveWindowState();
                return true;
            default:
                return false;
        }
    }

    private void scheduleSaveWindowState() {
        saveHandler.removeCallbacks(saveWindowRunnable);
        saveHandler.postDelayed(saveWindowRunnable, SAVE_DELAY_MS);
    }

    private void saveWindowState() {
        if (params == null || overlay == null) return;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putInt(KEY_X, normalX)
                .putInt(KEY_Y, normalY)
                .putInt(KEY_WIDTH, normalWidth)
                .putInt(KEY_HEIGHT, normalHeight)
                .apply();
    }

    private int clamp(int value, int min, int max) {
        if (max < min) return max;
        return Math.max(min, Math.min(max, value));
    }

    private Button smallButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(16);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setPadding(0, 0, 0, 0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setBackgroundTintList(ColorStateList.valueOf(0xFF242424));
        return b;
    }

    private GradientDrawable roundRect(int color, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        d.setStroke(dp(1), stroke);
        return d;
    }

    private Notification buildNotification() {
        return new Notification.Builder(this, CHANNEL)
                .setContentTitle("VN30 Floating Panel")
                .setContentText("Cửa sổ nổi đang hoạt động")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL, "VN30 Floating Panel", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onDestroy() {
        saveHandler.removeCallbacks(saveWindowRunnable);
        // Lưu ngay cả khi người dùng đóng app khi cửa sổ đang thu nhỏ.
        saveWindowState();
        if (overlay != null && windowManager != null) {
            try { windowManager.removeView(overlay); } catch (Exception ignored) {}
        }
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
