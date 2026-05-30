package com.alora.app.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.alora.app.ui.ReminderReceiver;

import java.util.Calendar;

public class AlarmHelper {

    public static void programarAlarma(Context context, Long reminderId, Long pacienteId, String titulo, String hora) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        String[] partes = hora.split(":");
        int horas = Integer.parseInt(partes[0]);
        int minutos = Integer.parseInt(partes[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, horas);
        calendar.set(Calendar.MINUTE, minutos);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("EXTRA_TITULO", titulo);
        intent.putExtra("EXTRA_RECORDATORIO_ID", reminderId);
        intent.putExtra("EXTRA_ID", pacienteId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId.intValue(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
            Log.d("AloraAlarm", "Alarma programada: " + titulo + " a las " + hora);
        } catch (SecurityException e) {
            Log.e("AloraAlarm", "No se pudo programar la alarma: falta permiso", e);
        }
    }

    // Overload sin pacienteId para compatibilidad en zonas donde no está disponible
    public static void programarAlarma(Context context, Long reminderId, String titulo, String hora) {
        programarAlarma(context, reminderId, -1L, titulo, hora);
    }

    public static void cancelarAlarma(Context context, Long reminderId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId.intValue(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
        Log.d("AloraAlarm", "Alarma cancelada: " + reminderId);
    }
}
