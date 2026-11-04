package com.vn30.floatingpanel;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int REQUEST_OVERLAY = 701;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showScreen();
    }

    private void showScreen() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(28), dp(22), dp(22));
        page.setBackgroundColor(0xFFF5F7FA);

        TextView title = new TextView(this);
        title.setText("VN30 Floating Panel");
        title.setTextSize(24);
        title.setTextColor(0xFF172033);
        title.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(title);

        TextView info = new TextView(this);
        info.setText("Cửa sổ nổi hiển thị index_mini.html bằng Light Mode.\n\nTrước khi mở, hãy đảm bảo Termux đang chạy web server tại cổng 8000 và các server dữ liệu VN30.");
        info.setTextSize(16);
        info.setTextColor(0xFF39465C);
        info.setPadding(0, dp(18), 0, dp(22));
        page.addView(info);

        Button start = new Button(this);
        start.setText("MỞ CỬA SỔ NỔI");
        page.addView(start);
        start.setOnClickListener(v -> startFloating());

        TextView address = new TextView(this);
        address.setText("Trang: http://127.0.0.1:8000/index_mini.html");
        address.setTextSize(13);
        address.setTextColor(0xFF64748B);
        address.setPadding(0, dp(18), 0, 0);
        page.addView(address);

        setContentView(page);
    }

    private void startFloating() {
        if (!Settings.canDrawOverlays(this)) {
            Intent permission = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(permission, REQUEST_OVERLAY);
            return;
        }
        Intent service = new Intent(this, FloatingPanelService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(service);
        } else {
            startService(service);
        }
        moveTaskToBack(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY && Settings.canDrawOverlays(this)) {
            startFloating();
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
