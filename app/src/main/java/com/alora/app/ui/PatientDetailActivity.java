package com.alora.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.alora.app.R;
import com.bumptech.glide.Glide;

public class PatientDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_detail);

        // 1. Recibir TODOS los datos del Intent (No solo los que mostramos visualmente)
        Long idPaciente = getIntent().getLongExtra("EXTRA_ID", -1);
        String nombre = getIntent().getStringExtra("EXTRA_NOMBRE");
        String ciudad = getIntent().getStringExtra("EXTRA_CIUDAD");
        String alergias = getIntent().getStringExtra("EXTRA_ALERGIAS");
        String condiciones = getIntent().getStringExtra("EXTRA_CONDICIONES");
        String medicamentos = getIntent().getStringExtra("EXTRA_MEDICAMENTOS");
        String telefono = getIntent().getStringExtra("EXTRA_TELEFONO");
        String pin = getIntent().getStringExtra("EXTRA_PIN");
        String fotoUrl = getIntent().getStringExtra("EXTRA_FOTO");
        String qrToken = getIntent().getStringExtra("EXTRA_TOKEN");

        // 2. Mostrar el nombre
        TextView tvNombre = findViewById(R.id.tvDetailNombre);
        tvNombre.setText(nombre);

        // 3. Cargar la foto con Glide
        ImageView ivAvatar = findViewById(R.id.ivDetailAvatar);
        if (fotoUrl == null || fotoUrl.trim().isEmpty() || fotoUrl.equals("null")) {
            Glide.with(this).load(android.R.drawable.ic_menu_camera).into(ivAvatar);
        } else {
            String fullUrl = "http://192.168.1.196:8080/images/" + fotoUrl;
            Glide.with(this)
                    .load(fullUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_camera)
                    .into(ivAvatar);
        }

        // 4. Enlazar las vistas
        View btnBitacora = findViewById(R.id.btnVerBitacora);
        View btnQr = findViewById(R.id.btnVerQr);
        View btnIA = findViewById(R.id.btnAsistenteIA);
        View btnRecordatorios = findViewById(R.id.btnRecordatorios);
        View btnEditar = findViewById(R.id.btnEditarPaciente);

        // 5. Configurar los clics

        btnRecordatorios.setOnClickListener(v -> {
            Intent i = new Intent(this, RemindersActivity.class);
            i.putExtra("EXTRA_ID", idPaciente);
            startActivity(i);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        btnBitacora.setOnClickListener(v -> {
            Intent i = new Intent(this, CareLogActivity.class);
            i.putExtra("EXTRA_ID", idPaciente);
            startActivity(i);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        btnIA.setOnClickListener(v -> {
            Intent i = new Intent(this, AssistantActivity.class);
            i.putExtra("EXTRA_ID", idPaciente);
            startActivity(i);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        btnQr.setOnClickListener(v -> {
            Intent i = new Intent(this, QrActivity.class);
            i.putExtra("EXTRA_TOKEN", qrToken);
            i.putExtra("EXTRA_NOMBRE", nombre);
            startActivity(i);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // CORRECCIÓN: Pasar TODOS los datos al formulario de edición
        btnEditar.setOnClickListener(v -> {
            Intent i = new Intent(this, AddPacienteActivity.class);
            i.putExtra("EXTRA_ID", idPaciente);
            i.putExtra("EXTRA_NOMBRE", nombre);
            i.putExtra("EXTRA_CIUDAD", ciudad);
            i.putExtra("EXTRA_ALERGIAS", alergias);
            i.putExtra("EXTRA_CONDICIONES", condiciones);
            i.putExtra("EXTRA_MEDICAMENTOS", medicamentos);
            i.putExtra("EXTRA_TELEFONO", telefono);
            i.putExtra("EXTRA_PIN", pin);
            i.putExtra("EXTRA_FOTO", fotoUrl);
            i.putExtra("EXTRA_TOKEN", qrToken);
            startActivity(i);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });
    }
}