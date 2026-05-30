package com.alora.app.ui;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.alora.app.R;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton; // 👈 Asegúrate de que se importe esto

public class QrActivity extends AppCompatActivity {

    private ImageView ivQrCode;
    private MaterialButton btnBack; // 👈 1. Declaramos la variable del botón

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);

        ivQrCode = findViewById(R.id.ivQrCode);
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        String nombre = getIntent().getStringExtra("EXTRA_NOMBRE");
        TextView tvQrNombre = findViewById(R.id.tvQrNombre);
        if (nombre != null && !nombre.isEmpty()) {
            tvQrNombre.setText(nombre);
        }

        String qrToken = getIntent().getStringExtra("EXTRA_TOKEN");

        if (qrToken != null && !qrToken.isEmpty()) {
            // Construimos la URL apuntando al endpoint de tu API en el emulador
            String baseApiUrl = "http://10.0.2.2:8080";
            String qrImageUrl = baseApiUrl + "/public/profile/" + qrToken + "/qr-image";

            // Glide descarga y pinta la imagen PNG asíncronamente
            Glide.with(this)
                    .load(qrImageUrl)
                    .placeholder(android.R.drawable.progress_horizontal)
                    .error(android.R.drawable.stat_notify_error)
                    .into(ivQrCode);
        } else {
            Toast.makeText(this, "Error crítico: El token del paciente llegó vacío", Toast.LENGTH_LONG).show();
        }
    }
}