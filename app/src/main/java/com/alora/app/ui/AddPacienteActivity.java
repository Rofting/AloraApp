package com.alora.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.alora.app.R;
import com.alora.app.api.ApiClient;
import com.alora.app.api.ApiService;
import com.alora.app.model.Paciente;
import com.alora.app.util.TokenManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddPacienteActivity extends AppCompatActivity {

    private TextInputLayout tilNombre, tilCiudad, tilPinCode;
    private TextInputEditText etNombre, etCiudad, etAlergias, etCondiciones, etMedicamentos, etTelefonoEmergencia, etPinCode;
    private Button btnGuardar;
    private ProgressBar progressBar;
    private ImageView ivSeleccionarFoto;
    private TokenManager tokenManager;
    private Uri fotoUriSeleccionada = null;
    private Long idPacienteEdit = null;
    private String qrTokenActual = null;
    private String fotoUrlActual = null;

    private final ActivityResultLauncher<Intent> galeriaLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    fotoUriSeleccionada = result.getData().getData();
                    ivSeleccionarFoto.setImageURI(fotoUriSeleccionada);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_paciente);

        // Referencias a los Layouts de validación
        tilNombre = findViewById(R.id.tilNombre);
        tilCiudad = findViewById(R.id.tilCiudad);
        tilPinCode = findViewById(R.id.tilPinCode);

        // Referencias a los Inputs
        etNombre = findViewById(R.id.etNombre);
        etCiudad = findViewById(R.id.etCiudad);
        etAlergias = findViewById(R.id.etAlergias);
        etCondiciones = findViewById(R.id.etCondiciones);
        etMedicamentos = findViewById(R.id.etMedicamentos);
        etTelefonoEmergencia = findViewById(R.id.etTelefonoEmergencia);
        etPinCode = findViewById(R.id.etPinCode);

        btnGuardar = findViewById(R.id.btnGuardar);
        progressBar = findViewById(R.id.progressBar);
        ivSeleccionarFoto = findViewById(R.id.ivSeleccionarFoto);

        tokenManager = new TokenManager(this);

