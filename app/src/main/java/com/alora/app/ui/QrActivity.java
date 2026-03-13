package com.alora.app.ui;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alora.app.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class QrActivity extends AppCompatActivity {

    private TextView tvQrNombre;
    private ImageView ivQrCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);

        tvQrNombre = findViewById(R.id.tvQrNombre);
        ivQrCode = findViewById(R.id.ivQrCode);

        // 1. Abrimos el Intent y sacamos los datos
        String token = getIntent().getStringExtra("EXTRA_TOKEN");
        String nombre = getIntent().getStringExtra("EXTRA_NOMBRE");

        // 2. Ponemos el nombre en la pantalla
        if (nombre != null) {
            tvQrNombre.setText(nombre);
        }

        // 3. Cargamos la imagen del QR desde el servidor
        if (token != null && !token.isEmpty()) {

            // ⚠️ IMPORTANTE: Esta es la ruta exacta de tu backend público
            String qrUrl = "http://192.168.1.196:8080/public/profile/" + token + "/qr-image";

            android.util.Log.d("ALORA_QR", "Descargando QR desde: " + qrUrl);

            Glide.with(this)
                    .load(qrUrl)
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Evita guardar QRs viejos
                    .skipMemoryCache(true)
                    .placeholder(android.R.drawable.ic_popup_sync)
                    .error(android.R.drawable.stat_notify_error)
                    .into(ivQrCode);
        } else {
            Toast.makeText(this, "Error: El paciente no tiene un Token QR", Toast.LENGTH_LONG).show();
        }
    }
}