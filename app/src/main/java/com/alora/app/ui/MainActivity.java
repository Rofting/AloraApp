package com.alora.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.alora.app.R;
import com.alora.app.api.ApiClient;
import com.alora.app.api.ApiService;
import com.alora.app.local.AppDatabase;
import com.alora.app.model.Paciente;
import com.alora.app.ui.LoginActivity;
import com.alora.app.util.TokenManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PacienteAdapter adapter;
    private TokenManager tokenManager;
    private FloatingActionButton fabAddPaciente;
    private ImageView ivLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tokenManager = new TokenManager(this);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (adapter != null) {
                    Paciente pacienteABorrar = adapter.getPacienteAt(position);
                    if (pacienteABorrar.getId() != null) {
                        ejecutarBorrado(pacienteABorrar.getId(), position);
                    } else {
                        Toast.makeText(MainActivity.this, "Error: Paciente sin ID", Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(position);
                    }
                }
            }
        }).attachToRecyclerView(recyclerView);

        fabAddPaciente = findViewById(R.id.fabAddPaciente);
        fabAddPaciente.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AddPacienteActivity.class);
            startActivity(intent);
        });

        ivLogout = findViewById(R.id.ivLogout);
        ivLogout.setOnClickListener(v -> {
            tokenManager.clearToken();
            Toast.makeText(MainActivity.this, "Sesión cerrada de forma segura", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        cargarPacientes();
    }

    private void cargarPacientes() {
        String token = tokenManager.getToken();

        if (token == null) {
            Toast.makeText(this, "Sesión expirada", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        //  1. OFFLINE: Cargar rápido de SQLite
        new Thread(() -> {
            List<Paciente> pacientesLocales = AppDatabase.getInstance(this).pacienteDao().getAllPacientesLocales();
            if (!pacientesLocales.isEmpty()) {
                runOnUiThread(() -> {
                    adapter = new PacienteAdapter(pacientesLocales);
                    recyclerView.setAdapter(adapter);
                });
            }
        }).start();

        //  2. ONLINE: Sincronizar datos frescos en segundo plano
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.getPacientes("Bearer " + token).enqueue(new Callback<List<Paciente>>() {
            @Override
            public void onResponse(Call<List<Paciente>> call, Response<List<Paciente>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Paciente> listaServidor = response.body();

                    new Thread(() -> {
                        AppDatabase db = AppDatabase.getInstance(MainActivity.this);
                        db.pacienteDao().deleteAll();
                        db.pacienteDao().insertAll(listaServidor);

                        runOnUiThread(() -> {
                            adapter = new PacienteAdapter(listaServidor);
                            recyclerView.setAdapter(adapter);
                        });
                    }).start();

                } else {
                    Toast.makeText(MainActivity.this, "Aviso: Mostrando datos locales", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Paciente>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Modo sin conexión activado", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void ejecutarBorrado(Long idPaciente, int position) {
        String token = tokenManager.getToken();
        ApiService api = ApiClient.getClient().create(ApiService.class);

        api.borrarPaciente("Bearer " + token, idPaciente).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    adapter.removePaciente(position);
                    Toast.makeText(MainActivity.this, "Paciente borrado", Toast.LENGTH_SHORT).show();
                    // Opcional: Podrías borrarlo de SQLite aquí también para mantener consistencia inmediata
                } else {
                    cargarPacientes();
                    Toast.makeText(MainActivity.this, "Error al borrar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                cargarPacientes();
                Toast.makeText(MainActivity.this, "Fallo de red al borrar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tokenManager.getToken() != null) {
            cargarPacientes();
        }
    }
}