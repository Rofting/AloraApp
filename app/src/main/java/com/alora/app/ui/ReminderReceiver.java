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
        if (titulo == null) titulo = "¡Es hora de tu pauta médica!";

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
                    CHANNEL_ID, "Recordatorios Médicos Alora", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent interactivoIntent = new Intent(context, AssistantActivity.class);
        interactivoIntent.putExtra("EXTRA_ID", idPaciente);
        interactivoIntent.putExtra("EXTRA_RECORDATORIO_ID", idRecordatorio);
        interactivoIntent.putExtra("EXTRA_RECORDATORIO_TITULO", titulo);
        interactivoIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                interactivoIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Alora Cuidado:")
                .setContentText(titulo)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }

        hablarRecordatorio(context, titulo);
    }

    private void hablarRecordatorio(Context context, String titulo) {
        final String textoAHablar = "Alora te recuerda: " + titulo;
        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int langResult = tts.setLanguage(new Locale("es", "ES"));
                if (langResult != TextToSpeech.LANG_MISSING_DATA && langResult != TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.speak(textoAHablar, TextToSpeech.QUEUE_FLUSH, null, "ReminderTTS");
                }
            }
        });
    }
}
