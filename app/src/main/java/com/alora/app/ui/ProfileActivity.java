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
            textTokenValue.setText(getString(R.string.medical_id_prefix, qrToken.substring(0, Math.min(qrToken.length(), 8)) + "…"));
        } else {
            textTokenValue.setText(getString(R.string.medical_id_unavailable));
        }

        btnUnlock.setOnClickListener(v -> {
            String pin = editPin.getText() != null ? editPin.getText().toString().trim() : "";
            if (pin.isEmpty()) {
                Toast.makeText(this, getString(R.string.enter_pin), Toast.LENGTH_SHORT).show();
                return;
            }
            if (qrToken == null || qrToken.isEmpty()) {
                Toast.makeText(this, getString(R.string.invalid_token), Toast.LENGTH_SHORT).show();
                return;
            }
            verificarPin(pin);
        });
    }

    private void verificarPin(String pin) {
        btnUnlock.setEnabled(false);
        btnUnlock.setText(getString(R.string.verifying));

        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.unlockProfile(qrToken, new ApiService.UnlockRequest(pin)).enqueue(new Callback<ApiService.PublicProfile>() {
            @Override
            public void onResponse(Call<ApiService.PublicProfile> call, Response<ApiService.PublicProfile> response) {
                btnUnlock.setEnabled(true);
                btnUnlock.setText(getString(R.string.unlock_medical));
                if (response.isSuccessful() && response.body() != null) {
                    mostrarDatosMedicos(response.body());
                } else {
                    Toast.makeText(ProfileActivity.this, getString(R.string.wrong_pin), Toast.LENGTH_SHORT).show();
                    editPin.setText("");
                }
            }

            @Override
            public void onFailure(Call<ApiService.PublicProfile> call, Throwable t) {
                btnUnlock.setEnabled(true);
                btnUnlock.setText(getString(R.string.unlock_medical));
                Toast.makeText(ProfileActivity.this, getString(R.string.no_server_connection), Toast.LENGTH_SHORT).show();
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
        tvAlergias.setText(profile.allergies != null && !profile.allergies.isEmpty() ? profile.allergies : getString(R.string.none_f));
        tvCondiciones.setText(profile.medicalConditions != null && !profile.medicalConditions.isEmpty() ? profile.medicalConditions : getString(R.string.none_f));
        tvMedicamentos.setText(profile.medications != null && !profile.medications.isEmpty() ? profile.medications : getString(R.string.none_m));

        String contacto = "";
        if (profile.emergencyContactName != null) contacto = profile.emergencyContactName;
        if (profile.relationship != null && !profile.relationship.isEmpty())
            contacto += " (" + profile.relationship + ")";
        if (profile.emergencyContactPhone != null && !profile.emergencyContactPhone.isEmpty())
            contacto += "\n" + profile.emergencyContactPhone;
        tvContactoEmergencia.setText(contacto.isEmpty() ? getString(R.string.not_available) : contacto);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
