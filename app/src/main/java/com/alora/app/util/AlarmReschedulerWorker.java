package com.alora.app.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.alora.app.api.ApiClient;
import com.alora.app.api.ApiService;
import com.alora.app.local.AppDatabase;
import com.alora.app.model.Paciente;
import com.alora.app.model.Reminder;

import java.io.IOException;
import java.util.List;

public class AlarmReschedulerWorker extends Worker {

    public AlarmReschedulerWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        String token = new TokenManager(context).getToken();
        if (token == null) return Result.success();

        String authHeader = "Bearer " + token;
        ApiService api = ApiClient.getClient().create(ApiService.class);

        List<Paciente> pacientes = AppDatabase.getInstance(context)
                .pacienteDao().getAllPacientesLocales();

        for (Paciente paciente : pacientes) {
            if (paciente.getId() == null) continue;
            try {
                List<Reminder> reminders = api.getReminders(authHeader, paciente.getId()).execute().body();
                if (reminders == null) continue;
                for (Reminder r : reminders) {
                    if (r.isActive() && r.getId() != null && r.getTime() != null) {
                        AlarmHelper.programarAlarma(context, r.getId(), paciente.getId(),
                                r.getTitle(), r.getTime());
                    }
                }
            } catch (IOException e) {
                // Continuar con el siguiente paciente si hay error de red
            }
        }

        return Result.success();
    }
}
