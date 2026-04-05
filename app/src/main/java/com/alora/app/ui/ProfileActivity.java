package com.alora.app.ui;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.alora.app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ProfileActivity extends AppCompatActivity {

    private String qrToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 1. Recibimos el Token que nos envía el Panel de Control
        qrToken = getIntent().getStringExtra("EXTRA_TOKEN");

        // 2. Enlazamos las vistas del nuevo diseño moderno
        TextView textTokenValue = findViewById(R.id.textTokenValue);
        TextInputEditText editPin = findViewById(R.id.editPin);
        MaterialButton btnUnlock = findViewById(R.id.btnUnlock);

        // 3. Mostramos el ID/Token de emergencia
        if (qrToken != null && !qrToken.isEmpty()) {
            textTokenValue.setText("ID Médico: " + qrToken);
        } else {
            textTokenValue.setText("ID Médico: No disponible");
        }

        // 4. Configuramos el botón de desbloqueo con PIN
        btnUnlock.setOnClickListener(v -> {
            String pinIngresado = editPin.getText() != null ? editPin.getText().toString().trim() : "";

            if (pinIngresado.isEmpty()) {
                Toast.makeText(this, "Por favor, introduce el PIN de seguridad", Toast.LENGTH_SHORT).show();
            } else {
                if (pinIngresado.equals("1234")) {
                    Toast.makeText(this, "Datos desbloqueados con éxito", Toast.LENGTH_SHORT).show();
                    // Aquí podrías cargar la información médica completa
                } else {
                    Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        // Animación suave al volver atrás
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}