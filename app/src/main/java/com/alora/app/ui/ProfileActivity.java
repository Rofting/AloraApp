package com.alora.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alora.app.R;
import com.alora.app.api.ApiClient;
import com.alora.app.api.ApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private String qrToken;
    private MaterialButton btnUnlock;
    private TextInputEditText editPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        qrToken = getIntent().getStringExtra("EXTRA_TOKEN");

        TextView textTokenValue = findViewById(R.id.textTokenValue);
        editPin = findViewById(R.id.editPin);
        btnUnlock = findViewById(R.id.btnUnlock);

        if (qrToken != null && !qrToken.isEmpty()) {
            textTokenValue.setText("ID Médico: " + qrToken.substring(0, Math.min(qrToken.length(), 8)) + "...");
        } else {
            textTokenValue.setText("ID Médico: No disponible");
        }

        btnUnlock.setOnClickListener(v -> {
            String pin = editPin.getText() != null ? editPin.getText().toString().trim() : "";
            if (pin.isEmpty()) {
                Toast.makeText(this, "Introduce el PIN de seguridad", Toast.LENGTH_SHORT).show();
                return;
            }
            if (qrToken == null || qrToken.isEmpty()) {
                Toast.makeText(this, "Token inválido", Toast.LENGTH_SHORT).show();
                return;
            }
            verificarPin(pin);
        });
    }

    private void verificarPin(String pin) {
        btnUnlock.setEnabled(false);
        btnUnlock.setText("Verificando...");

        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.unlockProfile(qrToken, new ApiService.UnlockRequest(pin)).enqueue(new Callback<ApiService.PublicProfile>() {
            @Override
            public void onResponse(Call<ApiService.PublicProfile> call, Response<ApiService.PublicProfile> response) {
                btnUnlock.setEnabled(true);
                btnUnlock.setText("Desbloquear Datos Médicos");
                if (response.isSuccessful() && response.body() != null) {
                    mostrarDatosMedicos(response.body());
                } else {
                    Toast.makeText(ProfileActivity.this, "PIN incorrecto", Toast.LENGTH_SHORT).show();
                    editPin.setText("");
                }
            }

            @Override
            public void onFailure(Call<ApiService.PublicProfile> call, Throwable t) {
                btnUnlock.setEnabled(true);
                btnUnlock.setText("Desbloquear Datos Médicos");
                Toast.makeText(ProfileActivity.this, "Sin conexión con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDatosMedicos(ApiService.PublicProfile profile) {
        editPin.setVisibility(View.GONE);
        btnUnlock.setVisibility(View.GONE);

        View sectionMedica = findViewById(R.id.sectionDatosMedicos);
        sectionMedica.setVisibility(View.VISIBLE);

        TextView tvNombrePaciente = findViewById(R.id.tvNombrePaciente);
        TextView tvAlergias = findViewById(R.id.tvAlergias);
        TextView tvCondiciones = findViewById(R.id.tvCondiciones);
        TextView tvMedicamentos = findViewById(R.id.tvMedicamentos);
        TextView tvContactoEmergencia = findViewById(R.id.tvContactoEmergencia);

        tvNombrePaciente.setText(profile.fullName != null ? profile.fullName : "—");
        tvAlergias.setText(profile.allergies != null && !profile.allergies.isEmpty() ? profile.allergies : "Ninguna");
        tvCondiciones.setText(profile.medicalConditions != null && !profile.medicalConditions.isEmpty() ? profile.medicalConditions : "Ninguna");
        tvMedicamentos.setText(profile.medications != null && !profile.medications.isEmpty() ? profile.medications : "Ninguno");

        String contacto = "";
        if (profile.emergencyContactName != null) contacto = profile.emergencyContactName;
        if (profile.relationship != null && !profile.relationship.isEmpty())
            contacto += " (" + profile.relationship + ")";
        if (profile.emergencyContactPhone != null && !profile.emergencyContactPhone.isEmpty())
            contacto += "\n" + profile.emergencyContactPhone;
        tvContactoEmergencia.setText(contacto.isEmpty() ? "No disponible" : contacto);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
