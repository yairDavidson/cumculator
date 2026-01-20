package com.nerigon.cumculator;
//MainActivity.java
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    private TextView timeText, monthDaysText;
    private Button dateButton, timeButton, nowButton;
    private ImageButton settingBtn, logout;

    private final Calendar calendar = Calendar.getInstance();
    private int theme = 0;
    private int selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, selectedSecond;
    private long startTimeMillis;

    // NEW FLAG: Prevents auto-save during logout
    private boolean isLoggingOut = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("TimerPrefs", MODE_PRIVATE);

        if (!prefs.getString("login", "0").equals("1")) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        new UpdateChecker(this).checkForUpdates();

        logout = findViewById(R.id.LogOut);
        timeText = findViewById(R.id.timeText);
        monthDaysText = findViewById(R.id.monthDaysText);
        dateButton = findViewById(R.id.dateButton);
        timeButton = findViewById(R.id.timeButton);
        nowButton = findViewById(R.id.nowButton);
        settingBtn = findViewById(R.id.settingsButton);

        logout.setOnClickListener(v -> logout());
        initializeTime();
        initalPrefernece();
        updateDateTimeButtons();
        updateStartTime();
        startTimer();

        dateButton.setOnClickListener(v -> showDatePicker());
        timeButton.setOnClickListener(v -> showTimePicker());
        nowButton.setOnClickListener(v -> showResetConfirmation());
        settingBtn.setOnClickListener(v -> openSettings());
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure? This will remove your timer from this device.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // 1. Set flag so onPause doesn't save data back
                    isLoggingOut = true;

                    // 2. Wipe everything (login status, username, and local timer data)
                    prefs.edit().clear().apply();

                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void saveCurrentTime() {
        // Double check to ensure we don't save if logging out
        if (isLoggingOut) return;

        prefs.edit()
                .putInt("year", selectedYear)
                .putInt("month", selectedMonth)
                .putInt("day", selectedDay)
                .putInt("hour", selectedHour)
                .putInt("minute", selectedMinute)
                .putInt("second", selectedSecond)
                .apply();

        // Sync to cloud if not guest
        boolean isGuest = prefs.getBoolean("isGuest", false);
        if (!isGuest) {
            syncDataToCloud();
        }
    }

    private void syncDataToCloud() {
        String username = prefs.getString("username", null);
        if (username != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(username);
            Map<String, Object> updates = new HashMap<>();
            updates.put("year", selectedYear);
            updates.put("month", selectedMonth);
            updates.put("day", selectedDay);
            updates.put("hour", selectedHour);
            updates.put("minute", selectedMinute);
            updates.put("second", selectedSecond);
            userRef.updateChildren(updates);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            selectedYear = year; selectedMonth = month; selectedDay = day; selectedSecond = 0;
            updateDateTimeButtons();
            updateStartTime();
        }, selectedYear, selectedMonth, selectedDay).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showTimePicker() {
        new TimePickerDialog(this, (view, hour, minute) -> {
            selectedHour = hour; selectedMinute = minute; selectedSecond = 0;
            updateDateTimeButtons();
            updateStartTime();
        }, selectedHour, selectedMinute, true).show();
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showResetConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_confirmation_title)
                .setPositiveButton(R.string.yes, (d, w) -> { setStartToNow(); updateStartTime(); })
                .setNegativeButton(R.string.cancel, null).show();
    }

    private void initalPrefernece() {
        theme = prefs.getInt("theme", 0);
        getDelegate().setLocalNightMode(theme == 1 ? AppCompatDelegate.MODE_NIGHT_YES : (theme == 2 ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
    }

    private void updateDateTimeButtons() {
        String[] months = getResources().getStringArray(R.array.months);
        String monthString = (selectedMonth >= 0 && selectedMonth < months.length) ? months[selectedMonth] : "";
        dateButton.setText(monthString + " " + selectedDay + ", " + selectedYear);
        timeButton.setText(String.format("%02d:%02d", selectedHour, selectedMinute));
    }

    private void setStartToNow() {
        Calendar c = Calendar.getInstance();
        selectedYear = c.get(Calendar.YEAR); selectedMonth = c.get(Calendar.MONTH); selectedDay = c.get(Calendar.DAY_OF_MONTH);
        selectedHour = c.get(Calendar.HOUR_OF_DAY); selectedMinute = c.get(Calendar.MINUTE); selectedSecond = c.get(Calendar.SECOND);
        updateDateTimeButtons();
    }

    private void initializeTime() {
        Calendar c = Calendar.getInstance();
        selectedYear = prefs.getInt("year", c.get(Calendar.YEAR));
        selectedMonth = prefs.getInt("month", c.get(Calendar.MONTH));
        selectedDay = prefs.getInt("day", c.get(Calendar.DAY_OF_MONTH));
        selectedHour = prefs.getInt("hour", c.get(Calendar.HOUR_OF_DAY));
        selectedMinute = prefs.getInt("minute", c.get(Calendar.MINUTE));
        selectedSecond = prefs.getInt("second", c.get(Calendar.SECOND));
    }

    private void startTimer() {
        handler.post(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                long elapsed = Math.max(0L, System.currentTimeMillis() - startTimeMillis);
                timeText.setText(formatDisplayTime(elapsed));
                monthDaysText.setText(formatDaysTime(elapsed));
                handler.postDelayed(this, 200);
            }
        });
    }

    private String formatDisplayTime(long millis) {
        long h = TimeUnit.MILLISECONDS.toHours(millis);
        long m = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long s = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String formatDaysTime(long millis) {
        Calendar c = Calendar.getInstance();
        long totalDays = TimeUnit.MILLISECONDS.toDays(millis);
        int years = c.get(Calendar.YEAR) - selectedYear;
        int months = c.get(Calendar.MONTH) - selectedMonth + years * 12;
        int days = c.get(Calendar.DAY_OF_MONTH) - selectedDay;

        if (days < 0) {
            days += (int) ChronoUnit.DAYS.between(LocalDate.now().minusMonths(1), LocalDate.now());
            months--;
        }
        if (selectedHour * 60 + selectedMinute > c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)) days--;
        if (millis < 10000) { months = 0; days = 0; }

        return String.format("%2d days, which is \n %2d months & %2d days", totalDays, months, days);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateStartTime() {
        Calendar c = Calendar.getInstance();
        c.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, selectedSecond);
        startTimeMillis = c.getTimeInMillis();
        long elapsed = System.currentTimeMillis() - startTimeMillis;
        timeText.setText(formatDisplayTime(elapsed));
        monthDaysText.setText(formatDaysTime(elapsed));
        saveCurrentTime();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // FIX: Only save if we are NOT logging out
        if (!isLoggingOut) {
            saveCurrentTime();
        }
    }

    @Override protected void onDestroy() { super.onDestroy(); handler.removeCallbacksAndMessages(null); }
}