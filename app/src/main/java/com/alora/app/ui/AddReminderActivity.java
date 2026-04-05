package com.alora.app.ui;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alora.app.R;
import com.alora.app.api.ApiClient;
import com.alora.app.api.ApiService;
import com.alora.app.model.Reminder;
import com.alora.app.util.AlarmHelper; // 🔥 IMPORTANTE: Conectamos con el programador de alarmas
import com.alora.app.util.TokenManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddReminderActivity extends AppCompatActivity {

    private TextInputEditText etTitulo;
    private TextView tvHoraSeleccionada, tvTituloPantalla;
    private MaterialButton btnGuardar;
    private View cardSeleccionarHora;

    private TokenManager tokenManager;
    private Long idPaciente;
    private Long idRecordatorio;

    private int horaSelec = 8;
    private int minSelec = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);

        etTitulo = findViewById(R.id.etTituloRecordatorio);
        tvHoraSeleccionada = findViewById(R.id.tvHoraSeleccionada);
        tvTituloPantalla = findViewById(R.id.tvTituloPantalla);
        btnGuardar = findViewById(R.id.btnGuardarRecordatorio);
        cardSeleccionarHora = findViewById(R.id.cardSeleccionarHora);

        tokenManager = new TokenManager(this);
        idPaciente = getIntent().getLongExtra("EXTRA_PACIENTE_ID", -1);
        idRecordatorio = getIntent().getLongExtra("EXTRA_REMINDER_ID", -1); // Si es -1, es creación

        if (idRecordatorio != -1) {
            // MODO EDICIÓN
            tvTituloPantalla.setText("Editar Alarma");
            btnGuardar.setText("Actualizar Alarma");
            etTitulo.setText(getIntent().getStringExtra("EXTRA_TITULO"));

            // Parseamos la hora que viene del servidor (Ej: "08:30:00" o "08:30")
            String[] partes = getIntent().getStringExtra("EXTRA_HORA").split(":");
            horaSelec = Integer.parseInt(partes[0]);
            minSelec = Integer.parseInt(partes[1]);
        } else {
            // MODO CREACIÓN (Hora actual por defecto)
            Calendar c = Calendar.getInstance();
            horaSelec = c.get(Calendar.HOUR_OF_DAY);
            minSelec = c.get(Calendar.MINUTE);
        }
        actualizarRelojUI();

        // ABRIR EL RELOJ AL TOCAR LA TARJETA
        cardSeleccionarHora.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, h, m) -> {
                horaSelec = h;
                minSelec = m;
                actualizarRelojUI();
            }, horaSelec, minSelec, true).show();
        });

        // GUARDAR ALARMA
        btnGuardar.setOnClickListener(v -> {
            String titulo = etTitulo.getText().toString().trim();
            if (titulo.isEmpty()) {
                Toast.makeText(this, "Escribe un título", Toast.LENGTH_SHORT).show();
                return;
            }

            // Formateamos la hora para que el backend la entienda perfectamente
            String horaBackend = String.format(Locale.getDefault(), "%02d:%02d:00", horaSelec, minSelec);

            if (idRecordatorio != -1) {
                // Borramos la vieja y creamos la nueva (Simula una edición)
                borrarYRecrear(titulo, horaBackend);
            } else {
                guardarEnServidor(titulo, horaBackend);
            }
        });
    }

    private void actualizarRelojUI() {
        tvHoraSeleccionada.setText(String.format(Locale.getDefault(), "%02d:%02d", horaSelec, minSelec));
    }

    private void borrarYRecrear(String titulo, String hora) {
        //  1. Cancelamos la alarma antigua físicamente en el móvil para que no suene
        AlarmHelper.cancelarAlarma(this, idRecordatorio);

        // 2. Borramos en el servidor y creamos la nueva
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.deleteReminder("Bearer " + tokenManager.getToken(), idPaciente, idRecordatorio).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                guardarEnServidor(titulo, hora);
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(AddReminderActivity.this, "Error de red al actualizar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarEnServidor(String titulo, String hora) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        Reminder nuevo = new Reminder(titulo, hora);

        api.createReminder("Bearer " + tokenManager.getToken(), idPaciente, nuevo).enqueue(new Callback<Reminder>() {
            @Override
            public void onResponse(Call<Reminder> call, Response<Reminder> response) {
                //  Aseguramos que recibimos el cuerpo de la respuesta con el ID generado por la BBDD
                if (response.isSuccessful() && response.body() != null) {
                    Reminder guardado = response.body();

                    //  3. Programamos la alarma definitiva usando el ID real
                    AlarmHelper.programarAlarma(AddReminderActivity.this, guardado.getId(), titulo, hora);

                    Toast.makeText(AddReminderActivity.this, "¡Alarma Guardada!", Toast.LENGTH_SHORT).show();
                    finish(); // Vuelve atrás a la lista
                } else {
                    Toast.makeText(AddReminderActivity.this, "Error al guardar en el servidor", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Reminder> call, Throwable t) {
                Toast.makeText(AddReminderActivity.this, "Fallo de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }
}