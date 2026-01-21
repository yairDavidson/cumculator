package com.nerigon.cumculator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;

public class LoginActivity extends BaseActivity {

    // CONSTANTS: We use these to prevent typing errors when accessing data later.
    // If we change a key here, it updates everywhere.
    private static final String PREFS_NAME = "TimerPrefs"; // Name of the local storage file
    private static final String KEY_LOGIN_STATE = "login"; // Key to check if user is logged in
    private static final String KEY_USERNAME = "username"; // Key to store the username locally
    private static final String KEY_IS_GUEST = "isGuest"; // Key to flag if they are a guest
    private static final String DB_USERS = "users"; // Name of the node in Firebase Database

    // FIREBASE: Get a reference to the root of the database
    private final DatabaseReference db = FirebaseDatabase.getInstance().getReference();

    // STATE: Tracks if the user is currently looking at "Login" or "Create Account"
    private boolean isRegisterMode = false;

    // UI ELEMENTS: Variables to hold references to the views on screen
    private ImageButton V_settingsBtn;
    private TextView V_loginAsGuestText;
    private TextView V_subtitleText;
    private TextInputEditText V_usernameInput;
    private TextInputEditText V_passwordInput;
    private Button V_actionButton; // This button serves as both "Login" and "Create Account"
    private TextView V_toggleModeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        linkVisualComponents();
        initialThemePreferenceFromLocal();

        // 3. Attach listeners to inputs to enable/disable the button
        setupInputValidation();

        // 4. Set up what happens when buttons are clicked
        setupClickListeners();
    }

    private void linkVisualComponents() {
        // Connects Java variables to the XML IDs
        V_loginAsGuestText = findViewById(R.id.loginAsGuestText);
        V_subtitleText = findViewById(R.id.subtitleText);
        V_usernameInput = findViewById(R.id.inputUsername);
        V_passwordInput = findViewById(R.id.inputPassword);
        V_actionButton = findViewById(R.id.loginButton);
        V_settingsBtn = findViewById(R.id.settingsButton);
        V_toggleModeText = findViewById(R.id.createAccountText);
    }

    private void setupInputValidation() {
        // Watcher monitors keystrokes in real-time
        TextWatcher watcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Every time text changes, check if the form is valid
                validateForm();
            }
        };
        // Attach the watcher to both fields
        V_usernameInput.addTextChangedListener(watcher);
        V_passwordInput.addTextChangedListener(watcher);
    }

    private void validateForm() {
        // Get current text, trimming whitespace from username
        String user = V_usernameInput.getText() != null ? V_usernameInput.getText().toString().trim() : "";
        String pass = V_passwordInput.getText() != null ? V_passwordInput.getText().toString() : "";

        // Enable the button ONLY if both fields have text
        V_actionButton.setEnabled(!user.isEmpty() && !pass.isEmpty());
    }

    private void setupClickListeners() {
        // Open settings dialog
        V_settingsBtn.setOnClickListener(v -> openSettingsDialog());

        // Guest Logic: Save empty username and navigate immediately
        V_loginAsGuestText.setOnClickListener(v -> {
            saveLoginState(null); // null indicates guest
            navigateToMain();
        });

        // Switch between Login and Registration views
        V_toggleModeText.setOnClickListener(v -> toggleMode());

        // Main Action Button (Login or Register)
        V_actionButton.setOnClickListener(v -> {
            String username = V_usernameInput.getText().toString().trim();
            String password = V_passwordInput.getText().toString();

            // Basic client-side validation before hitting the server
            if (password.length() < 6) {
                V_passwordInput.setError("Password must be at least 6 characters");
                return;
            }

            setLoading(true); // Disable UI to prevent double-clicking

            if (isRegisterMode) {
                handleRegistration(username, password);
            } else {
                handleLogin(username, password);
            }
        });
    }

    private void toggleMode() {
        // Flip the boolean switch
        isRegisterMode = !isRegisterMode;

        // Update UI text to reflect the new mode
        if (isRegisterMode) {
            V_actionButton.setText("Create Account");
            V_subtitleText.setText("Join the community");
            V_toggleModeText.setText("Already have an account? Login");
        } else {
            V_actionButton.setText("Login");
            V_subtitleText.setText("Sign in to continue");
            V_toggleModeText.setText("Don't have an account? Create one");
        }
        // Clear old errors so they don't confuse the user
        V_usernameInput.setError(null);
        V_passwordInput.setError(null);
    }

    private void handleLogin(String username, String password) {
        // Query Firebase: Look at 'users' -> 'username'
        db.child(DB_USERS).child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                setLoading(false); // Re-enable UI

                // Convert the JSON data from Firebase into a User java object
                User user = snapshot.getValue(User.class);

                // Check if user exists AND password matches
                if (snapshot.exists() && user != null && password.equals(user.password)) {
                    syncUserDataToPrefs(user); // Save their data locally
                    saveLoginState(username);  // Mark as logged in
                    navigateToMain();          // Go to next screen
                } else {
                    V_passwordInput.setError("Invalid username or password");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleRegistration(String username, String password) {
        // First, check if the username already exists to prevent overwriting
        db.child(DB_USERS).child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    setLoading(false);
                    V_usernameInput.setError("Username already taken");
                } else {
                    // Create new User object
                    User newUser = new User(password);

                    // Save to Firebase: users -> username = newUser data
                    db.child(DB_USERS).child(username).setValue(newUser)
                            .addOnSuccessListener(aVoid -> {
                                // On success, save state locally and move to main
                                saveLoginState(username);
                                navigateToMain();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(LoginActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper to disable buttons while network request is happening
    private void setLoading(boolean isLoading) {
        V_actionButton.setEnabled(!isLoading);
        V_loginAsGuestText.setEnabled(!isLoading);
        V_toggleModeText.setEnabled(!isLoading);
        // Change button text to give feedback
        V_actionButton.setText(isLoading ? "Processing..." : (isRegisterMode ? "Create Account" : "Login"));
    }

    // Saves the user's timer data (year, month, etc.) from Firebase into local phone storage
    private void syncUserDataToPrefs(User user) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        if (user.year != 0) { // Only save if data is valid
            editor.putInt("year", user.year)
                    .putInt("month", user.month)
                    .putInt("day", user.day)
                    .putInt("hour", user.hour)
                    .putInt("minute", user.minute)
                    .putInt("second", user.second);
        }
        editor.apply(); // Commit changes asynchronously
    }

    // Marks the app as "Logged In" so the user isn't asked to login next time they open the app
    private void saveLoginState(String username) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(KEY_LOGIN_STATE, "1"); // "1" means logged in
        if (username != null) {
            editor.putString(KEY_USERNAME, username);
            editor.putBoolean(KEY_IS_GUEST, false);
        } else {
            editor.putBoolean(KEY_IS_GUEST, true);
        }
        editor.apply();
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish(); // Closes LoginActivity so pressing "Back" doesn't return here
    }

    // Abstract helper class: We only need 'onTextChanged', so this hides the other 2 unused methods
    abstract static class SimpleTextWatcher implements TextWatcher {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void afterTextChanged(Editable s) {}
    }
}