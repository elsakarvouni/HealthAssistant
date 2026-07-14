package com.example.healthassistant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Calendar;

public class CalendarActivity extends AppCompatActivity {

    private ListView lstCalendar;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(applyFontSize(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        updateSettings();
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calendar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        lstCalendar = findViewById(R.id.lstCalendar);

        loadDailySchedule();

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            Intent intent = new Intent(CalendarActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.navEdit).setOnClickListener(v -> {
            String[] options = {"Φάρμακα", "Ραντεβού"};
            new AlertDialog.Builder(CalendarActivity.this)
                    .setTitle("Τι θέλετε να επεξεργαστείτε;")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            startActivity(new Intent(CalendarActivity.this, EditMedicineActivity.class));
                        } else if (which == 1) {
                            startActivity(new Intent(CalendarActivity.this, com.example.healthassistant.EditAppointmentActivity.class));
                        }
                    })
                    .show();
        });

        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(CalendarActivity.this, MainActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.navCalendar).setOnClickListener(v -> {
            Intent intent = new Intent(CalendarActivity.this, CalendarActivity.class);
            startActivity(intent);
        });
    }

    private void loadDailySchedule() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());

        java.util.ArrayList<TimelineItem> scheduleList = new java.util.ArrayList<>();

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

        Cursor medCursor = db.rawQuery("SELECT name, description, hours, repeat_days FROM medicines", null);
        if (medCursor.moveToFirst()) {
            int nameIdx = medCursor.getColumnIndex("name");
            int descIdx = medCursor.getColumnIndex("description");
            int hoursIdx = medCursor.getColumnIndex("hours");
            int daysIdx = medCursor.getColumnIndex("repeat_days");

            do {
                if (nameIdx != -1 && hoursIdx != -1 && descIdx != -1) {

                    String repeatDays = "Καθημερινά";
                    if (daysIdx != -1 && !medCursor.isNull(daysIdx)) {
                        repeatDays = medCursor.getString(daysIdx);
                    }
                    if (repeatDays == null || repeatDays.isEmpty()) {
                        repeatDays = "Καθημερινά";
                    }
                    if (!repeatDays.equals("Καθημερινά") && !repeatDays.contains(currentDayName)) {
                        continue;
                    }

                    String name = medCursor.getString(nameIdx);
                    String description = medCursor.getString(descIdx);
                    String hoursStr = medCursor.getString(hoursIdx);
                    String[] hoursArray = hoursStr.split("[,\\s]+");

                    for (String hour : hoursArray) {
                        if (!hour.trim().isEmpty()) {
                            scheduleList.add(new TimelineItem(hour.trim(), name, description, ""));
                        }
                    }
                }
            } while (medCursor.moveToNext());
        }
        medCursor.close();


        Cursor appCursor = db.rawQuery(
                "SELECT doctor_name, specialty, appointment_time, doctor_phone FROM appointments WHERE appointment_date = ?",
                new String[]{todayDate}
        );
        if (appCursor.moveToFirst()) {
            do {
                String docName = appCursor.getString(0);
                String specialty = appCursor.getString(1);
                String time = appCursor.getString(2);
                String phone = appCursor.getString(3);

                scheduleList.add(new TimelineItem(time, docName, specialty, phone));
            } while (appCursor.moveToNext());
        }
        appCursor.close();


        java.util.Collections.sort(scheduleList, (item1, item2) -> item1.time.compareTo(item2.time));


        List<MedicineOccurrence> adapterItems = new ArrayList<>();
        for (TimelineItem item : scheduleList) {
            adapterItems.add(new MedicineOccurrence(item.time, item.text, item.notes, item.description));
        }

        if (lstCalendar != null) {
            CalendarAdapter adapter = new CalendarAdapter(this, adapterItems);
            lstCalendar.setAdapter(adapter);
        }
    }

    private class CalendarAdapter extends ArrayAdapter<MedicineOccurrence> {
        public CalendarAdapter(Context context, List<MedicineOccurrence> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.calendar_item, parent, false);
            }

            MedicineOccurrence item = getItem(position);

            TextView txtMain = convertView.findViewById(R.id.txtTimeNameNotes);
            TextView txtDesc = convertView.findViewById(R.id.txtDescription);

            String mainText = item.time + " - " + item.name;
            if (item.notes != null && !item.notes.isEmpty()) {
                mainText += " (" + item.notes + ")";
            }

            if (txtMain != null) {
                txtMain.setText(mainText);
                txtMain.setTextColor(android.graphics.Color.parseColor("#5E35B1"));
            }
            if (txtDesc != null) {
                txtDesc.setText(item.description);
                txtDesc.setTextColor(android.graphics.Color.parseColor("#5E35B1"));
            }

            return convertView;
        }
    }

    class MedicineOccurrence {
        String time, name, notes, description;

        MedicineOccurrence(String time, String name, String notes, String description) {
            this.time = time;
            this.name = name;
            this.notes = notes;
            this.description = description;
        }
    }

    class TimelineItem {
        String time;
        String text;
        String description;
        String notes;

        TimelineItem(String time, String text, String description, String notes) {
            this.time = time;
            this.text = text;
            this.description = description;
            this.notes = notes;
        }
    }

    @Override
    protected void onResume() {
        updateSettings();
        super.onResume();
        loadDailySchedule();
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

        float fontScale = (position == 0) ? 0.85f : (position == 2) ? 1.20f : 1.0f;
        Configuration configuration = context.getResources().getConfiguration();
        configuration.fontScale = fontScale;
        return context.createConfigurationContext(configuration);
    }
}