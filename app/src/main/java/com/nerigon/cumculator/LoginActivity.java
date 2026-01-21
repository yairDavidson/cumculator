package com.nerigon.cumculator;
//loginctivity.java
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.concurrent.atomic.AtomicInteger;

public class LoginActivity extends AppCompatActivity {
    private DatabaseReference db = FirebaseDatabase.getInstance().getReference();
    private boolean isRegisterMode = false;
    private ImageButton settingBtn;
    private int theme;
    private SharedPreferences prefs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("TimerPrefs", MODE_PRIVATE);
        if (prefs.getString("login", "0").equals("1")) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.login_activity);

        TextView loginAsGuestText = findViewById(R.id.loginAsGuestText);
        TextView subtitleText = findViewById(R.id.subtitleText);
        TextInputEditText usernameInput = findViewById(R.id.inputUsername);
        TextInputEditText passwordInput = findViewById(R.id.inputPassword);
        Button actionButton = findViewById(R.id.loginButton);
        settingBtn = findViewById(R.id.settingsButton);
        TextView toggleModeText = findViewById(R.id.createAccountText);
        initalPrefernece();
        toggleModeText.setOnClickListener(v -> {
            isRegisterMode = !isRegisterMode;
            if (isRegisterMode) {
                actionButton.setText("Create Account");
                subtitleText.setText("Join the community");
                toggleModeText.setText("Already have an account? Login");
            } else {
                actionButton.setText("Login");
                subtitleText.setText("Sign in to continue");
                toggleModeText.setText("Don't have an account? Create one");
            }
        });

        TextWatcher watcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                actionButton.setEnabled(
                        usernameInput.getText().length() > 0 &&
                                passwordInput.getText().length() > 0
                );
            }
        };

        usernameInput.addTextChangedListener(watcher);
        passwordInput.addTextChangedListener(watcher);

        loginAsGuestText.setOnClickListener(v -> {
            saveLoginState(null);
            navigateToMain();
        });
        settingBtn.setOnClickListener(v -> openSettings());
        actionButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            if (password.length() < 6) {
                passwordInput.setError("Password must be at least 6 characters");
                return;
            }

            if (isRegisterMode) {
                handleRegistration(username, password, usernameInput);
            } else {
                handleLogin(username, password, passwordInput);
            }
        });
    }
    private void openSettings() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
        LinearLayout themeRow = view.findViewById(R.id.themeRow);
        TextView themeValue = view.findViewById(R.id.themeValue);
        Button btnApply = view.findViewById(R.id.btnApply);

        themeValue.setText(theme == 0 ? "System default" : (theme == 1 ? "Dark" : "Light(nerd)"));
        AtomicInteger newTheme = new AtomicInteger(theme);

        themeRow.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, themeRow, Gravity.END);
            popup.getMenu().add("System default"); popup.getMenu().add("Dark"); popup.getMenu().add("Light(nerd)");
            popup.setOnMenuItemClickListener(item -> {
                themeValue.setText(item.getTitle());
                newTheme.set(item.getTitle().equals("Dark") ? 1 : (item.getTitle().equals("Light(nerd)") ? 2 : 0));
                return true;
            });
            popup.show();
        });

        btnApply.setOnClickListener(v -> {
            theme = newTheme.get();
            getDelegate().setLocalNightMode(theme == 1 ? AppCompatDelegate.MODE_NIGHT_YES : (theme == 2 ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
            prefs.edit().putInt("theme", theme).apply();
            dialog.dismiss();
        });
        dialog.show();
    }
    private void handleLogin(String username, String password, TextInputEditText passwordInput) {
        db.child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (snapshot.exists() && user != null && user.password.equals(password)) {
                    // Sync cloud data to local preferences
                    SharedPreferences.Editor editor = getSharedPreferences("TimerPrefs", MODE_PRIVATE).edit();
                    if(user.year != 0) {
                        editor.putInt("year", user.year)
                                .putInt("month", user.month)
                                .putInt("day", user.day)
                                .putInt("hour", user.hour)
                                .putInt("minute", user.minute)
                                .putInt("second", user.second);
                    }
                    editor.apply();

                    saveLoginState(username);
                    navigateToMain();
                } else {
                    passwordInput.setError("Invalid username or password");
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
    private void initalPrefernece() {
        theme = prefs.getInt("theme", 0);
        getDelegate().setLocalNightMode(theme == 1 ? AppCompatDelegate.MODE_NIGHT_YES : (theme == 2 ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
    }
    private void handleRegistration(String username, String password, TextInputEditText usernameInput) {
        db.child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    usernameInput.setError("Username already taken");
                } else {
                    db.child("users").child(username).setValue(new User(password));
                    saveLoginState(username);
                    navigateToMain();
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void saveLoginState(String username) {
        SharedPreferences.Editor editor = getSharedPreferences("TimerPrefs", MODE_PRIVATE).edit();
        editor.putString("login", "1");
        if (username != null) {
            editor.putString("username", username);
            editor.putBoolean("isGuest", false);
        } else {
            editor.putBoolean("isGuest", true);
        }
        editor.apply();
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    abstract class SimpleTextWatcher implements TextWatcher {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void afterTextChanged(Editable s) {}
    }
}