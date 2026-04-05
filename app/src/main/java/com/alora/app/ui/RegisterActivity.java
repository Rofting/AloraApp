package com.alora.app.ui;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.alora.app.R;
import com.alora.app.api.ApiClient;
import com.alora.app.api.ApiService;
import com.alora.app.model.LoginResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilEmail, tilPassword;
    private TextInputEditText etFullName, etEmail, etPassword;
    private MaterialButton btnRegistrar, btnVolverLogin;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Referencias a los Layouts para mostrar errores
        tilFullName = findViewById(R.id.tilFullName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);

        // Referencias a los Inputs
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        // Botones y Progreso
        btnRegistrar = findViewById(R.id.btnRegistrar);
        btnVolverLogin = findViewById(R.id.btnVolverLogin);
        progressBar = findViewById(R.id.progressBar);

        btnVolverLogin.setOnClickListener(v -> finish());

        btnRegistrar.setOnClickListener(v -> validarYRegistrar());
    }

    private void validarYRegistrar() {
        // Limpiar errores previos
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);

        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        boolean isValid = true;

        // Validar Nombre
        if (name.isEmpty()) {
            tilFullName.setError("El nombre es obligatorio");
            isValid = false;
        }

        // Validar Email (Formato correcto)
        if (email.isEmpty()) {
            tilEmail.setError("El email es obligatorio");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Debe ser un email válido");
            isValid = false;
        }

        // Validar Contraseña (Mínimo 6 caracteres según el backend)
        if (password.isEmpty()) {
            tilPassword.setError("La contraseña es obligatoria");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("La contraseña debe tener al menos 6 caracteres");
            isValid = false;
        }

        if (isValid) {
            ejecutarRegistro(name, email, password);
        }
    }

    private void ejecutarRegistro(String name, String email, String password) {
        setLoadingState(true);
        ApiService api = ApiClient.getClient().create(ApiService.class);

        RegisterRequest request = new RegisterRequest(email, password, name);

        api.register(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoadingState(false);
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(RegisterActivity.this, "¡Registro exitoso!", Toast.LENGTH_SHORT).show();
                    finish(); // Cierra la actividad y vuelve al login
                } else {
                    // Si el backend devuelve un 400 por validación o 409 por conflicto
                    Toast.makeText(RegisterActivity.this, "Error: Verifica los datos o el usuario ya existe", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoadingState(false);
                Toast.makeText(RegisterActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Método para manejar la UI durante la carga
    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            btnRegistrar.setText("");
            btnRegistrar.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            btnRegistrar.setText("Registrarse ahora");
            btnRegistrar.setEnabled(true);
            progressBar.setVisibility(View.GONE);
        }
    }

    public static class RegisterRequest {
        public String email;
        public String password;
        public String fullName;

        public RegisterRequest(String email, String password, String fullName) {
            this.email = email;
            this.password = password;
            this.fullName = fullName;
        }
    }
}