package com.example.healthassistant;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        if (!notificationsEnabled) {
            return;
        }

        String doctorName = intent.getStringExtra("doctorName");

        if (doctorName != null) {
            String appointmentTime = intent.getStringExtra("appointmentTime");
            int notificationId = (doctorName + appointmentTime).hashCode();

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            int notifStyle = prefs.getInt("notification_style", 2);
            String channelId = "appt_channel_" + notifStyle;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                if (notifStyle == 0) {
                    importance = NotificationManager.IMPORTANCE_LOW;
                }

                NotificationChannel channel = new NotificationChannel(channelId, "Ραντεβού", importance);

                if (notifStyle == 0 || notifStyle == 1) {
                    channel.setSound(null, null);
                }
                notificationManager.createNotificationChannel(channel);
            }

            Intent mainIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle("Υπενθύμιση Ραντεβού")
                    .setContentText("Έχετε ραντεβού με τον/την: " + doctorName + " στις " + appointmentTime)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            if (notifStyle == 0) {
                builder.setPriority(NotificationCompat.PRIORITY_LOW);
                builder.setDefaults(0);
            } else if (notifStyle == 1) {
                builder.setPriority(NotificationCompat.PRIORITY_HIGH);
                builder.setDefaults(0);
            } else {
                builder.setPriority(NotificationCompat.PRIORITY_HIGH);
                builder.setDefaults(NotificationCompat.DEFAULT_ALL);
            }

            notificationManager.notify(notificationId, builder.build());
            return;
        }

        String medicineName = intent.getStringExtra("medicineName");
        if (medicineName != null) {
            String expiryDate = intent.getStringExtra("expiryDate");

            String currentDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
            if (expiryDate != null && currentDate.compareTo(expiryDate) > 0) {
                return;
            }

            Calendar calendar = Calendar.getInstance();
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
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

            DatabaseHelper dbHelper = new DatabaseHelper(context);
            Cursor cursor = dbHelper.getMedicineDetails(medicineName);
            if (cursor != null && cursor.moveToFirst()) {
                int daysIndex = cursor.getColumnIndex("repeat_days");
                String repeatDays = "Καθημερινά";
                if (daysIndex != -1 && !cursor.isNull(daysIndex)) {
                    repeatDays = cursor.getString(daysIndex);
                }
                cursor.close();

                if (repeatDays == null || repeatDays.isEmpty()) {
                    repeatDays = "Καθημερινά";
                }

                if (!repeatDays.equals("Καθημερινά") && !repeatDays.contains(currentDayName)) {
                    return;
                }
            }

            int notificationId = medicineName.hashCode();
            int snoozeCount = intent.getIntExtra("snoozeCount", 0);

            Intent fullScreenIntent = new Intent(context, AlarmActivity.class);
            fullScreenIntent.putExtra("medicineName", medicineName);
            fullScreenIntent.putExtra("expiryDate", expiryDate);
            fullScreenIntent.putExtra("notificationId", notificationId);
            fullScreenIntent.putExtra("snoozeCount", snoozeCount);

            fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);

            context.startActivity(fullScreenIntent);
        }
    }
}