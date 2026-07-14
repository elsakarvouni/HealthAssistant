package com.example.healthassistant;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.app.AlarmManager;
import android.app.PendingIntent;
import com.example.healthassistant.R;

import java.util.Calendar;

public class AddMedicineActivity extends AppCompatActivity {
    EditText edtName, edtDesc, edtStartDate, edtDate;
    Spinner spnMedType;
    EditText edtInventory, edtDose;
    LinearLayout containerHours;
    Button btnSave;
    DatabaseHelper dbHelper;

    CheckBox chkMon, chkTue, chkWed, chkThu, chkFri, chkSat, chkSun, chkSelectAll;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(applyFontSize(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        updateSettings();
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_medicine);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);

        edtName = findViewById(R.id.edtName);
        edtDesc = findViewById(R.id.edtDescription);
        edtStartDate = findViewById(R.id.edtStartDate);
        edtDate = findViewById(R.id.edtExpiryDate);
        containerHours = findViewById(R.id.containerHours);
        btnSave = findViewById(R.id.btnSave);
        spnMedType = findViewById(R.id.spnMedType);
        edtInventory = findViewById(R.id.edtInventory);
        edtDose = findViewById(R.id.edtDose);

        chkMon = findViewById(R.id.chkMon);
        chkTue = findViewById(R.id.chkTue);
        chkWed = findViewById(R.id.chkWed);
        chkThu = findViewById(R.id.chkThu);
        chkFri = findViewById(R.id.chkFri);
        chkSat = findViewById(R.id.chkSat);
        chkSun = findViewById(R.id.chkSun);
        chkSelectAll = findViewById(R.id.chkSelectAll);

        chkSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            chkMon.setChecked(isChecked);
            chkTue.setChecked(isChecked);
            chkWed.setChecked(isChecked);
            chkThu.setChecked(isChecked);
            chkFri.setChecked(isChecked);
            chkSat.setChecked(isChecked);
            chkSun.setChecked(isChecked);
        });

        String[] types = {"Χάπια (τεμάχια)", "Σιρόπι (ml)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spnMedType.setAdapter(adapter);

        if (containerHours != null && containerHours.getChildCount() > 1) {
            View firstRow = containerHours.getChildAt(1);
            if (firstRow instanceof LinearLayout) {
                EditText firstEditText = firstRow.findViewWithTag("time_edit_text");
                ImageButton firstAddButton = firstRow.findViewWithTag("btn_add_hour");

                if (firstEditText != null) {
                    firstEditText.setOnClickListener(v -> showTimePicker(firstEditText));
                }
                if (firstAddButton != null) {
                    firstAddButton.setOnClickListener(v -> addNewHourRow());
                }
            }
        }

        edtStartDate.setFocusable(false);
        edtStartDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
                String selectedDate = year1 + "-" + String.format("%02d", (month1 + 1)) + "-" + String.format("%02d", dayOfMonth);
                edtStartDate.setText(selectedDate);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        edtDate.setFocusable(false);
        edtDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
                String selectedDate = year1 + "-" + String.format("%02d", (month1 + 1)) + "-" + String.format("%02d", dayOfMonth);
                edtDate.setText(selectedDate);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        btnSave.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String startDate = edtStartDate.getText().toString().trim();
            String date = edtDate.getText().toString().trim();
            String desc = edtDesc.getText().toString().trim();
            String type = spnMedType.getSelectedItem().toString();
            String invStr = edtInventory.getText().toString().trim();
            String doseStr = edtDose.getText().toString().trim();

            if (name.isEmpty()) {
                edtName.setError("Το όνομα είναι υποχρεωτικό / Name is obligatory");
                edtName.requestFocus();
                return;
            }

            if (date.isEmpty()) {
                Toast.makeText(this, R.string.expdate_inp_error, Toast.LENGTH_SHORT).show();
                return;
            }

            double inventory = invStr.isEmpty() ? 0 : Double.parseDouble(invStr);
            double dose = doseStr.isEmpty() ? 0 : Double.parseDouble(doseStr);

            StringBuilder daysBuilder = new StringBuilder();
            if (chkMon.isChecked()) daysBuilder.append("Δευ,");
            if (chkTue.isChecked()) daysBuilder.append("Τρι,");
            if (chkWed.isChecked()) daysBuilder.append("Τετ,");
            if (chkThu.isChecked()) daysBuilder.append("Πεμ,");
            if (chkFri.isChecked()) daysBuilder.append("Παρ,");
            if (chkSat.isChecked()) daysBuilder.append("Σαβ,");
            if (chkSun.isChecked()) daysBuilder.append("Κυρ,");

            String repeatDays = daysBuilder.toString();
            if (repeatDays.endsWith(",")) {
                repeatDays = repeatDays.substring(0, repeatDays.length() - 1);
            }

            if (repeatDays.isEmpty()) {
                repeatDays = "Καθημερινά";
            }

            StringBuilder hoursBuilder = new StringBuilder();
            for (int i = 0; i < containerHours.getChildCount(); i++) {
                View row = containerHours.getChildAt(i);
                if (row instanceof LinearLayout) {
                    EditText rowEditText = row.findViewWithTag("time_edit_text");
                    if (rowEditText != null) {
                        String time = rowEditText.getText().toString().trim();
                        if (!time.isEmpty()) {
                            if (hoursBuilder.length() > 0) {
                                hoursBuilder.append(", ");
                            }
                            hoursBuilder.append(time);
                        }
                    }
                }
            }
            String hours = hoursBuilder.toString().trim();

            if (hours.isEmpty()) {
                Toast.makeText(this, R.string.hour_inp_error, Toast.LENGTH_SHORT).show();
                return;
            }

            dbHelper.addMedicine(name, desc, startDate, date, hours, "", type, inventory, dose, repeatDays);

            String[] hoursArray = hours.split("[,\\s]+");
            for (String hourPart : hoursArray) {
                try {
                    String[] timeParts = hourPart.trim().split(":");
                    int h = Integer.parseInt(timeParts[0]);
                    int m = Integer.parseInt(timeParts[1]);

                    setAlarm(name, h, m, date);
                } catch (Exception e) {
                    Log.e("ParsingError", "Wrong hour format: " + hourPart);
                }
            }

            Toast.makeText(this, R.string.med_set, Toast.LENGTH_SHORT).show();
            finish();
        });

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            Intent intent = new Intent(AddMedicineActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.navEdit).setOnClickListener(v -> {
            String[] options = {"Φάρμακα / Medicines", "Ραντεβού / Appointments"};
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Τι θέλετε να επεξεργαστείτε;")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            startActivity(new Intent(this, EditMedicineActivity.class));
                            finish();
                        } else if (which == 1) {
                            startActivity(new Intent(this, EditAppointmentActivity.class));
                            finish();
                        }
                    })
                    .show();
        });

        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(AddMedicineActivity.this, MainActivity.class));
            finish();
        });

        findViewById(R.id.navCalendar).setOnClickListener(v -> {
            startActivity(new Intent(AddMedicineActivity.this, CalendarActivity.class));
            finish();
        });
    }

    private void showTimePicker(EditText editText) {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(this,
                (view, hourOfDay, minuteOfHour) -> {
                    String selectedTime = String.format("%02d:%02d", hourOfDay, minuteOfHour);
                    editText.setText(selectedTime);
                }, hour, minute, true);
        timePickerDialog.show();
    }

    private void addNewHourRow() {
        LinearLayout newRow = new LinearLayout(this);
        newRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        newRow.setOrientation(LinearLayout.HORIZONTAL);
        newRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) newRow.getLayoutParams();
        params.bottomMargin = (int) (4 * getResources().getDisplayMetrics().density);
        newRow.setLayoutParams(params);

        EditText newEditText = new EditText(this);
        newEditText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        newEditText.setHint(R.string.hours);
        newEditText.setFocusable(false);
        newEditText.setClickable(true);
        newEditText.setTag("time_edit_text");
        newEditText.setOnClickListener(v -> showTimePicker(newEditText));

        ImageButton removeButton = new ImageButton(this);
        removeButton.setLayoutParams(new LinearLayout.LayoutParams(
                (int) (48 * getResources().getDisplayMetrics().density),
                (int) (48 * getResources().getDisplayMetrics().density)));
        removeButton.setImageResource(android.R.drawable.ic_delete);

        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        removeButton.setBackgroundResource(outValue.resourceId);

        removeButton.setOnClickListener(v -> containerHours.removeView(newRow));

        newRow.addView(newEditText);
        newRow.addView(removeButton);
        containerHours.addView(newRow);

        showTimePicker(newEditText);
    }

    @Override
    protected void onResume() {
        updateSettings();
        super.onResume();
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

    private void setAlarm(String name, int h, int m, String expiryDate) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intentPermission = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intentPermission);
                return;
            }
        }

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("medicineName", name);
        intent.putExtra("expiryDate", expiryDate);

        int id = (name + h + m).hashCode();

        PendingIntent pi = PendingIntent.getBroadcast(this, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, h);
        cal.set(Calendar.MINUTE, m);
        cal.set(Calendar.SECOND, 0);

        if (cal.before(Calendar.getInstance())) {
            cal.add(Calendar.DATE, 1);
        }

        try {
            if (alarmManager != null) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }
        } catch (SecurityException e) {
            Log.e("ALARM_ERROR", "SecurityException: " + e.getMessage());
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