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
        // 1. Recuperamos el título de la alarma
        // Buscamos con ambas claves por si acaso la mandas como EXTRA_TITULO o TITULO_RECORDATORIO
        String titulo = intent.getStringExtra("EXTRA_TITULO");
        if (titulo == null) {
            titulo = intent.getStringExtra("TITULO_RECORDATORIO");
        }
        if (titulo == null) {
            titulo = "¡Es hora de tu recordatorio!";
        }

        Log.d("AloraAlarm", "¡Alarma recibida! " + titulo);

        // 2. Encender la pantalla brevemente (WakeLock)
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "AloraApp::ReminderWakeLock"
        );
        // Mantenemos la pantalla encendida por 3 segundos
        wakeLock.acquire(3000);

        // 3. Mostrar Notificación visual
        mostrarNotificacion(context, titulo);

        // 4. Hacer que el móvil hable (TTS)
        hablarRecordatorio(context, titulo);
    }

    private void mostrarNotificacion(Context context, String titulo) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // A partir de Android 8.0, es obligatorio crear un "Canal de Notificaciones"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorios de Alora",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Canal para avisos de pastillas y tareas");
            notificationManager.createNotificationChannel(channel);
        }

        // Qué pasa si el usuario toca la notificación (Abre la app)
        Intent intentAbrirApp = new Intent(context, MainActivity.class);
        intentAbrirApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intentAbrirApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Construimos la tarjeta de la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // ⚠️ Asegúrate de tener un icono aquí
                .setContentTitle("Alora te avisa:")
                .setContentText(titulo)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL) // Para que suene y vibre
                .setAutoCancel(true) // Que desaparezca al tocarla
                .setContentIntent(pendingIntent);

        // Lanzamos la notificación
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void hablarRecordatorio(Context context, String titulo) {
        final String textoAHablar = "Alora te recuerda: " + titulo;

        // Usamos getApplicationContext() para evitar fugas de memoria en el BroadcastReceiver
        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Configurar idioma a español
                int langResult = tts.setLanguage(new Locale("es", "ES"));
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("AloraTTS", "Idioma español no soportado en este dispositivo.");
                } else {
                    // Reproducir el mensaje
                    tts.speak(textoAHablar, TextToSpeech.QUEUE_FLUSH, null, "ReminderTTS");
                }
            } else {
                Log.e("AloraTTS", "Error al inicializar el motor de voz (TTS)");
            }
        });
    }
}