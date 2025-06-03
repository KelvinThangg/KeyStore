package com.example.passwordmanager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "PasswordManagerPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_HAS_PIN = "hasPin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Kiểm tra trạng thái đăng nhập
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
        boolean hasPin = sharedPreferences.getBoolean(KEY_HAS_PIN, false);

        if (isLoggedIn && hasPin) {
            // Đã đăng nhập và có PIN -> chuyển đến màn hình nhập PIN
            startActivity(new Intent(this, PinActivity.class));
        } else if (isLoggedIn && !hasPin) {
            // Đã đăng nhập nhưng chưa có PIN -> chuyển đến màn hình tạo PIN
            startActivity(new Intent(this, CreatePinActivity.class));
        } else {
            // Chưa đăng nhập -> chuyển đến màn hình đăng nhập
            startActivity(new Intent(this, LoginActivity.class));
        }

        finish();
    }
}