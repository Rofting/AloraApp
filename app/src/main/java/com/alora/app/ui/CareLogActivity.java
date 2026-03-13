package com.alora.app.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alora.app.R;
import com.alora.app.api.ApiClient;
import com.alora.app.api.ApiService;
import com.alora.app.model.CareLog;
import com.alora.app.util.TokenManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CareLogActivity extends AppCompatActivity {

    private EditText etNuevaNota;
    private Button btnGuardarNota;
    private RecyclerView rvCareLogs;
    private CareLogAdapter adapter;
    private TokenManager tokenManager;
    private Long idPaciente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_care_log);

        etNuevaNota = findViewById(R.id.etNuevaNota);
        btnGuardarNota = findViewById(R.id.btnGuardarNota);
        rvCareLogs = findViewById(R.id.rvCareLogs);

        // Las listas siempre necesitan un LayoutManager
        rvCareLogs.setLayoutManager(new LinearLayoutManager(this));
        tokenManager = new TokenManager(this);

        // 1. Recibimos el ID del paciente que nos manda el Panel de Control
        idPaciente = getIntent().getLongExtra("EXTRA_ID", -1);

        if (idPaciente == -1) {
            Toast.makeText(this, "Error crítico: ID de paciente perdido", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 2. Cargamos las notas antiguas
        cargarHistorial();

        // 3. Configuramos el botón de guardar
        btnGuardarNota.setOnClickListener(v -> {
            String textoNota = etNuevaNota.getText().toString().trim();
            if (!textoNota.isEmpty()) {
                guardarNuevaNota(textoNota);
            } else {
                Toast.makeText(this, "La nota no puede estar vacía", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // MÉTODO 1: PEDIR HISTORIAL AL SERVIDOR
    private void cargarHistorial() {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        String authHeader = "Bearer " + tokenManager.getToken();

        api.getCareLogs(authHeader, idPaciente).enqueue(new Callback<List<CareLog>>() {
            @Override
            public void onResponse(Call<List<CareLog>> call, Response<List<CareLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<CareLog> logs = response.body();
                    adapter = new CareLogAdapter(logs);
                    rvCareLogs.setAdapter(adapter);
                } else {
                    Toast.makeText(CareLogActivity.this, "Error al cargar historial", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<CareLog>> call, Throwable t) {
                Toast.makeText(CareLogActivity.this, "Fallo de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // MÉTODO 2: ENVIAR NUEVA NOTA AL SERVIDOR
    private void guardarNuevaNota(String textoNota) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        String authHeader = "Bearer " + tokenManager.getToken();

        // Creamos la nota. Como tipo de registro (logType) pondremos "GENERAL" por defecto.
        CareLog nuevaNota = new CareLog("GENERAL", textoNota);

        api.createCareLog(authHeader, idPaciente, nuevaNota).enqueue(new Callback<CareLog>() {
            @Override
            public void onResponse(Call<CareLog> call, Response<CareLog> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CareLogActivity.this, "Nota guardada", Toast.LENGTH_SHORT).show();
                    etNuevaNota.setText(""); // Limpiamos el cajón de texto
                    cargarHistorial();       // Recargamos la lista para que aparezca la nueva
                } else {
                    Toast.makeText(CareLogActivity.this, "Error al guardar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CareLog> call, Throwable t) {
                Toast.makeText(CareLogActivity.this, "Fallo de red al guardar", Toast.LENGTH_SHORT).show();
            }
        });
    }
}