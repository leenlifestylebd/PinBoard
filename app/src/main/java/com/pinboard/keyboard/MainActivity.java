package com.pinboard.keyboard;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView step1Status, step2Status;
    private Button btn1, btn2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            step1Status = findViewById(R.id.step1_status);
            step2Status = findViewById(R.id.step2_status);
            btn1 = findViewById(R.id.btn_enable);
            btn2 = findViewById(R.id.btn_select);

            if (btn1 != null) btn1.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
                } catch (Exception e) {
                    Toast.makeText(this, "Open Settings manually", Toast.LENGTH_SHORT).show();
                }
            });

            if (btn2 != null) btn2.setOnClickListener(v -> {
                try {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.showInputMethodPicker();
                } catch (Exception e) {
                    Toast.makeText(this, "Open Settings manually", Toast.LENGTH_SHORT).show();
                }
            });

            Button btnManage = findViewById(R.id.btn_manage_pins);
            if (btnManage != null) btnManage.setOnClickListener(v -> {
                startActivity(new Intent(this, PinboardActivity.class));
            });

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try { updateStatus(); } catch (Exception ignored) {}
    }

    private void updateStatus() {
        boolean enabled   = isKeyboardEnabled();
        boolean isDefault = isKeyboardDefault();

        if (step1Status != null) {
            step1Status.setText(enabled ? "✓ Enabled" : "Not enabled yet");
            step1Status.setTextColor(enabled ? 0xFFE94560 : 0xFF888888);
        }
        if (step2Status != null) {
            step2Status.setText(isDefault ? "✓ Selected" : "Not selected yet");
            step2Status.setTextColor(isDefault ? 0xFFE94560 : 0xFF888888);
        }
        if (btn1 != null) btn1.setAlpha(enabled ? 0.5f : 1f);
        if (btn2 != null) btn2.setAlpha(!enabled || isDefault ? 0.5f : 1f);
    }

    private boolean isKeyboardEnabled() {
        try {
            String enabled = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_INPUT_METHODS);
            return enabled != null && enabled.contains(getPackageName());
        } catch (Exception e) { return false; }
    }

    private boolean isKeyboardDefault() {
        try {
            String def = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
            return def != null && def.contains(getPackageName());
        } catch (Exception e) { return false; }
    }
}
