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

    public static void programarAlarma(Context context, Long reminderId, String titulo, String hora) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Desglosar la hora ("HH:mm")
        String[] partes = hora.split(":");
        int horas = Integer.parseInt(partes[0]);
        int minutos = Integer.parseInt(partes[1]);

        // Configurar el calendario para la alarma
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, horas);
        calendar.set(Calendar.MINUTE, minutos);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Si la hora programada ya pasó hoy, programarla para mañana
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Crear el intent que despertará al Receiver
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("EXTRA_TITULO", titulo);
        intent.putExtra("EXTRA_REMINDER_ID", reminderId);

        // Usar FLAG_UPDATE_CURRENT para poder actualizar la alarma si se edita
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId.intValue(), // ID único para la alarma
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            // Programar alarma exacta según la versión de Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                } else {
                    // Si no tiene permiso, usar alarma inexacta (mejor pedir permiso antes)
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