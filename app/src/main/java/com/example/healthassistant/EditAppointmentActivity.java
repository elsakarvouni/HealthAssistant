package com.example.healthassistant;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.Calendar;
import java.util.List;

public class EditAppointmentActivity extends AppCompatActivity {
    Spinner spnAppointments;
    EditText edtDocName, edtSpecialty, edtAppDate, edtAppTime, edtDocPhone;
    Button btnUpdate, btnDelete;
    DatabaseHelper dbHelper;
    private String nameFromSpinner = "";
    private String dateFromSpinner = "";
    private String timeFromSpinner = "";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(applyFontSize(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        updateSettings();
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_appointment);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);
        spnAppointments = findViewById(R.id.spnAppointments);
        edtDocName = findViewById(R.id.editDocName);
        edtSpecialty = findViewById(R.id.editSpecialty);
        edtAppDate = findViewById(R.id.editAppDate);
        edtAppTime = findViewById(R.id.editAppTime);
        edtDocPhone = findViewById(R.id.editDocPhone);
        btnUpdate = findViewById(R.id.btnUpdateAppointment);
        btnDelete = findViewById(R.id.btnDeleteAppointment);

        loadSpinnerData();

        edtAppDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
                String selectedDate = year1 + "-" + String.format("%02d", (month1 + 1)) + "-" + String.format("%02d", dayOfMonth);
                edtAppDate.setText(selectedDate);
            }, year, month, day);
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        edtAppTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
                String selectedTime = String.format("%02d:%02d", hourOfDay, minuteOfHour);
                edtAppTime.setText(selectedTime);
            }, hour, minute, true);
            timePickerDialog.show();
        });

        btnUpdate.setOnClickListener(v -> {
            String newName = edtDocName.getText().toString().trim();
            String specialty = edtSpecialty.getText().toString().trim();
            String date = edtAppDate.getText().toString().trim();
            String time = edtAppTime.getText().toString().trim();
            String phone = edtDocPhone.getText().toString().trim();

            if (newName.isEmpty()) {
                edtDocName.setError("Το όνομα είναι υποχρεωτικό");
                return;
            }

            cancelAppointmentAlarm(nameFromSpinner, dateFromSpinner, timeFromSpinner);
            dbHelper.updateAppointment(nameFromSpinner, newName, specialty, date, time, phone);
            setAppointmentAlarm(newName, date, time);
            Toast.makeText(this, "Το ραντεβού ενημερώθηκε επιτυχώς!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(EditAppointmentActivity.this, MainActivity.class));
            finish();
        });

        btnDelete.setOnClickListener(v -> {
            if (nameFromSpinner.isEmpty()) return;

            new AlertDialog.Builder(this)
                    .setTitle("Διαγραφή")
                    .setMessage("Θέλετε σίγουρα να διαγράψετε το ραντεβού με τον γιατρό " + nameFromSpinner + ";")
                    .setPositiveButton("Ναι", (dialog, which) -> {
                        cancelAppointmentAlarm(nameFromSpinner, dateFromSpinner, timeFromSpinner);
                        dbHelper.deleteAppointment(nameFromSpinner);
                        Toast.makeText(this, "Το ραντεβού διαγράφηκε!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(EditAppointmentActivity.this, MainActivity.class));
                        finish();
                    })
                    .setNegativeButton("Όχι", null)
                    .show();
        });

        spnAppointments.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                nameFromSpinner = parent.getItemAtPosition(position).toString();
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT * FROM appointments WHERE doctor_name = ?", new String[]{nameFromSpinner});

                if (cursor != null && cursor.moveToFirst()) {
                    nameFromSpinner = cursor.getString(cursor.getColumnIndexOrThrow("doctor_name"));
                    dateFromSpinner = cursor.getString(cursor.getColumnIndexOrThrow("appointment_date"));
                    timeFromSpinner = cursor.getString(cursor.getColumnIndexOrThrow("appointment_time"));

                    edtDocName.setText(nameFromSpinner);
                    edtSpecialty.setText(cursor.getString(cursor.getColumnIndexOrThrow("specialty")));
                    edtAppDate.setText(dateFromSpinner);
                    edtAppTime.setText(timeFromSpinner);
                    edtDocPhone.setText(cursor.getString(cursor.getColumnIndexOrThrow("doctor_phone")));
                    cursor.close();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                nameFromSpinner = "";
                dateFromSpinner = "";
                timeFromSpinner = "";
            }
        });

        findViewById(R.id.navHome).setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.navEdit).setOnClickListener(v -> {
            String[] options = {"Φάρμακα / Medicines", "Ραντεβού / Appointments"};
            new AlertDialog.Builder(this)
                    .setTitle("Τι θέλετε να επεξεργαστείτε;")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            startActivity(new Intent(this, EditMedicineActivity.class));
                            finish();
                        }
                    })
                    .show();
        });

        findViewById(R.id.navCalendar).setOnClickListener(v -> startActivity(new Intent(this, CalendarActivity.class)));
    }

    @SuppressLint("ScheduleExactAlarm")
    private void setAppointmentAlarm(String docName, String date, String time) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
            java.util.Date appDate = sdf.parse(date + " " + time);
            if (appDate == null) return;

            android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            java.util.Calendar cal2Hours = java.util.Calendar.getInstance();
            cal2Hours.setTime(appDate);
            cal2Hours.add(java.util.Calendar.MINUTE, -120);

            if (!cal2Hours.before(java.util.Calendar.getInstance())) {
                Intent intent1 = new Intent(this, AlarmReceiver.class);
                intent1.putExtra("doctorName", docName);
                intent1.putExtra("appointmentTime", time + " (Σε 2 ώρες)");

                int id2Hours = (docName + date + time + "2h").hashCode();
                android.app.PendingIntent pi1 = android.app.PendingIntent.getBroadcast(this, id2Hours, intent1, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, cal2Hours.getTimeInMillis(), pi1);
            }

            java.util.Calendar calExact = java.util.Calendar.getInstance();
            calExact.setTime(appDate);

            if (!calExact.before(java.util.Calendar.getInstance())) {
                Intent intent2 = new Intent(this, AlarmReceiver.class);
                intent2.putExtra("doctorName", docName);
                intent2.putExtra("appointmentTime", time + " (Τώρα!)");

                int idExact = (docName + date + time + "exact").hashCode();
                android.app.PendingIntent pi2 = android.app.PendingIntent.getBroadcast(this, idExact, intent2, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, calExact.getTimeInMillis(), pi2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cancelAppointmentAlarm(String docName, String date, String time) {
        if (docName.isEmpty() || date.isEmpty() || time.isEmpty()) return;

        android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(this, AlarmReceiver.class);

        int id2Hours = (docName + date + time + "2h").hashCode();
        android.app.PendingIntent pi1 = android.app.PendingIntent.getBroadcast(this, id2Hours, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pi1);

        int idExact = (docName + date + time + "exact").hashCode();
        android.app.PendingIntent pi2 = android.app.PendingIntent.getBroadcast(this, idExact, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pi2);
    }

    private void loadSpinnerData() {
        List<String> appointments = dbHelper.getAllAppointments();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, appointments);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnAppointments.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        updateSettings();
        super.onResume();
    }

    private void updateSettings() {
        SharedPreferences preferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        if (preferences.getBoolean("DarkMode", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    public Context applyFontSize(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        int position = sharedPreferences.getInt("font_size_pos", 1);
        float fontScale = (position == 0) ? 0.85f : (position == 2) ? 1.20f : 1.0f;
        Configuration configuration = context.getResources().getConfiguration();
        configuration.fontScale = fontScale;
        return context.createConfigurationContext(configuration);
    }
}