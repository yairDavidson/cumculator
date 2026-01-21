package com.nerigon.cumculator;

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
import android.view.Menu;
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

public abstract class BaseActivity extends AppCompatActivity {

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_DARK   = 1;
    public static final int THEME_LIGHT  = 2;
    protected int THEME;
    protected SharedPreferences prefs; //= getSharedPreferences("TimerPrefs", MODE_PRIVATE);;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("TimerPrefs", MODE_PRIVATE);
    }
    protected void openSettingsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();

        LinearLayout themeRow = view.findViewById(R.id.themeRow);
        TextView themeTitle = view.findViewById(R.id.themeValue);
        Button btnApply = view.findViewById(R.id.btnApply);
        Button btnCancel = view.findViewById(R.id.btnCANCEL);

        themeTitle.setText(
                THEME == THEME_SYSTEM ? "System default" :
                        (THEME == THEME_DARK ? "Dark" : "Light")
        );

        AtomicInteger newTHEME = new AtomicInteger(THEME);

        themeRow.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, themeRow, Gravity.END);
            popup.getMenu().add(Menu.NONE, THEME_SYSTEM, 0, R.string.theme_system);
            popup.getMenu().add(Menu.NONE, THEME_DARK, 1, R.string.theme_dark);
            popup.getMenu().add(Menu.NONE, THEME_LIGHT, 2, R.string.theme_light);

            popup.setOnMenuItemClickListener(item -> {
                newTHEME.set(item.getItemId());
                themeTitle.setText(item.getTitle());
                return true;
            });
            popup.show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnApply.setOnClickListener(v -> {
            THEME = newTHEME.get();
            applyTheme(THEME);
            dialog.dismiss();
        });

        dialog.show();
    }
    protected void initialThemePreferenceFromLocal() {
        THEME = prefs.getInt("THEME", THEME_SYSTEM);
        applyTheme(THEME);
    }
    protected void applyTheme(int theme) {
        prefs.edit().putInt("THEME", theme).apply();
        switch (theme) {
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_YES
                );
                break;

            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_NO
                );
                break;

            default:
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                );
        }
    }

}
