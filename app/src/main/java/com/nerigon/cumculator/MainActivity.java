package com.nerigon.cumculator;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    // UI Elements
    private TextView timeText;
    private TextView monthDaysText;
    private Button dateButton;
    private Button timeButton;
    private Button nowButton;
    private ImageButton settingBtn;
    // Time Tracking
    private final Calendar calendar = Calendar.getInstance();
    private int theme = 0;

    private int selectedYear = calendar.get(Calendar.YEAR);
    private int selectedMonth = calendar.get(Calendar.MONTH);
    private int selectedDay = calendar.get(Calendar.DAY_OF_MONTH);
    private int selectedHour = calendar.get(Calendar.HOUR_OF_DAY);
    private int selectedMinute = calendar.get(Calendar.MINUTE);
    private int selectedSecond = calendar.get(Calendar.SECOND);
    private long startTimeMillis = System.currentTimeMillis();

    // Handler and Preferences
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE);

        String login = prefs.getString("login","0");
        if(prefs.getString("login","0").equals("0")){
            setContentView(R.layout.login_activity);
            prefs.edit().putString("login","1").apply();
        }
        else{
        setContentView(R.layout.activity_main);

        new UpdateChecker(this).checkForUpdates();
        timeText = findViewById(R.id.timeText);
        monthDaysText = findViewById(R.id.monthDaysText);
        dateButton = findViewById(R.id.dateButton);
        timeButton = findViewById(R.id.timeButton);
        nowButton = findViewById(R.id.nowButton);
        settingBtn = findViewById(R.id.settingsButton);

        initializeTime(); // get saved time, and if not exist reset to now
        initalPrefernece();
        updateDateTimeButtons();
        updateStartTime();
        startTimer();

        dateButton.setOnClickListener(v -> showDatePicker());
        settingBtn.setOnClickListener(v -> openSettings());
        timeButton.setOnClickListener(v -> showTimePicker());
        nowButton.setOnClickListener(v -> showResetConfirmation());}
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showDatePicker() {
        new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    selectedYear = year;
                    selectedMonth = month;
                    selectedDay = day;
                    selectedSecond = 0;
                    updateDateTimeButtons();
                    updateStartTime();
                },
                selectedYear,
                selectedMonth,
                selectedDay
        ).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showTimePicker() {
        new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    selectedHour = hour;
                    selectedMinute = minute;
                    selectedSecond = 0; // reset seconds when choosing a new time
                    updateDateTimeButtons();
                    updateStartTime();
                },
                selectedHour,
                selectedMinute,
                true // true = 24-hour mode, false = 12-hour mode
        ).show();
    }
    private void openSettings() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_settings, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(true)
                .create();

        LinearLayout themeRow = view.findViewById(R.id.themeRow);
        TextView themeValue = view.findViewById(R.id.themeValue);
        //ImageButton themeV = view.findViewById(R.id.themeV);
        Button btnCancel = view.findViewById(R.id.btnCANCEL);
        Button btnApply = view.findViewById(R.id.btnApply);

        switch (theme) {
            case 0: themeValue.setText("System default"); break;
            case 1: themeValue.setText("Dark"); break;
            case 2: themeValue.setText("Light(nerd)"); break;
        }

        AtomicInteger newTheme = new AtomicInteger(theme);

        themeRow.setOnClickListener(v -> {
            View root = findViewById(android.R.id.content);
            PopupMenu popup = new PopupMenu(this, themeRow , Gravity.END);

            popup.getMenu().add("System default");
            popup.getMenu().add("Dark");
            popup.getMenu().add("Light(nerd)");

            popup.setOnMenuItemClickListener(item -> {
                String choice = item.getTitle().toString();
                themeValue.setText(choice);

                switch (choice) {
                    case "Dark": newTheme.set(1); break;
                    case "Light(nerd)": newTheme.set(2); break;
                    case "System default": newTheme.set(0); break;
                }
                return true;
            });
            popup.show();
        });

        btnCancel.setOnClickListener(v -> {
            newTheme.set(theme); // discard changes
            dialog.dismiss();
        });

        btnApply.setOnClickListener(v -> {
            // Apply chosen theme now
            theme = newTheme.get();
            switch (theme) {
                case 0:
                    getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    break;
                case 1:
                    getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    break;
                case 2:
                    getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    break;
            }
            prefs.edit().putInt("theme", theme).apply();
            dialog.dismiss();
        });

        dialog.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showResetConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_confirmation_title)
                .setMessage(R.string.reset_confirmation_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    setStartToNow();
                    updateStartTime();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
private void initalPrefernece() {
    theme = prefs.getInt("theme", 0);

    switch (theme) {
        case 0:
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            break;
        case 1:
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            break;
        case 2:
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            break;
    }

}
    private void updateDateTimeButtons() {
        String[] months = getResources().getStringArray(R.array.months);
        dateButton.setText(months[selectedMonth] + " " + selectedDay + ", " + selectedYear);
        timeButton.setText(String.format("%02d:%02d", selectedHour, selectedMinute));
    }

    private void setStartToNow() {
        Calendar calendar = Calendar.getInstance();
        selectedYear = calendar.get(Calendar.YEAR);
        selectedMonth = calendar.get(Calendar.MONTH);
        selectedDay = calendar.get(Calendar.DAY_OF_MONTH);
        selectedHour = calendar.get(Calendar.HOUR_OF_DAY);
        selectedMinute = calendar.get(Calendar.MINUTE);
        selectedSecond = calendar.get(Calendar.SECOND);
        updateDateTimeButtons();
    }

    private void initializeTime() { // get saved time, and if not exist reset to now
        Calendar calendar = Calendar.getInstance();
        selectedYear = prefs.getInt("year", calendar.get(Calendar.YEAR));
        selectedMonth = prefs.getInt("month", calendar.get(Calendar.MONTH));
        selectedDay = prefs.getInt("day", calendar.get(Calendar.DAY_OF_MONTH));
        selectedHour = prefs.getInt("hour", calendar.get(Calendar.HOUR_OF_DAY));
        selectedMinute = prefs.getInt("minute", calendar.get(Calendar.MINUTE));
        selectedSecond = prefs.getInt("second", calendar.get(Calendar.SECOND));
    }

    private void startTimer() {
        handler.post(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                long elapsed = currentTime > startTimeMillis ? currentTime - startTimeMillis : 0L;
                timeText.setText(formatDisplayTime(elapsed));
                monthDaysText.setText(formatDaysTime(elapsed));
                handler.postDelayed(this, 200); // Update every 200ms
            }
        });
    }

    private String formatDisplayTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String formatDaysTime(long millis) {
        Calendar calendar = Calendar.getInstance();
        long totalDays = TimeUnit.MILLISECONDS.toDays(millis);
        int years = calendar.get(Calendar.YEAR) - selectedYear;
        int months = calendar.get(Calendar.MONTH) - selectedMonth + years * 12;
        int days = calendar.get(Calendar.DAY_OF_MONTH) - selectedDay;

        if (days < 0) {
            LocalDate today = LocalDate.now();
            LocalDate lastMonthSameDay = today.minusMonths(1);
            days += (int) ChronoUnit.DAYS.between(lastMonthSameDay, today);
            months--;
        }

        if (selectedHour * 60 + selectedMinute > calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)) {
            days--;
        }

        if (millis < 10000) {
            months = 0;
            days = 0;
        }

        return String.format("%2d days, which is \n %2d months & %2d days", totalDays, months, days);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateStartTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, selectedSecond);
        startTimeMillis = calendar.getTimeInMillis();

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - startTimeMillis;

        timeText.setText(formatDisplayTime(elapsed));
        monthDaysText.setText(formatDaysTime(elapsed));
        saveCurrentTime();
    }

    private void saveCurrentTime() {
        prefs.edit()
                .putInt("year", selectedYear)
                .putInt("month", selectedMonth)
                .putInt("day", selectedDay)
                .putInt("hour", selectedHour)
                .putInt("minute", selectedMinute)
                .putInt("second", selectedSecond)
                .apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCurrentTime();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
