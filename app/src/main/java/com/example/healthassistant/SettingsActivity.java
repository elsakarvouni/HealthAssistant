package com.example.healthassistant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends AppCompatActivity {

    Spinner fontSpinner, spinner, spnSnoozeTime;
    EditText edtEmergencyPhone;

    private boolean isFirstLoadFont = true;
    private boolean isFirstLoadSpinner = true;
    private boolean isFirstLoadSnooze = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        updateSettings();
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        SwitchCompat swDarkMode = findViewById(R.id.swDarkMode);
        boolean isNightMode = sharedPreferences.getBoolean("DarkMode", false);
        swDarkMode.setChecked(isNightMode);

        fontSpinner = findViewById(R.id.spinnerFontSize);
        int savedSize = sharedPreferences.getInt("font_size_pos", 1);
        fontSpinner.setSelection(savedSize);

        spinner = findViewById(R.id.spinnerPriority);
        int savedPos = sharedPreferences.getInt("priority_position", 2);
        spinner.setSelection(savedPos);

        spnSnoozeTime = findViewById(R.id.spnSnoozeTime);
        String[] snoozeOptions = {
                "5 λεπτά / minutes",
                "10 λεπτά / minutes",
                "15 λεπτά / minutes",
                "30 λεπτά / minutes"
        };
        ArrayAdapter<String> snoozeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, snoozeOptions);
        snoozeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnSnoozeTime.setAdapter(snoozeAdapter);
        int savedSnoozePos = sharedPreferences.getInt("snooze_position", 0);
        spnSnoozeTime.setSelection(savedSnoozePos);

        edtEmergencyPhone = findViewById(R.id.edtEmergencyPhone);
        String savedPhone = sharedPreferences.getString("emergency_phone", "");
        edtEmergencyPhone.setText(savedPhone);

        edtEmergencyPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                editor.putString("emergency_phone", s.toString().trim());
                editor.commit();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        edtEmergencyPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String currentPhone = edtEmergencyPhone.getText().toString().trim();
                if (!currentPhone.equals(savedPhone)) {
                    Toast.makeText(SettingsActivity.this, R.string.mode_change, Toast.LENGTH_SHORT).show();
                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        swDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                editor.putBoolean("DarkMode", true);
            } else {
                editor.putBoolean("DarkMode", false);
            }
            Toast.makeText(this, R.string.mode_change, Toast.LENGTH_SHORT).show();
            editor.commit();
        });

        fontSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                editor.putInt("font_size_pos", position);
                editor.commit();

                if (!isFirstLoadFont) {
                    Toast.makeText(SettingsActivity.this, R.string.mode_change, Toast.LENGTH_SHORT).show();
                }
                isFirstLoadFont = false;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                editor.putInt("priority_position", position);
                editor.commit();

                if (!isFirstLoadSpinner) {
                    Toast.makeText(SettingsActivity.this, R.string.mode_change, Toast.LENGTH_SHORT).show();
                }
                isFirstLoadSpinner = false;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spnSnoozeTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                editor.putInt("snooze_position", position);
                editor.commit();

                if (!isFirstLoadSnooze) {
                    Toast.makeText(SettingsActivity.this, R.string.mode_change, Toast.LENGTH_SHORT).show();
                }
                isFirstLoadSnooze = false;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        findViewById(R.id.btnAbout).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.about)
                    .setMessage(R.string.infotext)
                    .setPositiveButton("OK", null)
                    .show();
        });

        findViewById(R.id.navEdit).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, EditMedicineActivity.class);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.navCalendar).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, CalendarActivity.class);
            startActivity(intent);
            finish();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void updateSettings() {
        SharedPreferences preferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean isDark = preferences.getBoolean("DarkMode", false);
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}