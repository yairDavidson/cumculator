package com.nerigon.cumculator;
//MainActivity.java

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    private TextView V_Counter, V_MonthsAndDays; //00:00:00 + M & d Days
    private Button V_dateButton, V_timeButton, V_resetButton;
    private ImageButton V_settingBtn, V_logoutButton;
    private int theme = 0;//theme 0 = system default, 1 = dark, 2 = light
    private int selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, selectedSecond;
    private long millisOfStartPoint;
    private boolean isLoggingOut = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("TimerPrefs", MODE_PRIVATE);

        if (prefs.getString("login", "no_account").equals("no_account")) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        new UpdateChecker(this).checkForUpdates();

        LinkVisualComponents();
        initialThemePreferenceFromLocal();
        initialSelectedFromLocal();
        executeSelectedTime();
        startTimer();
        initialSetOnClickListeners();
    }
    //---------------used in OnCreate:
    private void LinkVisualComponents() {
        V_logoutButton = findViewById(R.id.logoutButton);
        V_Counter = findViewById(R.id.visualCounter);
        V_MonthsAndDays = findViewById(R.id.monthDaysText);
        V_dateButton = findViewById(R.id.dateButton);
        V_timeButton = findViewById(R.id.timeButton);
        V_resetButton = findViewById(R.id.resetButton);
        V_settingBtn = findViewById(R.id.settingsButton);
    }//V
    private void initialSetOnClickListeners() {
        V_logoutButton.setOnClickListener(v -> logoutDialog());
        V_dateButton.setOnClickListener(v -> showDatePickerDialog());
        V_timeButton.setOnClickListener(v -> showTimePickerDialog());
        V_resetButton.setOnClickListener(v -> ResetConfirmationDialog());
        V_settingBtn.setOnClickListener(v -> openSettingsDialog());
    }//V
    private void initialThemePreferenceFromLocal() {
        theme = prefs.getInt("theme", 0);
        getDelegate().setLocalNightMode(theme == 1 ? AppCompatDelegate.MODE_NIGHT_YES : (theme == 2 ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
    }
    private void initialSelectedFromLocal() {
        Calendar c = Calendar.getInstance();
        selectedYear = prefs.getInt("year", c.get(Calendar.YEAR));
        selectedMonth = prefs.getInt("month", c.get(Calendar.MONTH));
        selectedDay = prefs.getInt("day", c.get(Calendar.DAY_OF_MONTH));
        selectedHour = prefs.getInt("hour", c.get(Calendar.HOUR_OF_DAY));
        selectedMinute = prefs.getInt("minute", c.get(Calendar.MINUTE));
        selectedSecond = prefs.getInt("second", c.get(Calendar.SECOND));
    }

    private void executeSelectedTime() {
        //update pick time buttons:
        String[] months = getResources().getStringArray(R.array.months);
        String monthString = (selectedMonth >= 0 && selectedMonth < months.length) ? months[selectedMonth] : "";
        V_dateButton.setText(monthString + " " + selectedDay + ", " + selectedYear);
        V_timeButton.setText(String.format("%02d:%02d", selectedHour, selectedMinute));
        //update visual counter:
        Calendar selectedTime = Calendar.getInstance();
        selectedTime.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, selectedSecond);
        millisOfStartPoint = selectedTime.getTimeInMillis();
        long elapsed = System.currentTimeMillis() - millisOfStartPoint;
        V_Counter.setText(formatHoursDisplay(elapsed));
        V_MonthsAndDays.setText(formatMonthDaysTime(elapsed));
        //update data bases:
        syncDataToLocal();
        syncDataToCloud();
    }//execute to visual and to data bases
    private void startTimer() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = Math.max(0L, System.currentTimeMillis() - millisOfStartPoint);
                V_Counter.setText(formatHoursDisplay(elapsed));
                V_MonthsAndDays.setText(formatMonthDaysTime(elapsed));
                handler.postDelayed(this, 200);
            }
        });
    }//V visible continues counter
    //-----------------
    private void ResetConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_confirmation_title)
                .setMessage(R.string.reset_confirmation_message)
                .setPositiveButton(R.string.yes, (d, w) -> {
                    resetSelectedToNow();
                    executeSelectedTime();
                })
                .setNegativeButton(R.string.cancel, null).show();
    }
    private void resetSelectedToNow() {
        Calendar c = Calendar.getInstance();
        selectedYear = c.get(Calendar.YEAR);
        selectedMonth = c.get(Calendar.MONTH);
        selectedDay = c.get(Calendar.DAY_OF_MONTH);
        selectedHour = c.get(Calendar.HOUR_OF_DAY);
        selectedMinute = c.get(Calendar.MINUTE);
        selectedSecond = c.get(Calendar.SECOND);
    }//selected time to now
    private void openSettingsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
        LinearLayout themeRow = view.findViewById(R.id.themeRow);
        TextView themeValue = view.findViewById(R.id.themeValue);
        Button btnApply = view.findViewById(R.id.btnApply);
        Button btnCancel = view.findViewById(R.id.btnCANCEL);

        themeValue.setText(theme == 0 ? "System default" : (theme == 1 ? "Dark" : "Light(nerd)"));
        AtomicInteger newTheme = new AtomicInteger(theme);

        themeRow.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, themeRow, Gravity.END);
            popup.getMenu().add("System default");
            popup.getMenu().add("Dark");
            popup.getMenu().add("Light(nerd)");
            popup.setOnMenuItemClickListener(item -> {
                themeValue.setText(item.getTitle());
                newTheme.set(Objects.equals(item.getTitle(), "Dark") ? 1 : (Objects.equals(item.getTitle(), "Light(nerd)") ? 2 : 0));
                return true;
            });
            popup.show();
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnApply.setOnClickListener(v -> {
            theme = newTheme.get();
            getDelegate().setLocalNightMode(theme == 1 ? AppCompatDelegate.MODE_NIGHT_YES : (theme == 2 ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
            prefs.edit().putInt("theme", theme).apply();
            dialog.dismiss();
        });
        dialog.show();
    }
    private void logoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure? This will remove your timer from this device.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // 1. Set flag so onPause doesn't save data back
                    isLoggingOut = true;

                    // 2. Wipe everything (login status, username, and local timer data)
                    //theme = prefs.getInt("theme", 0);
                    prefs.edit().clear().apply();
                    prefs.edit().putInt("theme", theme).apply();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }//logout (need to be investigate)
    private void syncDataToLocal() {
        prefs.edit()
                .putInt("year", selectedYear)
                .putInt("month", selectedMonth)
                .putInt("day", selectedDay)
                .putInt("hour", selectedHour)
                .putInt("minute", selectedMinute)
                .putInt("second", selectedSecond)
                .apply();

    }//V
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
    }//V
    @Override
    protected void onPause() {
        super.onPause();
        // FIX: Only save if we are NOT logging out
        if (!isLoggingOut) {
            syncDataToLocal();
            syncDataToCloud();
        }
    }//sync both data bases when pause
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    } //destroy handler and running process when activity is closed

    private String formatHoursDisplay(long millis) {
        long h = TimeUnit.MILLISECONDS.toHours(millis);
        long m = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long s = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }//convert millis to hours:minutes:seconds

    private String formatMonthDaysTime(long millis) {
        Calendar c = Calendar.getInstance();
        long totalDays = TimeUnit.MILLISECONDS.toDays(millis);
        int years = c.get(Calendar.YEAR) - selectedYear;
        int months = c.get(Calendar.MONTH) - selectedMonth + years * 12;
        int days = c.get(Calendar.DAY_OF_MONTH) - selectedDay;

        if (days < 0) {
            days += (int) ChronoUnit.DAYS.between(LocalDate.now().minusMonths(1), LocalDate.now());
            months--;
        }
        if (selectedHour * 60 + selectedMinute > c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE))
            days--;
        if (millis < 10000) {
            months = 0;
            days = 0;
        }

        return String.format("%2d days, which is \n %2d months & %2d days", totalDays, months, days);
    }//convert millis to months and days
    private void showDatePickerDialog() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            selectedYear = year;
            selectedMonth = month;
            selectedDay = day;
            selectedSecond = 0;
            executeSelectedTime();
        }, selectedYear, selectedMonth, selectedDay).show();
    }//open pick date dialog
    private void showTimePickerDialog() {
        new TimePickerDialog(this, (view, hour, minute) -> {
            selectedHour = hour;
            selectedMinute = minute;
            selectedSecond = 0;
            executeSelectedTime();
        }, selectedHour, selectedMinute, true).show();
    }//open pick time dialog
}
