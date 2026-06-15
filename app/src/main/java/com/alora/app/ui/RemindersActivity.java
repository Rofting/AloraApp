package com.alora.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alora.app.R;
import com.alora.app.api.ApiClient;
import com.alora.app.api.ApiService;
import com.alora.app.model.Reminder;
import com.alora.app.util.AlarmHelper;
import com.alora.app.util.TokenManager;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RemindersActivity extends AppCompatActivity {

    private RecyclerView rvReminders;
    private View fabAddReminder;
    private View emptyStateReminders;
    private TokenManager tokenManager;
    private Long idPaciente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        rvReminders = findViewById(R.id.rvReminders);
        fabAddReminder = findViewById(R.id.fabAddReminder);
        emptyStateReminders = findViewById(R.id.emptyStateReminders);
        rvReminders.setLayoutManager(new LinearLayoutManager(this));
        tokenManager = new TokenManager(this);

        idPaciente = getIntent().getLongExtra("EXTRA_ID", -1);
        if (idPaciente == -1) {
            idPaciente = getIntent().getLongExtra("EXTRA_PACIENTE_ID", -1);
        }

        if (idPaciente == -1) {
            Toast.makeText(this, getString(R.string.toast_missing_patient), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        fabAddReminder.setOnClickListener(v -> {
            Intent i = new Intent(this, AddReminderActivity.class);
            i.putExtra("EXTRA_PACIENTE_ID", idPaciente);
            startActivity(i);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarRecordatorios();
    }

    private void cargarRecordatorios() {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.getReminders("Bearer " + tokenManager.getToken(), idPaciente).enqueue(new Callback<List<Reminder>>() {
            @Override
            public void onResponse(Call<List<Reminder>> call, Response<List<Reminder>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isEmpty()) {
                        emptyStateReminders.setVisibility(View.VISIBLE);
                        rvReminders.setVisibility(View.GONE);
                        return;
                    }
                    emptyStateReminders.setVisibility(View.GONE);
                    rvReminders.setVisibility(View.VISIBLE);
                    ReminderAdapter adapter = new ReminderAdapter(response.body(), new ReminderAdapter.OnReminderClickListener() {
                        @Override
                        public void onEditClick(Reminder reminder) {
                            Intent i = new Intent(RemindersActivity.this, AddReminderActivity.class);
                            i.putExtra("EXTRA_PACIENTE_ID", idPaciente);
                            i.putExtra("EXTRA_REMINDER_ID", reminder.getId());
                            i.putExtra("EXTRA_TITULO", reminder.getTitle());
                            i.putExtra("EXTRA_HORA", reminder.getTime());
                            i.putExtra("EXTRA_DIAS", reminder.getDaysOfWeek());
                            startActivity(i);
                        }

                        @Override
                        public void onDeleteClick(Reminder reminder) {
                            confirmarBorrado(reminder);
                        }

                        @Override
                        public void onToggleActive(Reminder reminder, boolean activo) {
                            cambiarEstado(reminder, activo);
                        }
                    });
                    rvReminders.setAdapter(adapter);
                }
            }
            @Override public void onFailure(Call<List<Reminder>> call, Throwable t) {}
        });
    }

    /** Activa o desactiva la alarma: actualiza el backend y programa/cancela la alarma local. */
    private void cambiarEstado(Reminder reminder, boolean activo) {
        reminder.setActive(activo);
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.updateReminder("Bearer " + tokenManager.getToken(), idPaciente, reminder.getId(), reminder)
                .enqueue(new Callback<Reminder>() {
                    @Override
                    public void onResponse(Call<Reminder> call, Response<Reminder> response) {
                        if (response.isSuccessful()) {
                            if (activo) {
                                AlarmHelper.programarAlarma(RemindersActivity.this, reminder.getId(),
                                        idPaciente, reminder.getTitle(), reminder.getTime(), reminder.getDaysOfWeek());
                                Toast.makeText(RemindersActivity.this, getString(R.string.alarm_enabled), Toast.LENGTH_SHORT).show();
                            } else {
                                AlarmHelper.cancelarAlarma(RemindersActivity.this, reminder.getId());
                                Toast.makeText(RemindersActivity.this, getString(R.string.alarm_disabled), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(RemindersActivity.this, getString(R.string.toast_update_error), Toast.LENGTH_SHORT).show();
                            cargarRecordatorios();
                        }
                    }
                    @Override public void onFailure(Call<Reminder> call, Throwable t) {
                        Toast.makeText(RemindersActivity.this, getString(R.string.toast_connection_error), Toast.LENGTH_SHORT).show();
                        cargarRecordatorios();
                    }
                });
    }

    private void confirmarBorrado(Reminder reminder) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_reminder_title))
                .setMessage(getString(R.string.delete_reminder_msg, reminder.getTitle()))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> borrarRecordatorioEnServidor(reminder.getId()))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void borrarRecordatorioEnServidor(Long idRecordatorio) {
        AlarmHelper.cancelarAlarma(this, idRecordatorio);
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.deleteReminder("Bearer " + tokenManager.getToken(), idPaciente, idRecordatorio).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) cargarRecordatorios();
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }
}
