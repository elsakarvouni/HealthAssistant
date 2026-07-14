package com.example.healthassistant;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "HealthDB";
    private static final int DATABASE_VERSION = 5;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE_MEDICINES = "CREATE TABLE medicines (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "description TEXT," +
                "start_date TEXT," +
                "expiry_date TEXT," +
                "hours TEXT," +
                "notes TEXT," +
                "medicine_type TEXT," +
                "inventory REAL," +
                "dose REAL," +
                "repeat_days TEXT)";
        db.execSQL(CREATE_TABLE_MEDICINES);

        String CREATE_TABLE_APPOINTMENTS = "CREATE TABLE appointments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "doctor_name TEXT," +
                "specialty TEXT," +
                "appointment_date TEXT," +
                "appointment_time TEXT," +
                "doctor_phone TEXT)";
        db.execSQL(CREATE_TABLE_APPOINTMENTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL("DROP TABLE IF EXISTS appointments");
            String CREATE_TABLE_APPOINTMENTS = "CREATE TABLE appointments (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "doctor_name TEXT," +
                    "specialty TEXT," +
                    "appointment_date TEXT," +
                    "appointment_time TEXT," +
                    "doctor_phone TEXT)";
            db.execSQL(CREATE_TABLE_APPOINTMENTS);
        }

        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE medicines ADD COLUMN medicine_type TEXT DEFAULT 'Χάπια'");
            db.execSQL("ALTER TABLE medicines ADD COLUMN inventory REAL DEFAULT 0");
            db.execSQL("ALTER TABLE medicines ADD COLUMN dose REAL DEFAULT 0");
        }

        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE medicines ADD COLUMN repeat_days TEXT DEFAULT 'Καθημερινά'");
        }
    }


    public void addMedicine(String name, String desc, String startDate, String date, String hours, String notes, String type, double inventory, double dose, String repeatDays) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("description", desc);
        values.put("start_date", startDate);
        values.put("expiry_date", date);
        values.put("hours", hours);
        values.put("notes", notes);
        values.put("medicine_type", type);
        values.put("inventory", inventory);
        values.put("dose", dose);
        values.put("repeat_days", repeatDays);

        db.insert("medicines", null, values);
        db.close();
    }

    public List<String> getAllMedicines() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM medicines", null);

        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public void deleteMedicine(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("medicines", "name = ?", new String[]{name});
        db.close();
    }

    public Cursor getMedicineDetails(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM medicines WHERE name = ?", new String[]{name});
    }

    public void updateMedicine(String oldName, String newName, String desc, String startDate, String date, String hours, String notes, String type, double inventory, double dose, String repeatDays) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", newName);
        values.put("description", desc);
        values.put("start_date", startDate);
        values.put("expiry_date", date);
        values.put("hours", hours);
        values.put("notes", notes);
        values.put("medicine_type", type);
        values.put("inventory", inventory);
        values.put("dose", dose);
        values.put("repeat_days", repeatDays);

        db.update("medicines", values, "name = ?", new String[]{oldName});
        db.close();
    }
    public void deleteExpiredMedicines() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("medicines", "expiry_date < date('now')", null);
        db.close();
    }

    public double takeDose(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT inventory, dose FROM medicines WHERE name = ?", new String[]{name});

        double remainingDoses = -1;

        if (cursor.moveToFirst()) {
            int invIndex = cursor.getColumnIndex("inventory");
            int doseIndex = cursor.getColumnIndex("dose");

            if (invIndex != -1 && doseIndex != -1) {
                double currentInventory = cursor.getDouble(invIndex);
                double dose = cursor.getDouble(doseIndex);

                if (dose > 0 && currentInventory >= dose) {
                    currentInventory -= dose;

                    ContentValues values = new ContentValues();
                    values.put("inventory", currentInventory);
                    db.update("medicines", values, "name = ?", new String[]{name});

                    remainingDoses = currentInventory / dose;
                } else if (currentInventory < dose && currentInventory > 0) {
                    currentInventory = 0;
                    ContentValues values = new ContentValues();
                    values.put("inventory", currentInventory);
                    db.update("medicines", values, "name = ?", new String[]{name});
                    remainingDoses = 0;
                } else {
                    remainingDoses = currentInventory > 0 ? (currentInventory / dose) : 0;
                }
            }
        }
        cursor.close();
        db.close();

        return remainingDoses;
    }

    public void addAppointment(String doctorName, String specialty, String date, String time, String phone) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("doctor_name", doctorName);
        values.put("specialty", specialty);
        values.put("appointment_date", date);
        values.put("appointment_time", time);
        values.put("doctor_phone", phone);

        db.insert("appointments", null, values);
        db.close();
    }

    public List<String> getAllAppointments() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT doctor_name FROM appointments", null);

        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public void deleteAppointment(String doctorName) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("appointments", "doctor_name = ?", new String[]{doctorName});
        db.close();
    }

    public void updateAppointment(String oldDoctorName, String newDoctorName, String specialty, String date, String time, String phone) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("doctor_name", newDoctorName);
        values.put("specialty", specialty);
        values.put("appointment_date", date);
        values.put("appointment_time", time);
        values.put("doctor_phone", phone);

        db.update("appointments", values, "doctor_name = ?", new String[]{oldDoctorName});
        db.close();
    }
}