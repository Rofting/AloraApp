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
import com.alora.app.util.TokenManager;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RemindersActivity extends AppCompatActivity {

    private RecyclerView rvReminders;
    private View fabAddReminder;
    private TokenManager tokenManager;
    private Long idPaciente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        rvReminders = findViewById(R.id.rvReminders);
        fabAddReminder = findViewById(R.id.fabAddReminder);
        rvReminders.setLayoutManager(new LinearLayoutManager(this));
        tokenManager = new TokenManager(this);

        idPaciente = getIntent().getLongExtra("EXTRA_ID", -1);

        if (idPaciente == -1) {
            Toast.makeText(this, "Error: Falta ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // PERMISOS DE NOTIFICACIONES (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // VIAJAR A LA NUEVA PANTALLA PARA CREAR
        fabAddReminder.setOnClickListener(v -> {
            Intent i = new Intent(this, AddReminderActivity.class);
            i.putExtra("EXTRA_PACIENTE_ID", idPaciente);
            startActivity(i);
        });
    }

    // CUANDO VOLVEMOS DE CREAR/EDITAR, RECARGAMOS LA LISTA AUTOMÁTICAMENTE
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
                    ReminderAdapter adapter = new ReminderAdapter(response.body(), new ReminderAdapter.OnReminderClickListener() {

                        @Override
                        public void onEditClick(Reminder reminder) {
                            // VIAJAR A LA PANTALLA PARA EDITAR
                            Intent i = new Intent(RemindersActivity.this, AddReminderActivity.class);
                            i.putExtra("EXTRA_PACIENTE_ID", idPaciente);
                            i.putExtra("EXTRA_REMINDER_ID", reminder.getId());
                            i.putExtra("EXTRA_TITULO", reminder.getTitle());
                            i.putExtra("EXTRA_HORA", reminder.getTime());
                            startActivity(i);
                        }

                        @Override
                        public void onDeleteClick(Reminder reminder) {
                            confirmarBorrado(reminder);
                        }
                    });
                    rvReminders.setAdapter(adapter);
                }
            }
            @Override public void onFailure(Call<List<Reminder>> call, Throwable t) {}
        });
    }

    private void confirmarBorrado(Reminder reminder) {
        new AlertDialog.Builder(this)
                .setTitle("Borrar recordatorio")
                .setMessage("¿Eliminar '" + reminder.getTitle() + "' permanentemente?")
                .setPositiveButton("Borrar", (dialog, which) -> borrarRecordatorioEnServidor(reminder.getId()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void borrarRecordatorioEnServidor(Long idRecordatorio) {
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