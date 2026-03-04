package com.alora.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tokenManager = new TokenManager(this);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fabAddPaciente = findViewById(R.id.fabAddPaciente);
        fabAddPaciente.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AddPacienteActivity.class);
                startActivity(intent);
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

        android.util.Log.e("ALORA_DEBUG", "🔥 MI CABECERA ES: " + authHeader);

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
    @Override
    protected void onResume() {
        super.onResume();
        if (tokenManager.getToken() != null) {
            cargarPacientes();
        }
    }
}