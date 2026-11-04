package com.vn30.floatingpanel;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQUEST_OVERLAY = 701;
    private static final String PREFS = "vn30_panel_settings";
    private static final String KEY_URL = "page_url";
    private static final String DEFAULT_URL = "http://127.0.0.1:8000/index_mini.html";
    private EditText urlInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showScreen();
    }

    private void showScreen() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(24), dp(20), dp(20));
        page.setBackgroundColor(0xFF101010);

        TextView title = new TextView(this);
        title.setText("VN30 Floating Panel");
        title.setTextSize(23);
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(title);

        TextView info = new TextView(this);
        info.setText("Nhập địa chỉ trang web cần hiển thị trong cửa sổ nổi. "
                + "Đảm bảo server tương ứng đang hoạt động trước khi mở.");
        info.setTextSize(14);
        info.setTextColor(0xFFBBBBBB);
        info.setPadding(0, dp(14), 0, dp(12));
        page.addView(info);

        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setTextSize(14);
        urlInput.setTextColor(0xFFFFFFFF);
        urlInput.setHintTextColor(0xFF888888);
        urlInput.setHint("https://example.com");
        urlInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        urlInput.setText(prefs.getString(KEY_URL, DEFAULT_URL));
        urlInput.setSelectAllOnFocus(false);
        page.addView(urlInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        Button start = new Button(this);
        start.setText("MỞ CỬA SỔ NỔI");
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        startParams.topMargin = dp(18);
        page.addView(start, startParams);
        start.setOnClickListener(v -> startFloating());

        TextView note = new TextView(this);
        note.setText("Địa chỉ được lưu tự động. Có thể thay đổi bất cứ lúc nào.");
        note.setTextSize(12);
        note.setTextColor(0xFF888888);
        note.setPadding(0, dp(12), 0, 0);
        page.addView(note);

        setContentView(page);
    }

    private void startFloating() {
        String url = urlInput.getText().toString().trim();
        if (url.isEmpty()) {
            url = DEFAULT_URL;
        }
        if (!url.matches("(?i)^https?://.+")) {
            Toast.makeText(this, "Địa chỉ phải bắt đầu bằng http:// hoặc https://",
                    Toast.LENGTH_LONG).show();
            return;
        }

        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putString(KEY_URL, url).apply();

        if (!Settings.canDrawOverlays(this)) {
            Intent permission = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(permission, REQUEST_OVERLAY);
            return;
        }
        launchService(url);
    }

    private void launchService(String url) {
        Intent service = new Intent(this, FloatingPanelService.class);
        service.putExtra(FloatingPanelService.EXTRA_URL, url);
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
            String url = urlInput.getText().toString().trim();
            if (url.isEmpty()) url = DEFAULT_URL;
            launchService(url);
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
