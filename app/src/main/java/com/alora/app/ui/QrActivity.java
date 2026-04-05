package com.alora.app.ui;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alora.app.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

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

        // 3. Fabricamos la imagen del QR de forma LOCAL (Offline)
        if (token != null && !token.isEmpty()) {
            generarQRLocal(token);
        } else {
            Toast.makeText(this, "Error: El paciente no tiene un Token QR", Toast.LENGTH_LONG).show();
        }
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void generarQRLocal(String token) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            String urlPublica = "http://192.168.1.196:8080/care/patient/" + token;

            // Le decimos a la librería que dibuje un cuadrado de 800x800 píxeles
            BitMatrix bitMatrix = writer.encode(urlPublica, BarcodeFormat.QR_CODE, 800, 800);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            // Pintamos los píxeles (Negro si hay dato, Blanco si está vacío)
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            // Colocamos el dibujo terminado en el ImageView
            ivQrCode.setImageBitmap(bmp);

        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al generar el código QR", Toast.LENGTH_SHORT).show();
        }
    }
}