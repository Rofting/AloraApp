package com.alora.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alora.app.R;

public class PatientDetailActivity extends AppCompatActivity {

    private TextView tvDetailNombre;
    private Button btnVerBitacora, btnVerQr;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_detail);

        Button btnAsistenteIA = findViewById(R.id.btnAsistenteIA);
        tvDetailNombre = findViewById(R.id.tvDetailNombre);
        btnVerBitacora = findViewById(R.id.btnVerBitacora);
        btnVerQr = findViewById(R.id.btnVerQr);

        // 1. Recogemos los datos que nos manda la lista
        String nombre = getIntent().getStringExtra("EXTRA_NOMBRE");
        String tokenQr = getIntent().getStringExtra("EXTRA_TOKEN");
        Long idPaciente = getIntent().getLongExtra("EXTRA_ID", -1); // ⚠️ El ID es vital para la bitácora

        if (nombre != null) {
            tvDetailNombre.setText(nombre);
        }

        // 2. Botón del QR (Nos lleva a la pantalla que ya tenías hecha)
        btnVerQr.setOnClickListener(v -> {
            Intent intentQr = new Intent(PatientDetailActivity.this, QrActivity.class);
            intentQr.putExtra("EXTRA_TOKEN", tokenQr);
            intentQr.putExtra("EXTRA_NOMBRE", nombre);
            startActivity(intentQr);
        });

// 3. Botón de la Bitácora (¡AHORA SÍ FUNCIONA!)
        btnVerBitacora.setOnClickListener(v -> {
            if (idPaciente != -1) {
                Intent intentBitacora = new Intent(PatientDetailActivity.this, CareLogActivity.class);
                intentBitacora.putExtra("EXTRA_ID", idPaciente); // Le pasamos el ID a la bitácora
                startActivity(intentBitacora);
            } else {
                Toast.makeText(this, "Error: Falta el ID del paciente", Toast.LENGTH_SHORT).show();
            }
        });

        btnAsistenteIA.setOnClickListener(v -> {
            Intent intent = new Intent(PatientDetailActivity.this, AssistantActivity.class);
            // Pasamos el ID del paciente, porque la IA lo necesitará luego para guardar la nota
            intent.putExtra("EXTRA_ID", idPaciente);
            startActivity(intent);
        });
    }
}