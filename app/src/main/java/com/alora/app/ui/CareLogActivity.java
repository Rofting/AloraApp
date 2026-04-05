package com.alora.app.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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

public class CareLogActivity extends AppCompatActivity implements CareLogAdapter.OnLogItemLongClickListener {

    private EditText etNuevaNota;
    private View btnGuardarNota;
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

        rvCareLogs.setLayoutManager(new LinearLayoutManager(this));
        tokenManager = new TokenManager(this);

        idPaciente = getIntent().getLongExtra("EXTRA_ID", -1);

        if (idPaciente == -1) {
            Toast.makeText(this, "Error crítico: ID de paciente perdido", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        cargarHistorial();

        btnGuardarNota.setOnClickListener(v -> {
            String textoNota = etNuevaNota.getText().toString().trim();
            if (!textoNota.isEmpty()) {
                guardarNuevaNota(textoNota);
            } else {
                Toast.makeText(this, "La nota no puede estar vacía", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarHistorial() {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        String authHeader = "Bearer " + tokenManager.getToken();

        api.getCareLogs(authHeader, idPaciente).enqueue(new Callback<List<CareLog>>() {
            @Override
            public void onResponse(Call<List<CareLog>> call, Response<List<CareLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<CareLog> logs = response.body();
                    // Pasamos 'this' como listener
                    adapter = new CareLogAdapter(logs, CareLogActivity.this);
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

    private void guardarNuevaNota(String textoNota) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        String authHeader = "Bearer " + tokenManager.getToken();

        CareLog nuevaNota = new CareLog("GENERAL", textoNota);

        api.createCareLog(authHeader, idPaciente, nuevaNota).enqueue(new Callback<CareLog>() {
            @Override
            public void onResponse(Call<CareLog> call, Response<CareLog> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CareLogActivity.this, "Nota guardada", Toast.LENGTH_SHORT).show();
                    etNuevaNota.setText("");
                    cargarHistorial();
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

    // --- MÉTODOS PARA EDITAR Y ELIMINAR ---

    @Override
    public void onEditLog(CareLog log) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar Registro");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setText(log.getNote());
        builder.setView(input);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nuevoTexto = input.getText().toString().trim();
            if (!nuevoTexto.isEmpty() && !nuevoTexto.equals(log.getNote())) {
                ejecutarActualizacion(log.getId(), nuevoTexto, log.getLogType());
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    @Override
    public void onDeleteLog(CareLog log) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Registro")
                .setMessage("¿Estás seguro de que quieres borrar este registro?")
                .setPositiveButton("Sí, eliminar", (dialog, which) -> ejecutarEliminacion(log.getId()))
                .setNegativeButton("No", null)
                .show();
    }

    private void ejecutarActualizacion(Long logId, String nuevaNota, String tipo) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        String authHeader = "Bearer " + tokenManager.getToken();

        CareLog updateLog = new CareLog(tipo, nuevaNota);
        api.updateCareLog(authHeader, idPaciente, logId, updateLog).enqueue(new Callback<CareLog>() {
            @Override
            public void onResponse(Call<CareLog> call, Response<CareLog> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CareLogActivity.this, "Actualizado", Toast.LENGTH_SHORT).show();
                    cargarHistorial();
                } else {
                    Toast.makeText(CareLogActivity.this, "Error al actualizar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CareLog> call, Throwable t) {
                Toast.makeText(CareLogActivity.this, "Fallo de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void ejecutarEliminacion(Long logId) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        String authHeader = "Bearer " + tokenManager.getToken();

        api.deleteCareLog(authHeader, idPaciente, logId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CareLogActivity.this, "Eliminado", Toast.LENGTH_SHORT).show();
                    cargarHistorial();
                } else {
                    Toast.makeText(CareLogActivity.this, "Error al eliminar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(CareLogActivity.this, "Fallo de red", Toast.LENGTH_SHORT).show();
            }
        });
    }
}