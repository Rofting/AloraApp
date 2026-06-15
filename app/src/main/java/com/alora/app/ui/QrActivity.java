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
    private MaterialButton btnBack;

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
            String baseApiUrl = com.alora.app.api.ApiClient.BASE_URL.replaceAll("/$", "");
            String qrImageUrl = baseApiUrl + "/public/profile/" + qrToken + "/qr-image";

            // Glide descarga y pinta la imagen PNG asíncronamente
            Glide.with(this)
                    .load(qrImageUrl)
                    .placeholder(R.drawable.ic_modern_qr)
                    .error(R.drawable.ic_modern_alert)
                    .into(ivQrCode);
        } else {
            Toast.makeText(this, getString(R.string.qr_token_error), Toast.LENGTH_LONG).show();
        }
    }
}