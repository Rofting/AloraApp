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

    // Definimos las variables para las vistas
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvError;
    private TextView tvIrRegistro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Enlazamos las variables con los IDs del XML
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvError = findViewById(R.id.tvError);
        tvIrRegistro = findViewById(R.id.tvIrRegistro);

        tvIrRegistro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Creamos la intención de viajar a RegisterActivity
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        // 2. Evento de clic en el botón de Iniciar Sesión (El que ya tenías)
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = etUsername.getText().toString();
                String pass = etPassword.getText().toString();

                if (user.isEmpty() || pass.isEmpty()) {
                    tvError.setText("Por favor, rellena todos los campos");
                } else {
                    ejecutarLogin(user, pass);
                }
            }
        });
    }

    private void ejecutarLogin(String username, String password) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        LoginRequest loginRequest = new LoginRequest(username, password);

        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    // 1. Obtenemos el cuerpo de la respuesta
                    LoginResponse res = response.body();

                    // 2. Guardamos el token usando nuestro TokenManager
                    TokenManager tokenManager  = new TokenManager(LoginActivity.this);
                    tokenManager.saveToken(res.getToken());

                    // 3. Navegamos a la siguiente pantalla
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class); // (H26, H27, H28, H29)
                    startActivity(intent);
                    finish();

                } else {
                    tvError.setText("Datos incorrectos");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                tvError.setText("Error de red");
            }
        });
    }
}