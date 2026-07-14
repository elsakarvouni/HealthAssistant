package com.example.healthassistant;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.app.DatePickerDialog;

import com.example.healthassistant.R;

import java.util.Calendar;
import java.util.List;

public class EditMedicineActivity extends AppCompatActivity {
    Spinner spnMedicines, spnMedType;
    EditText edtName, edtDesc, edtStartDate, edtDate, edtInventory, edtDose;
    LinearLayout containerHours;
    Button btnUpdate, btnDelete;
    DatabaseHelper dbHelper;
    private String nameFromSpinner = "";
    private ArrayAdapter<String> typeAdapter;

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
        setContentView(R.layout.activity_edit_medicine);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);
        spnMedicines = findViewById(R.id.spnMedicines);
        edtName = findViewById(R.id.editName);
        edtDesc = findViewById(R.id.editDesc);
        edtStartDate = findViewById(R.id.editStartDate);
        edtDate = findViewById(R.id.editDate);
        containerHours = findViewById(R.id.containerHours);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnDelete = findViewById(R.id.btnDelete);
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

        if (chkSelectAll != null) {
            chkSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (chkMon != null) chkMon.setChecked(isChecked);
                if (chkTue != null) chkTue.setChecked(isChecked);
                if (chkWed != null) chkWed.setChecked(isChecked);
                if (chkThu != null) chkThu.setChecked(isChecked);
                if (chkFri != null) chkFri.setChecked(isChecked);
                if (chkSat != null) chkSat.setChecked(isChecked);
                if (chkSun != null) chkSun.setChecked(isChecked);
            });
        }

        if (spnMedType != null) {
            String[] types = {"Χάπια (τεμάχια)", "Σιρόπι (ml)"};
            typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
            spnMedType.setAdapter(typeAdapter);
        }

        loadSpinnerData();

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

        btnUpdate.setOnClickListener(v -> {
            String fname = edtName.getText().toString().trim();
            String fstartDate = edtStartDate.getText().toString().trim();
            String fdate = edtDate.getText().toString().trim();
            String fdesc = edtDesc.getText().toString().trim();

            String type = spnMedType != null ? spnMedType.getSelectedItem().toString() : "Χάπια (τεμάχια)";
            String invStr = (edtInventory != null && edtInventory.getText() != null) ? edtInventory.getText().toString().trim() : "0";
            String doseStr = (edtDose != null && edtDose.getText() != null) ? edtDose.getText().toString().trim() : "0";

            if (fname.isEmpty()) {
                edtName.setError("Το όνομα είναι υποχρεωτικό");
                edtName.requestFocus();
                return;
            }
            if (fdate.isEmpty()) {
                Toast.makeText(this, R.string.expdate_inp_error, Toast.LENGTH_SHORT).show();
                return;
            }

            double inventory = invStr.isEmpty() ? 0 : Double.parseDouble(invStr);
            double dose = doseStr.isEmpty() ? 0 : Double.parseDouble(doseStr);

            StringBuilder daysBuilder = new StringBuilder();
            if (chkMon != null && chkMon.isChecked()) daysBuilder.append("Δευ,");
            if (chkTue != null && chkTue.isChecked()) daysBuilder.append("Τρι,");
            if (chkWed != null && chkWed.isChecked()) daysBuilder.append("Τετ,");
            if (chkThu != null && chkThu.isChecked()) daysBuilder.append("Πεμ,");
            if (chkFri != null && chkFri.isChecked()) daysBuilder.append("Παρ,");
            if (chkSat != null && chkSat.isChecked()) daysBuilder.append("Σαβ,");
            if (chkSun != null && chkSun.isChecked()) daysBuilder.append("Κυρ,");

            String fRepeatDays = daysBuilder.toString();
            if (fRepeatDays.endsWith(",")) {
                fRepeatDays = fRepeatDays.substring(0, fRepeatDays.length() - 1);
            }
            if (fRepeatDays.isEmpty()) {
                fRepeatDays = "Καθημερινά";
            }

            StringBuilder hoursBuilder = new StringBuilder();
            for (int i = 0; i < containerHours.getChildCount(); i++) {
                View row = containerHours.getChildAt(i);
                if (row instanceof LinearLayout) {
                    EditText rowEditText = row.findViewWithTag("time_edit_text");
                    if (rowEditText != null) {
                        String time = rowEditText.getText().toString().trim();
                        if (!time.isEmpty()) {
                            if (hoursBuilder.length() > 0) hoursBuilder.append(", ");
                            hoursBuilder.append(time);
                        }
                    }
                }
            }
            String fhours = hoursBuilder.toString().trim();

            if (fhours.isEmpty()) {
                Toast.makeText(this, R.string.hour_inp_error, Toast.LENGTH_SHORT).show();
                return;
            }

            Cursor oldCursor = dbHelper.getMedicineDetails(nameFromSpinner);
            if (oldCursor != null && oldCursor.moveToFirst()) {
                String oldHours = oldCursor.getString(oldCursor.getColumnIndexOrThrow("hours"));
                String[] oldHoursArray = oldHours.split("[,\\s]+");
                for (String h : oldHoursArray) {
                    String[] parts = h.split(":");
                    if (parts.length == 2) {
                        cancelAlarm(nameFromSpinner, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                    }
                }
                oldCursor.close();
            }
            dbHelper.updateMedicine(nameFromSpinner, fname, fdesc, fstartDate, fdate, fhours, "", type, inventory, dose, fRepeatDays);

            String[] newHoursArray = fhours.split("[,\\s]+");
            for (String h : newHoursArray) {
                String[] parts = h.split(":");
                if (parts.length == 2) {
                    setAlarm(fname, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), fdate);
                }
            }

            Toast.makeText(this, R.string.update_success, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(EditMedicineActivity.this, MainActivity.class));
            finish();
        });


        btnDelete.setOnClickListener(v -> {
            String targetName = (nameFromSpinner != null && !nameFromSpinner.isEmpty()) ? nameFromSpinner : edtName.getText().toString().trim();
            if (targetName.isEmpty()) {
                Toast.makeText(this, R.string.name_inp_error, Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle(R.string.delete)
                    .setMessage(getString(R.string.delete_confirm) + " " + targetName + getString(R.string.questionmark))
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        Cursor cursor = dbHelper.getMedicineDetails(targetName);
                        if (cursor != null && cursor.moveToFirst()) {
                            String hours = cursor.getString(cursor.getColumnIndexOrThrow("hours"));
                            String[] hoursArray = hours.split("[,\\s]+");
                            for (String h : hoursArray) {
                                String[] parts = h.split(":");
                                if (parts.length == 2) cancelAlarm(targetName, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                            }
                            cursor.close();

                            dbHelper.deleteMedicine(targetName);
                            Toast.makeText(this, R.string.med_deleted, Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(EditMedicineActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, R.string.med_not_found, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        });

        spnMedicines.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                nameFromSpinner = parent.getItemAtPosition(position).toString();
                Cursor cursor = dbHelper.getMedicineDetails(nameFromSpinner);

                if (cursor != null && cursor.moveToFirst()) {
                    edtName.setText(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                    edtDesc.setText(cursor.getString(cursor.getColumnIndexOrThrow("description")));
                    edtStartDate.setText(cursor.getString(cursor.getColumnIndexOrThrow("start_date")));
                    edtDate.setText(cursor.getString(cursor.getColumnIndexOrThrow("expiry_date")));

                    String mType = cursor.getString(cursor.getColumnIndexOrThrow("medicine_type"));
                    double inv = cursor.getDouble(cursor.getColumnIndexOrThrow("inventory"));
                    double dose = cursor.getDouble(cursor.getColumnIndexOrThrow("dose"));

                    int daysColumnIndex = cursor.getColumnIndex("repeat_days");
                    String savedDays = "Καθημερινά";
                    if (daysColumnIndex != -1 && !cursor.isNull(daysColumnIndex)) {
                        savedDays = cursor.getString(daysColumnIndex);
                    }

                    if (savedDays == null || savedDays.isEmpty()) {
                        savedDays = "Καθημερινά";
                    }

                    if (chkMon != null) chkMon.setChecked(savedDays.contains("Δευ") || savedDays.equals("Καθημερινά"));
                    if (chkTue != null) chkTue.setChecked(savedDays.contains("Τρι") || savedDays.equals("Καθημερινά"));
                    if (chkWed != null) chkWed.setChecked(savedDays.contains("Τετ") || savedDays.equals("Καθημερινά"));
                    if (chkThu != null) chkThu.setChecked(savedDays.contains("Πεμ") || savedDays.equals("Καθημερινά"));
                    if (chkFri != null) chkFri.setChecked(savedDays.contains("Παρ") || savedDays.equals("Καθημερινά"));
                    if (chkSat != null) chkSat.setChecked(savedDays.contains("Σαβ") || savedDays.equals("Καθημερινά"));
                    if (chkSun != null) chkSun.setChecked(savedDays.contains("Κυρ") || savedDays.equals("Καθημερινά"));

                    if (chkSelectAll != null) {
                        chkSelectAll.setChecked(savedDays.equals("Καθημερινά") ||
                                (savedDays.contains("Δευ") && savedDays.contains("Τρι") && savedDays.contains("Τετ") &&
                                        savedDays.contains("Πεμ") && savedDays.contains("Παρ") && savedDays.contains("Σαβ") && savedDays.contains("Κυρ")));
                    }

                    if (spnMedType != null && mType != null && typeAdapter != null) {
                        int pos = typeAdapter.getPosition(mType);
                        spnMedType.setSelection(Math.max(pos, 0));
                    }

                    if (edtInventory != null) edtInventory.setText(inv == (long) inv ? String.format("%d", (long) inv) : String.format("%s", inv));
                    if (edtDose != null) edtDose.setText(dose == (long) dose ? String.format("%d", (long) dose) : String.format("%s", dose));

                    String hoursStr = cursor.getString(cursor.getColumnIndexOrThrow("hours"));
                    populateHoursContainer(hoursStr);

                    cursor.close();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                nameFromSpinner = "";
            }
        });

        findViewById(R.id.btnSettings).setOnClickListener(v -> startActivity(new Intent(EditMedicineActivity.this, SettingsActivity.class)));
        findViewById(R.id.navHome).setOnClickListener(v -> startActivity(new Intent(EditMedicineActivity.this, MainActivity.class)));
        findViewById(R.id.navCalendar).setOnClickListener(v -> startActivity(new Intent(EditMedicineActivity.this, CalendarActivity.class)));

        findViewById(R.id.navEdit).setOnClickListener(v -> {
            String[] options = {"Φάρμακα / Medicines", "Ραντεβού / Appointments"};
            new AlertDialog.Builder(this)
                    .setTitle("Τι θέλετε να επεξεργαστείτε;")
                    .setItems(options, (dialog, which) -> {
                        if (which == 1) {
                            startActivity(new Intent(this, EditAppointmentActivity.class));
                            finish();
                        }
                    })
                    .show();
        });
    }

    private void loadSpinnerData() {
        List<String> medicines = dbHelper.getAllMedicines();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, medicines);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnMedicines.setAdapter(adapter);
    }

    private void populateHoursContainer(String hoursStr) {
        while (containerHours.getChildCount() > 1) {
            containerHours.removeViewAt(1);
        }
        String[] hoursArray = hoursStr.split("[,\\s]+");
        for (String hourText : hoursArray) {
            String time = hourText.trim();
            if (!time.isEmpty()) addHourRow(time);
        }
        addFooterAddButton();
    }

    private void addHourRow(String timeValue) {
        LinearLayout newRow = new LinearLayout(this);
        newRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
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
        newEditText.setText(timeValue);
        newEditText.setOnClickListener(v -> showTimePicker(newEditText));

        ImageButton removeButton = new ImageButton(this);
        removeButton.setLayoutParams(new LinearLayout.LayoutParams((int) (48 * getResources().getDisplayMetrics().density), (int) (48 * getResources().getDisplayMetrics().density)));
        removeButton.setImageResource(android.R.drawable.ic_delete);
        removeButton.setOnClickListener(v -> containerHours.removeView(newRow));

        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        removeButton.setBackgroundResource(outValue.resourceId);

        newRow.addView(newEditText);
        newRow.addView(removeButton);

        containerHours.addView(newRow, containerHours.getChildCount() - (hasFooterButton() ? 1 : 0));
    }

    private void addFooterAddButton() {
        ImageButton addButton = new ImageButton(this);
        addButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (40 * getResources().getDisplayMetrics().density)));
        addButton.setImageResource(android.R.drawable.ic_input_add);
        addButton.setTag("footer_add_button");
        addButton.setOnClickListener(v -> {
            addHourRow("");
            View lastRow = containerHours.getChildAt(containerHours.getChildCount() - 2);
            if (lastRow instanceof LinearLayout) {
                EditText et = lastRow.findViewWithTag("time_edit_text");
                if (et != null) showTimePicker(et);
            }
        });

        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        addButton.setBackgroundResource(outValue.resourceId);

        containerHours.addView(addButton);
    }

    private boolean hasFooterButton() {
        if (containerHours.getChildCount() > 1) {
            View lastView = containerHours.getChildAt(containerHours.getChildCount() - 1);
            return "footer_add_button".equals(lastView.getTag());
        }
        return false;
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
        PendingIntent pi = PendingIntent.getBroadcast(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
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

    private void cancelAlarm(String medicineName, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        int uniqueId = (medicineName + hour + minute).hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, uniqueId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
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