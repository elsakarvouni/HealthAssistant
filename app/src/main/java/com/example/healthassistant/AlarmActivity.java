package com.example.healthassistant;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class AlarmActivity extends AppCompatActivity {
    private Ringtone ringtone;

    @SuppressLint("ScheduleExactAlarm")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        setContentView(R.layout.activity_alarm);

        String medicineName = getIntent().getStringExtra("medicineName");
        String expiryDate = getIntent().getStringExtra("expiryDate");
        int notificationId = getIntent().getIntExtra("notificationId", 1);

        int snoozeCount = getIntent().getIntExtra("snoozeCount", 0);

        TextView txtName = findViewById(R.id.txtAlarmMedicineName);
        txtName.setText(medicineName);

        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), alarmUri);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone.setLooping(true);
        }
        ringtone.play();

        Button btnTookIt = findViewById(R.id.btnTookIt);
        Button btnSnooze = findViewById(R.id.btnSnooze);

        btnTookIt.setOnClickListener(v -> {
            if (ringtone != null && ringtone.isPlaying()) {
                ringtone.stop();
            }

            DatabaseHelper dbHelper = new DatabaseHelper(AlarmActivity.this);
            double remainingDoses = dbHelper.takeDose(medicineName);

            if (remainingDoses >= 0 && remainingDoses <= 2.0) {
                String message;
                if (remainingDoses == 0) {
                    message = "Το φάρμακο " + medicineName + " μόλις τελείωσε!\nΠρέπει να το ανανεώσετε άμεσα.";
                } else {
                    message = "Το φάρμακο " + medicineName + " κοντεύει να τελειώσει!\nΑπομένουν μόνο " + remainingDoses + " δόσεις. Μην ξεχάσετε να πάτε στο φαρμακείο.";
                }

                new AlertDialog.Builder(AlarmActivity.this)
                        .setTitle("⚠️ Χαμηλό Απόθεμα")
                        .setMessage(message)
                        .setPositiveButton("Εντάξει", (dialog, which) -> {
                            stopAlarmAndFinish(notificationId);
                        })
                        .setCancelable(false)
                        .show();
            } else {
                stopAlarmAndFinish(notificationId);
            }
        });


        btnSnooze.setOnClickListener(v -> {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent snoozeIntent = new Intent(this, AlarmReceiver.class);
            snoozeIntent.putExtra("medicineName", medicineName);
            snoozeIntent.putExtra("expiryDate", expiryDate);

            SharedPreferences preferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
            int snoozePos = preferences.getInt("snooze_position", 0);
            String emergencyPhone = preferences.getString("emergency_phone", "");

            int minutesForThisSnooze;
            switch (snoozePos) {
                case 1: minutesForThisSnooze = 10; break;
                case 2: minutesForThisSnooze = 15; break;
                case 3: minutesForThisSnooze = 30; break;
                case 0:
                default: minutesForThisSnooze = 5; break;
            }

            int newSnoozeCount = snoozeCount + 1;

            snoozeIntent.putExtra("snoozeCount", newSnoozeCount);

            PendingIntent pi = PendingIntent.getBroadcast(this, notificationId, snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


            if (newSnoozeCount >= 2) {
                String targetPhone = emergencyPhone.isEmpty() ? "της επαφής σας" : emergencyPhone;

                new AlertDialog.Builder(this)
                        .setTitle("⚠️ Προειδοποίηση Ασφαλείας")
                        .setMessage("Πατήσατε αναβολή " + newSnoozeCount + " φορές!\n\nΣτάλθηκε αυτόματη ειδοποίηση SMS στο τηλέφωνο ανάγκης: " + targetPhone)
                        .setPositiveButton("OK", (dialog, which) -> {
                            Calendar cal = Calendar.getInstance();
                            cal.add(Calendar.MINUTE, minutesForThisSnooze);
                            if (alarmManager != null) {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                            }
                            stopAlarmAndFinish(notificationId);
                        })
                        .setCancelable(false)
                        .show();
            } else {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MINUTE, minutesForThisSnooze);
                if (alarmManager != null) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                }
                stopAlarmAndFinish(notificationId);
            }
        });
    }

    private void stopAlarmAndFinish(int notificationId) {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(notificationId);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        super.onDestroy();
    }
}