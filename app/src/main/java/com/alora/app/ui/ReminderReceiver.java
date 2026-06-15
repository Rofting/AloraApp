package com.alora.app.ui;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.alora.app.R;

import java.util.Locale;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "alora_reminders_channel";
    private TextToSpeech tts;

    @Override
    public void onReceive(Context context, Intent intent) {
        String titulo = intent.getStringExtra("EXTRA_TITULO");
        if (titulo == null) titulo = intent.getStringExtra("TITULO_RECORDATORIO");
        if (titulo == null) titulo = context.getString(R.string.alarm_default_title);

        Long idPaciente = intent.getLongExtra("EXTRA_ID", -1L);
        Long idRecordatorio = intent.getLongExtra("EXTRA_RECORDATORIO_ID", -1L);

        Log.d("AloraAlarm", "Alarma recibida: " + titulo);

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                    "Alora:WakeLockAlarma"
            );
            wakeLock.acquire(5000);
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, context.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent interactivoIntent = new Intent(context, AssistantActivity.class);
        interactivoIntent.putExtra("EXTRA_ID", idPaciente);
        interactivoIntent.putExtra("EXTRA_RECORDATORIO_ID", idRecordatorio);
        interactivoIntent.putExtra("EXTRA_RECORDATORIO_TITULO", titulo);
        interactivoIntent.putExtra("EXTRA_RECORDATORIO_DIAS", intent.getStringExtra("EXTRA_DIAS"));
        interactivoIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                interactivoIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_modern_alarm)
                .setContentTitle(context.getString(R.string.alarm_notification_title))
                .setContentText(titulo)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }

        hablarRecordatorio(context, titulo);

        // Reprogramar la próxima ocurrencia (las alarmas exactas son de un solo disparo)
        String hora = intent.getStringExtra("EXTRA_HORA");
        String dias = intent.getStringExtra("EXTRA_DIAS");
        if (hora != null && idRecordatorio != -1L) {
            com.alora.app.util.AlarmHelper.programarAlarma(
                    context, idRecordatorio, idPaciente, titulo, hora,
                    dias != null ? dias : "TODOS");
        }
    }

    private void hablarRecordatorio(Context context, String titulo) {
        final String textoAHablar = context.getString(R.string.tts_reminder_prefix, titulo);
        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int langResult = tts.setLanguage(Locale.getDefault().getLanguage().equals("en") ? Locale.US : new Locale("es", "ES"));
                if (langResult != TextToSpeech.LANG_MISSING_DATA && langResult != TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.speak(textoAHablar, TextToSpeech.QUEUE_FLUSH, null, "ReminderTTS");
                }
            }
        });
    }
}
