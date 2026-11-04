package com.vn30.floatingpanel;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
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
import android.widget.TextView;

public class FloatingPanelService extends Service {
    private static final String CHANNEL = "vn30_floating_panel";
    private WindowManager windowManager;
    private View overlay;
    private WindowManager.LayoutParams params;
    private WebView webView;
    private float downX, downY;
    private int startX, startY;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createOverlay();
    }

    private void createOverlay() {
        int width = dp(520);
        int height = dp(410);

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackground(roundRect(0xFFF8FAFC, 0xFF94A3B8, dp(10)));
        shell.setClipToOutline(true);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(9), 0, dp(5), 0);
        bar.setBackgroundColor(0xFFE8EEF5);
        TextView title = new TextView(this);
        title.setText("VN30  •  FLOATING");
        title.setTextColor(0xFF172033);
        title.setTextSize(13);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0, dp(42), 1));

        Button reload = smallButton("↻");
        Button close = smallButton("×");
        bar.addView(reload, new LinearLayout.LayoutParams(dp(40), dp(36)));
        bar.addView(close, new LinearLayout.LayoutParams(dp(40), dp(36)));
        shell.addView(bar);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
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

        TextView resize = new TextView(this);
        resize.setText("◢");
        resize.setTextSize(17);
        resize.setTextColor(0xFF475569);
        resize.setGravity(Gravity.CENTER);
        resize.setBackgroundColor(0xFFE8EEF5);
        shell.addView(resize, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(22)));

        overlay = shell;
        int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(width, height, overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(18);
        params.y = dp(90);
        windowManager.addView(overlay, params);

        bar.setOnTouchListener((v, event) -> {
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
                    windowManager.updateViewLayout(overlay, params);
                    return true;
            }
            return false;
        });

        reload.setOnClickListener(v -> webView.reload());
        close.setOnClickListener(v -> stopSelf());

        resize.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getRawX();
                    downY = event.getRawY();
                    startX = params.width;
                    startY = params.height;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    params.width = Math.max(dp(320), startX + (int)(event.getRawX() - downX));
                    params.height = Math.max(dp(230), startY + (int)(event.getRawY() - downY));
                    windowManager.updateViewLayout(overlay, params);
                    return true;
            }
            return false;
        });

        webView.loadUrl("http://127.0.0.1:8000/index_mini.html");
    }

    private Button smallButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(18);
        b.setTextColor(0xFF172033);
        b.setPadding(0, 0, 0, 0);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFDCE5EF));
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
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
