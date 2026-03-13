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
import com.alora.app.model.Paciente;
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

        // ⚠️ NUEVO: Configurar el gesto de deslizar (Swipe to delete)
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // No queremos reordenar arrastrando arriba/abajo
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                // Nos aseguramos de que el adaptador ya esté cargado
                if (adapter != null) {
                    Paciente pacienteABorrar = adapter.getPacienteAt(position);

                    if (pacienteABorrar.getId() != null) {
                        ejecutarBorrado(pacienteABorrar.getId(), position);
                    } else {
                        Toast.makeText(MainActivity.this, "Error: Paciente sin ID", Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(position); // Si falla, devolvemos la fila a su sitio
                    }
                }
            }
        }).attachToRecyclerView(recyclerView);

        // 1. Configuración del botón FLOTANTE (Añadir Paciente)
        fabAddPaciente = findViewById(R.id.fabAddPaciente);
        fabAddPaciente.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AddPacienteActivity.class);
                startActivity(intent);
            }
        });

        // 2. Configuración del botón de SALIR (Cerrar Sesión)
        ivLogout = findViewById(R.id.ivLogout);
        ivLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Borramos el token
                tokenManager.clearToken();

                // Avisamos al usuario
                Toast.makeText(MainActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();

                // Volvemos al Login y cerramos esta pantalla
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
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

        ApiService api = ApiClient.getClient().create(ApiService.class);
        String authHeader = "Bearer " + token;

        api.getPacientes(authHeader).enqueue(new Callback<List<Paciente>>() {
            @Override
            public void onResponse(Call<List<Paciente>> call, Response<List<Paciente>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Paciente> lista = response.body();
                    adapter = new PacienteAdapter(lista);
                    recyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(MainActivity.this, "Error HTTP: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Paciente>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ⚠️ NUEVO MÉTODO: Llama al servidor para borrar el paciente
    private void ejecutarBorrado(Long idPaciente, int position) {
        String token = tokenManager.getToken();
        ApiService api = ApiClient.getClient().create(ApiService.class);

        api.borrarPaciente("Bearer " + token, idPaciente).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Si el servidor lo borra, lo quitamos de la lista visual
                    adapter.removePaciente(position);
                    Toast.makeText(MainActivity.this, "Paciente borrado", Toast.LENGTH_SHORT).show();
                } else {
                    // Si el servidor falla, recargamos la lista para que vuelva a aparecer
                    cargarPacientes();
                    Toast.makeText(MainActivity.this, "Error al borrar: " + response.code(), Toast.LENGTH_SHORT).show();
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