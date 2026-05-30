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
import com.alora.app.model.CareLogPage;
import com.alora.app.util.TokenManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CareLogActivity extends AppCompatActivity implements CareLogAdapter.OnLogItemLongClickListener {

    private EditText etNuevaNota;
    private View btnGuardarNota;
    private RecyclerView rvCareLogs;
    private View emptyState;
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
        emptyState = findViewById(R.id.emptyStateLogs);
        rvCareLogs.setLayoutManager(new LinearLayoutManager(this));
        tokenManager = new TokenManager(this);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        idPaciente = getIntent().getLongExtra("EXTRA_ID", -1);
        if (idPaciente == -1) {
            Toast.makeText(this, "Error: ID de paciente no encontrado", Toast.LENGTH_LONG).show();
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
        api.getCareLogs("Bearer " + tokenManager.getToken(), idPaciente, 0, 50)
                .enqueue(new Callback<CareLogPage>() {
                    @Override
                    public void onResponse(Call<CareLogPage> call, Response<CareLogPage> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<CareLog> logs = response.body().getContent();
                            if (logs.isEmpty()) {
                                emptyState.setVisibility(View.VISIBLE);
                                rvCareLogs.setVisibility(View.GONE);
                            } else {
                                emptyState.setVisibility(View.GONE);
                                rvCareLogs.setVisibility(View.VISIBLE);
                                adapter = new CareLogAdapter(logs, CareLogActivity.this);
                                rvCareLogs.setAdapter(adapter);
                            }
                        } else {
                            Toast.makeText(CareLogActivity.this, "Error al cargar historial", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<CareLogPage> call, Throwable t) {
                        Toast.makeText(CareLogActivity.this, "Sin conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void guardarNuevaNota(String textoNota) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.createCareLog("Bearer " + tokenManager.getToken(), idPaciente, new CareLog("GENERAL", textoNota))
                .enqueue(new Callback<CareLog>() {
                    @Override
                    public void onResponse(Call<CareLog> call, Response<CareLog> response) {
                        if (response.isSuccessful()) {
                            etNuevaNota.setText("");
                            cargarHistorial();
                        } else {
                            Toast.makeText(CareLogActivity.this, "Error al guardar", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<CareLog> call, Throwable t) {
                        Toast.makeText(CareLogActivity.this, "Sin conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }

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
        api.updateCareLog("Bearer " + tokenManager.getToken(), idPaciente, logId, new CareLog(tipo, nuevaNota))
                .enqueue(new Callback<CareLog>() {
                    @Override
                    public void onResponse(Call<CareLog> call, Response<CareLog> response) {
                        if (response.isSuccessful()) {
                            cargarHistorial();
                        } else {
                            Toast.makeText(CareLogActivity.this, "Error al actualizar", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<CareLog> call, Throwable t) {
                        Toast.makeText(CareLogActivity.this, "Sin conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void ejecutarEliminacion(Long logId) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.deleteCareLog("Bearer " + tokenManager.getToken(), idPaciente, logId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            cargarHistorial();
                        } else {
                            Toast.makeText(CareLogActivity.this, "Error al eliminar", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(CareLogActivity.this, "Sin conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
