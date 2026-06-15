package com.alora.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.alora.app.R;
import com.alora.app.api.ApiClient;
import com.alora.app.api.ApiService;
import com.alora.app.model.LoginRequest;
import com.alora.app.model.LoginResponse;
import com.alora.app.util.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvError;
    private TextView tvIrRegistro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvError = findViewById(R.id.tvError);
        tvIrRegistro = findViewById(R.id.tvIrRegistro);

        tvIrRegistro.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );

        btnLogin.setOnClickListener(v -> {
            String user = etUsername.getText().toString();
            String pass = etPassword.getText().toString();
            if (user.isEmpty() || pass.isEmpty()) {
                tvError.setText(getString(R.string.fill_all_fields));
            } else {
                ejecutarLogin(user, pass);
            }
        });
    }

    private void ejecutarLogin(String username, String password) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.login(new LoginRequest(username, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    new TokenManager(LoginActivity.this).saveToken(response.body().getToken());
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    tvError.setText(getString(R.string.wrong_credentials));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                tvError.setText(getString(R.string.network_error));
            }
        });
    }
}
