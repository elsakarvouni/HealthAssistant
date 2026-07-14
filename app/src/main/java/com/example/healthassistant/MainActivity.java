package com.example.healthassistant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.OnBackPressedCallback;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(applyFontSize(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        updateSettings();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DatabaseHelper dbHelper = new DatabaseHelper(this);


        dbHelper.deleteExpiredMedicines();
        autoDeletePastAppointments(dbHelper);

        updateNextPillDisplay();
        updateNextAppointmentDisplay();
        updateDailyProgramDisplay();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (doubleBackToExitPressedOnce) {
                    finishAffinity();
                    return;
                }

                doubleBackToExitPressedOnce = true;
                Toast.makeText(MainActivity.this, R.string.push_twice, Toast.LENGTH_SHORT).show();

                new Handler(Looper.getMainLooper()).postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnAddMedicine).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddMedicineActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnAddAppointment).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddAppointmentActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.navEdit).setOnClickListener(v -> {
            String[] options = {"Φάρμακα", "Ραντεβού"};
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Τι θέλετε να επεξεργαστείτε;")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            Intent intent = new Intent(MainActivity.this, EditMedicineActivity.class);
                            startActivity(intent);
                        } else if (which == 1) {
                            Intent intent = new Intent(MainActivity.this, EditAppointmentActivity.class);
                            startActivity(intent);
                        }
                    })
                    .show();
        });

        findViewById(R.id.navCalendar).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
            startActivity(intent);
        });

        new Handler(Looper.getMainLooper()).postDelayed(this::checkFirstTimeLaunch, 300);
    }

    private void autoDeletePastAppointments(DatabaseHelper dbHelper) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            db.execSQL("DELETE FROM appointments WHERE appointment_date < '" + todayDate + "'");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkFirstTimeLaunch() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        String savedPhone = sharedPreferences.getString("emergency_phone", "");

        if (savedPhone.isEmpty()) {
            final EditText input = new EditText(this);
            input.setHint("π.χ. 6912345678");
            input.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
            input.setPadding(60, 40, 60, 40);

            new AlertDialog.Builder(this)
                    .setTitle("Καλώς ήρθατε!")
                    .setMessage("Παρακαλώ εισάγετε ένα τηλέφωνο έκτακτης ανάγκης:")
                    .setView(input)
                    .setCancelable(false)
                    .setPositiveButton("Αποθήκευση", (dialog, which) -> {
                        String phoneNumber = input.getText().toString().trim();

                        if (!phoneNumber.isEmpty()) {
                            sharedPreferences.edit()
                                    .putString("emergency_phone", phoneNumber)
                                    .apply();
                            Toast.makeText(MainActivity.this, "Ο αριθμός αποθηκεύτηκε με επιτυχία!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Το τηλέφωνο είναι υποχρεωτικό!", Toast.LENGTH_SHORT).show();
                            new Handler(Looper.getMainLooper()).postDelayed(this::checkFirstTimeLaunch, 200);
                        }
                    })
                    .show();
        }
    }

    @Override
    protected void onResume() {
        updateSettings();
        super.onResume();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        autoDeletePastAppointments(dbHelper);

        updateNextPillDisplay();
        updateNextAppointmentDisplay();
        updateDailyProgramDisplay();
    }

    private void updateNextPillDisplay() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT name, hours, repeat_days FROM medicines", null);

        String nextPillTime = "";
        StringBuilder nextPillNames = new StringBuilder();
        long minDiff = Long.MAX_VALUE;

        Calendar now = Calendar.getInstance();
        int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        String currentDayName = "";
        switch (dayOfWeek) {
            case Calendar.MONDAY:    currentDayName = "Δευ"; break;
            case Calendar.TUESDAY:   currentDayName = "Τρι"; break;
            case Calendar.WEDNESDAY: currentDayName = "Τετ"; break;
            case Calendar.THURSDAY:  currentDayName = "Πεμ"; break;
            case Calendar.FRIDAY:    currentDayName = "Παρ"; break;
            case Calendar.SATURDAY:  currentDayName = "Σαβ"; break;
            case Calendar.SUNDAY:    currentDayName = "Κυρ"; break;
        }

        if (cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex("name");
            int hoursIndex = cursor.getColumnIndex("hours");
            int daysIndex = cursor.getColumnIndex("repeat_days");

            do {
                if (nameIndex != -1 && hoursIndex != -1) {
                    String repeatDays = "Καθημερινά";
                    if (daysIndex != -1 && !cursor.isNull(daysIndex)) {
                        repeatDays = cursor.getString(daysIndex);
                    }
                    if (repeatDays == null || repeatDays.isEmpty()) {
                        repeatDays = "Καθημερινά";
                    }

                    if (!repeatDays.equals("Καθημερινά") && !repeatDays.contains(currentDayName)) {
                        continue;
                    }

                    String name = cursor.getString(nameIndex);
                    String hoursStr = cursor.getString(hoursIndex);
                    String[] hoursArray = hoursStr.split("[,\\s]+");

                    for (String h : hoursArray) {
                        String[] parts = h.split(":");
                        if (parts.length == 2) {
                            int pillMinutes = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
                            int diff = pillMinutes - currentMinutes;

                            if (diff >= 0) {
                                if (diff < minDiff) {
                                    minDiff = diff;
                                    nextPillTime = h;
                                    nextPillNames = new StringBuilder(name);
                                } else if (diff == minDiff) {
                                    nextPillNames.append(", ").append(name);
                                }
                            }
                        }
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        TextView txtInfo = findViewById(R.id.txtNextPillInfo);
        TextView txtTime = findViewById(R.id.txtNextPillTime);
        if (txtInfo != null && txtTime != null) {
            if (nextPillNames.length() > 0) {
                txtTime.setText(nextPillTime);
                txtInfo.setText(nextPillNames.toString());
            } else {
                txtTime.setText("");
                txtInfo.setText(R.string.no_more_meds);
            }
        }
    }
    private void updateDailyProgramDisplay() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name, hours, repeat_days, description FROM medicines", null);

        Calendar now = Calendar.getInstance();
        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        String currentDayName = "";
        switch (dayOfWeek) {
            case Calendar.MONDAY:    currentDayName = "Δευ"; break;
            case Calendar.TUESDAY:   currentDayName = "Τρι"; break;
            case Calendar.WEDNESDAY: currentDayName = "Τετ"; break;
            case Calendar.THURSDAY:  currentDayName = "Πεμ"; break;
            case Calendar.FRIDAY:    currentDayName = "Παρ"; break;
            case Calendar.SATURDAY:  currentDayName = "Σαβ"; break;
            case Calendar.SUNDAY:    currentDayName = "Κυρ"; break;
        }
        if (cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex("name");
            int hoursIndex = cursor.getColumnIndex("hours");
            int daysIndex = cursor.getColumnIndex("repeat_days");

            do {
                if (nameIndex != -1 && hoursIndex != -1 && daysIndex != -1) {
                    String repeatDays = cursor.getString(daysIndex);
                    if (repeatDays == null || repeatDays.isEmpty()) {
                        repeatDays = "Καθημερινά";
                    }
                    if (!repeatDays.equals("Καθημερινά") && !repeatDays.contains(currentDayName)) {}
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private void updateNextAppointmentDisplay() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        Cursor cursor = db.rawQuery(
                "SELECT doctor_name, specialty, appointment_date, appointment_time FROM appointments " +
                        "WHERE appointment_date || ' ' || appointment_time >= ? " +
                        "ORDER BY appointment_date ASC, appointment_time ASC LIMIT 1",
                new String[]{currentDateTime}
        );

        TextView txtAppTime = findViewById(R.id.txtNextAppointmentTime);
        TextView txtAppInfo = findViewById(R.id.txtNextAppointmentInfo);

        if (txtAppTime != null && txtAppInfo != null) {
            if (cursor != null && cursor.moveToFirst()) {
                String docName = cursor.getString(0);
                String specialty = cursor.getString(1);
                String dateStr = cursor.getString(2);
                String timeStr = cursor.getString(3);

                txtAppTime.setText(dateStr + "   " + timeStr);
                txtAppInfo.setText(docName + " (" + specialty + ")");
            } else {
                txtAppTime.setText("");
                txtAppInfo.setText("Δεν υπάρχουν ραντεβού");
            }
        }

        if (cursor != null) {
            cursor.close();
        }
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

    public Context applyFontSize(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        int position = sharedPreferences.getInt("font_size_pos", 1);

        float fontScale;
        switch (position) {
            case 0: fontScale = 0.85f; break;
            case 2: fontScale = 1.20f; break;
            default: fontScale = 1.0f; break;
        }

        Configuration configuration = context.getResources().getConfiguration();
        configuration.fontScale = fontScale;
        return context.createConfigurationContext(configuration);
    }
}