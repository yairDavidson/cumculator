package com.nerigon.cumculator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private static final String KEY_CURRENT_USER = "current_user";
    private static final String KEY_LOGIN_STATUS = "login_status";
    DatabaseReference db = FirebaseDatabase.getInstance().getReference();

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
        TextInputEditText usernameInput = findViewById(R.id.inputUsername);
        TextInputEditText passwordInput = findViewById(R.id.inputpassword);
        Button loginButton = findViewById(R.id.loginButton);
        TextView createAccountText = findViewById(R.id.createAccountText);

        TextWatcher watcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loginButton.setEnabled(
                        usernameInput.getText().length() > 0 &&
                                passwordInput.getText().length() > 0
                );
            }
        };

        usernameInput.addTextChangedListener(watcher);
        passwordInput.addTextChangedListener(watcher);
        loginAsGuestText.setOnClickListener(v -> {
            prefs.edit().putString("login", "1").apply();

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish(); // important
        });
        loginButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            db.child("users").child(username)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (!snapshot.exists()) {
                                passwordInput.setError("User does not exist");
                                return;
                            }

                            User user = snapshot.getValue(User.class);
                            if (user != null && user.password.equals(password)) {
                                SharedPreferences prefs =
                                        getSharedPreferences("TimerPrefs", MODE_PRIVATE);
                                prefs.edit().putString("login", "1").apply();

                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            } else {
                                passwordInput.setError("Wrong password");
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {}
                    });
        });
        createAccountText.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            if (username.isEmpty() || password.isEmpty()) return;

            db.child("users").child(username)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                usernameInput.setError("Username already taken");
                                return;
                            }

                            db.child("users").child(username)
                                    .setValue(new User(password));

                            SharedPreferences prefs =
                                    getSharedPreferences("TimerPrefs", MODE_PRIVATE);
                            prefs.edit().putString("login", "1").apply();

                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {}
                    });
        });

    }
    abstract class SimpleTextWatcher implements TextWatcher {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void afterTextChanged(Editable s) {}
    }
    public static void logout(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_LOGIN_STATUS, false);
        editor.remove(KEY_CURRENT_USER);
        editor.apply();
    }
}
