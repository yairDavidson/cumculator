package com.nerigon.cumculator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

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
}
