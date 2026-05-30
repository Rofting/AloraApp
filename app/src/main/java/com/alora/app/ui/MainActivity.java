package com.alora.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
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
import com.alora.app.model.UserInfo;
import com.alora.app.util.TokenManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PacienteAdapter adapter;
    private TokenManager tokenManager;
    private ExtendedFloatingActionButton fabAddPaciente;
    private View ivLogout;
    private TextView tvWelcome;
    private View emptyStatePacientes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tokenManager = new TokenManager(this);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        tvWelcome = findViewById(R.id.tvWelcome);
        emptyStatePacientes = findViewById(R.id.emptyStatePacientes);

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
                        adapter.notifyItemChanged(position);
                    }
                }
            }
        }).attachToRecyclerView(recyclerView);

        fabAddPaciente = findViewById(R.id.fabAddPaciente);
        fabAddPaciente.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, AddPacienteActivity.class))
        );

        ivLogout = findViewById(R.id.ivLogout);
        ivLogout.setOnClickListener(v -> {
            tokenManager.clearToken();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        cargarNombreUsuario();
        cargarPacientes();
    }

    private void cargarNombreUsuario() {
        String token = tokenManager.getToken();
        if (token == null) return;
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.getMe("Bearer " + token).enqueue(new Callback<UserInfo>() {
            @Override
            public void onResponse(Call<UserInfo> call, Response<UserInfo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String nombre = response.body().getFullName();
                    if (nombre != null && !nombre.isEmpty()) {
                        tvWelcome.setText("Hola, " + nombre.split(" ")[0]);
                    }
                }
            }
            @Override public void onFailure(Call<UserInfo> call, Throwable t) {}
        });
    }

    private void cargarPacientes() {
        String token = tokenManager.getToken();
        if (token == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        new Thread(() -> {
            List<Paciente> pacientesLocales = AppDatabase.getInstance(this).pacienteDao().getAllPacientesLocales();
            runOnUiThread(() -> {
                if (adapter == null && !pacientesLocales.isEmpty()) {
                    mostrarLista(pacientesLocales);
                } else if (adapter == null) {
                    mostrarEstadoVacio();
                }
            });
        }).start();

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
                            if (listaServidor.isEmpty()) {
                                mostrarEstadoVacio();
                            } else {
                                mostrarLista(listaServidor);
                            }
                        });
                    }).start();
                }
            }

            @Override
            public void onFailure(Call<List<Paciente>> call, Throwable t) {
                if (adapter == null) {
                    Toast.makeText(MainActivity.this, "Modo sin conexión", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void mostrarLista(List<Paciente> pacientes) {
        emptyStatePacientes.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        adapter = new PacienteAdapter(pacientes);
        recyclerView.setAdapter(adapter);
    }

    private void mostrarEstadoVacio() {
        adapter = null;
        recyclerView.setVisibility(View.GONE);
        emptyStatePacientes.setVisibility(View.VISIBLE);
    }

    private void ejecutarBorrado(Long idPaciente, int position) {
        String token = tokenManager.getToken();
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.deletePaciente("Bearer " + token, idPaciente).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    adapter.removePaciente(position);
                    if (adapter.getItemCount() == 0) mostrarEstadoVacio();
                } else {
                    cargarPacientes();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                cargarPacientes();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tokenManager.getToken() != null) {
            adapter = null;
            cargarPacientes();
        }
    }
}