// RECOGER DATOS PARA EDICIÓN
        idPacienteEdit = getIntent().getLongExtra("EXTRA_ID", -1);
        if (idPacienteEdit == -1) {
            idPacienteEdit = null;
        } else {
            etNombre.setText(getIntent().getStringExtra("EXTRA_NOMBRE"));
            etCiudad.setText(getIntent().getStringExtra("EXTRA_CIUDAD"));
            etAlergias.setText(getIntent().getStringExtra("EXTRA_ALERGIAS"));
            etCondiciones.setText(getIntent().getStringExtra("EXTRA_CONDICIONES"));
            etMedicamentos.setText(getIntent().getStringExtra("EXTRA_MEDICAMENTOS"));
            etTelefonoEmergencia.setText(getIntent().getStringExtra("EXTRA_TELEFONO"));
            etPinCode.setText(getIntent().getStringExtra("EXTRA_PIN"));

            // Guardamos los datos ocultos para no perderlos al actualizar
            qrTokenActual = getIntent().getStringExtra("EXTRA_TOKEN");
            fotoUrlActual = getIntent().getStringExtra("EXTRA_FOTO");

            btnGuardar.setText("Actualizar Paciente");
        }

        ivSeleccionarFoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galeriaLauncher.launch(intent);
        });

        btnGuardar.setOnClickListener(v -> validarYProcesar());
    }

    private void validarYProcesar() {
        // Limpiar errores previos
        tilNombre.setError(null);
        tilCiudad.setError(null);
        tilPinCode.setError(null);

        String nom = etNombre.getText().toString().trim();
        String ciu = etCiudad.getText().toString().trim();
        String ale = etAlergias.getText().toString().trim();
        String con = etCondiciones.getText().toString().trim();
        String med = etMedicamentos.getText().toString().trim();
        String tel = etTelefonoEmergencia.getText().toString().trim();
        String pin = etPinCode.getText().toString().trim();

        boolean isValid = true;

        if (nom.isEmpty()) {
            tilNombre.setError("El nombre es obligatorio");
            isValid = false;
        }
        if (ciu.isEmpty()) {
            tilCiudad.setError("La ciudad es obligatoria");
            isValid = false;
        }
        if (pin.isEmpty()) {
            tilPinCode.setError("El PIN es obligatorio");
            isValid = false;
        }

        if (isValid) {
            procesarPaciente(nom, ciu, ale, con, med, tel, pin);
        }
    }

    private void procesarPaciente(String n, String c, String a, String con, String med, String tel, String pin) {
        setLoadingState(true);
        String authHeader = "Bearer " + tokenManager.getToken();
        Paciente paciente = new Paciente(n, c, a, con, med, tel, pin);
        ApiService api = ApiClient.getClient().create(ApiService.class);

        if (idPacienteEdit != null) {
            paciente.setId(idPacienteEdit);
            paciente.setQrToken(qrTokenActual);
            paciente.setFoto(fotoUrlActual);

            api.updatePaciente(authHeader, idPacienteEdit, paciente).enqueue(new Callback<Paciente>() {
                @Override
                public void onResponse(Call<Paciente> call, Response<Paciente> response) {
                    if (response.isSuccessful()) {
                        manejarSubidaFoto(idPacienteEdit);
                    } else {
                        setLoadingState(false);
                        Toast.makeText(AddPacienteActivity.this, "Error Servidor: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Paciente> call, Throwable t) {
                    setLoadingState(false);
                    Toast.makeText(AddPacienteActivity.this, "Error de Red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            api.crearPaciente(authHeader, paciente).enqueue(new Callback<Paciente>() {
                @Override
                public void onResponse(Call<Paciente> call, Response<Paciente> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        manejarSubidaFoto(response.body().getId());
                    } else {
                        setLoadingState(false);
                        Toast.makeText(AddPacienteActivity.this, "Error al crear: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Paciente> call, Throwable t) {
                    setLoadingState(false);
                    Toast.makeText(AddPacienteActivity.this, "Fallo conexión", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void manejarSubidaFoto(Long id) {
        if (fotoUriSeleccionada != null) {
            subirFotoAlServidor(id);
        } else {
            setLoadingState(false);
            Toast.makeText(this, "¡Paciente guardado!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void subirFotoAlServidor(Long id) {
        try {
            File archivoFoto = crearArchivoTemporalDesdeUri(fotoUriSeleccionada);
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), archivoFoto);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", archivoFoto.getName(), requestFile);

            String authHeader = "Bearer " + tokenManager.getToken();
            ApiService api = ApiClient.getClient().create(ApiService.class);

            api.subirFoto(authHeader, id, body).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    setLoadingState(false);
                    Toast.makeText(AddPacienteActivity.this, "Guardado con éxito", Toast.LENGTH_SHORT).show();
                    finish();
                }
                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    setLoadingState(false);
                    Toast.makeText(AddPacienteActivity.this, "Paciente guardado, pero falló la foto", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        } catch (Exception e) {
            setLoadingState(false);
            finish();
        }
    }

    private File crearArchivoTemporalDesdeUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        File archivoTemporal = new File(getCacheDir(), "temp_img.jpg");
        FileOutputStream outputStream = new FileOutputStream(archivoTemporal);
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) > 0) outputStream.write(buffer, 0, len);
        outputStream.close();
        if (inputStream != null) inputStream.close();
        return archivoTemporal;
    }

    // Método para manejar la UI durante la carga
    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            btnGuardar.setText("");
            btnGuardar.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            btnGuardar.setText(idPacienteEdit != null ? "Actualizar Paciente" : "Guardar Paciente");
            btnGuardar.setEnabled(true);
            progressBar.setVisibility(View.GONE);
        }
    }
}