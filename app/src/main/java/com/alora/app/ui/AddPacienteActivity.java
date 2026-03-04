package com.alora.app.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.alora.app.R;
import com.alora.app.api.ApiClient;
import com.alora.app.api.ApiService;
import com.alora.app.model.Paciente;
import com.alora.app.util.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddPacienteActivity extends AppCompatActivity {

    private EditText etNombre, etCiudad, etAlergias;
    private Button btnGuardar;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_paciente);

        etNombre = findViewById(R.id.etNombre);
        etCiudad = findViewById(R.id.etCiudad);
        etAlergias = findViewById(R.id.etAlergias);
        btnGuardar = findViewById(R.id.btnGuardar);
        tokenManager = new TokenManager(this);

        btnGuardar.setOnClickListener(v -> {
            String nom = etNombre.getText().toString();
            String ciu = etCiudad.getText().toString();
            String ale = etAlergias.getText().toString();

            if (nom.isEmpty() || ciu.isEmpty() || ale.isEmpty()) {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
            } else {
                guardarEnServidor(nom, ciu, ale);
            }
        });
    }

    private void guardarEnServidor(String n, String c, String a) {
        String token = tokenManager.getToken();
        String authHeader = "Bearer " + token;

        Paciente nuevo = new Paciente(n, c, a);
        ApiService api = ApiClient.getClient().create(ApiService.class);

        api.crearPaciente(authHeader, nuevo).enqueue(new Callback<Paciente>() {
            @Override
            public void onResponse(Call<Paciente> call, Response<Paciente> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddPacienteActivity.this, "Paciente creado", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AddPacienteActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Paciente> call, Throwable t) {
                Toast.makeText(AddPacienteActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }
}