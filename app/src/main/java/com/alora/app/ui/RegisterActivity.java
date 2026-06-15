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
import com.alora.app.model.RegisterRequest;
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

        tilFullName = findViewById(R.id.tilFullName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        btnVolverLogin = findViewById(R.id.btnVolverLogin);
        progressBar = findViewById(R.id.progressBar);

        btnRegistrar.setOnClickListener(v -> validarYRegistrar());
        btnVolverLogin.setOnClickListener(v -> finish());
    }

    private void validarYRegistrar() {
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);

        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        boolean esValido = true;

        if (fullName.isEmpty()) {
            tilFullName.setError(getString(R.string.name_required));
            esValido = false;
        }
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.email_required));
            esValido = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.email_invalid));
            esValido = false;
        }
        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.password_required));
            esValido = false;
        } else if (password.length() < 6) {
            tilPassword.setError(getString(R.string.password_min));
            esValido = false;
        }

        if (!esValido) return;

        setLoadingState(true);

        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.register(new RegisterRequest(email, password, fullName)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoadingState(false);
                if (response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, getString(R.string.register_success), Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this,
                            getString(R.string.user_exists, response.code()),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoadingState(false);
                Toast.makeText(RegisterActivity.this, "Fallo de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        btnRegistrar.setText(isLoading ? "" : getString(R.string.register_now));
        btnRegistrar.setEnabled(!isLoading);
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

}
