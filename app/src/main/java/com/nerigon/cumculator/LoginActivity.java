package com.nerigon.cumculator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private static final String PREF_NAME = "UserPrefs";
    private static final String KEY_CURRENT_USER = "current_user";
    private static final String KEY_ALL_USERS = "all_users";
    private static final String KEY_LOGIN_STATUS = "login_status";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        SharedPreferences prefs =
                getSharedPreferences("TimerPrefs", MODE_PRIVATE);
        if (prefs.getString("login", "0").equals("1")) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.login_activity);
        TextView loginAsGuestText = findViewById(R.id.loginAsGuestText);

        loginAsGuestText.setOnClickListener(v -> {
            prefs.edit().putString("login", "1").apply();

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish(); // important
        });
    }
    public static void logout(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_LOGIN_STATUS, false);
        editor.remove(KEY_CURRENT_USER);
        editor.apply();
    }
}
