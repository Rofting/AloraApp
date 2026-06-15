package com.alora.app.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.alora.app.ui.ReminderReceiver;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class AlarmHelper {

    /**
     * Programa la próxima ocurrencia de la alarma respetando los días de la semana
     * ("TODOS" o lista tipo "LUNES,MIERCOLES"). La alarma se reprograma sola al sonar
     * gracias a ReminderReceiver.
     */
    public static void programarAlarma(Context context, Long reminderId, Long pacienteId,
                                       String titulo, String hora, String diasSemana) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Calendar calendar = calcularProximaOcurrencia(hora, diasSemana);
        if (calendar == null) {
            Log.e("AloraAlarm", "Hora inválida, no se programa: " + hora);
            return;
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("EXTRA_TITULO", titulo);
        intent.putExtra("EXTRA_RECORDATORIO_ID", reminderId);
        intent.putExtra("EXTRA_ID", pacienteId);
        intent.putExtra("EXTRA_HORA", hora);
        intent.putExtra("EXTRA_DIAS", diasSemana == null ? "TODOS" : diasSemana);

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
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
            Log.d("AloraAlarm", "Alarma programada: " + titulo + " → " + calendar.getTime());
        } catch (SecurityException e) {
            Log.e("AloraAlarm", "No se pudo programar la alarma: falta permiso", e);
        }
    }

    public static void programarAlarma(Context context, Long reminderId, Long pacienteId, String titulo, String hora) {
        programarAlarma(context, reminderId, pacienteId, titulo, hora, "TODOS");
    }

    // Overload sin pacienteId para compatibilidad en zonas donde no está disponible
    public static void programarAlarma(Context context, Long reminderId, String titulo, String hora) {
        programarAlarma(context, reminderId, -1L, titulo, hora, "TODOS");
    }

    /** Calcula la próxima fecha/hora válida según la hora "HH:MM[:SS]" y los días de la semana. */
    static Calendar calcularProximaOcurrencia(String hora, String diasSemana) {
        int horas, minutos;
        try {
            String[] partes = hora.split(":");
            horas = Integer.parseInt(partes[0].trim());
            minutos = Integer.parseInt(partes[1].trim());
        } catch (Exception e) {
            return null;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, horas);
        calendar.set(Calendar.MINUTE, minutos);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        Set<Integer> diasValidos = parsearDias(diasSemana);
        if (diasValidos != null) {
            for (int i = 0; i < 7; i++) {
                if (diasValidos.contains(calendar.get(Calendar.DAY_OF_WEEK))) break;
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }
        }
        return calendar;
    }

    /** Devuelve null si aplica todos los días. */
    private static Set<Integer> parsearDias(String diasSemana) {
        if (diasSemana == null || diasSemana.trim().isEmpty()) return null;
        String normalizado = diasSemana.toUpperCase(Locale.ROOT);
        if (normalizado.contains("TODOS")) return null;

        Set<Integer> dias = new HashSet<>();
        for (String dia : normalizado.split(",")) {
            switch (dia.trim()) {
                case "LUNES": dias.add(Calendar.MONDAY); break;
                case "MARTES": dias.add(Calendar.TUESDAY); break;
                case "MIERCOLES": case "MIÉRCOLES": dias.add(Calendar.WEDNESDAY); break;
                case "JUEVES": dias.add(Calendar.THURSDAY); break;
                case "VIERNES": dias.add(Calendar.FRIDAY); break;
                case "SABADO": case "SÁBADO": dias.add(Calendar.SATURDAY); break;
                case "DOMINGO": dias.add(Calendar.SUNDAY); break;
            }
        }
        return dias.isEmpty() ? null : dias;
    }

    /**
     * Pospone una alarma N minutos como aviso puntual: no toca el recordatorio del
     * servidor ni la alarma recurrente. Al no llevar EXTRA_HORA, el receiver no la reprograma.
     */
    public static void programarSnooze(Context context, Long reminderId, Long pacienteId,
                                       String titulo, int minutos) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, minutos);

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("EXTRA_TITULO", titulo);
        intent.putExtra("EXTRA_RECORDATORIO_ID", reminderId);
        intent.putExtra("EXTRA_ID", pacienteId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) (reminderId + 1_000_000),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
            Log.d("AloraAlarm", "Snooze de " + minutos + " min para: " + titulo);
        } catch (SecurityException e) {
            Log.e("AloraAlarm", "No se pudo posponer la alarma", e);
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

        if (alarmManager != null) alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
        Log.d("AloraAlarm", "Alarma cancelada: " + reminderId);
    }
}
