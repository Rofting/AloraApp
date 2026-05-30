package com.alora.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.alora.app.R;
import com.alora.app.api.ApiClient;
import com.alora.app.api.ApiService;
import com.alora.app.model.Paciente;
import com.alora.app.util.TokenManager;
import com.google.android.material.button.MaterialButton;
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

    // Contenedores de error
    private TextInputLayout tilNombre, tilCiudad, tilNombreContacto, tilParentesco, tilTelefonoEmergencia, tilPinCode;

    // Inputs de texto
    private TextInputEditText etNombre, etCiudad, etAlergias, etCondiciones, etMedicamentos;
    private TextInputEditText etNombreContacto, etParentesco, etTelefonoEmergencia, etPinCode;

    // UI Elements
    private MaterialButton btnAgregarPaciente;
    private ProgressBar pbLoading;
    private ImageView ivSeleccionarFoto;

    private TokenManager tokenManager;
    private Uri fotoUriSeleccionada = null;
    private Long idPacienteEdit = null;
    private String qrTokenActual = null;
    private String fotoUrlActual = null;

    private final ActivityResultLauncher<PickVisualMediaRequest> galeriaLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    fotoUriSeleccionada = uri;
                    if (ivSeleccionarFoto != null) {
                        ivSeleccionarFoto.setImageURI(uri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_paciente);

        inicializarVistas();
        cargarDatosSiEsEdicion();

        if (ivSeleccionarFoto != null) {
            ivSeleccionarFoto.setOnClickListener(v -> {
                galeriaLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            });
        }

        btnAgregarPaciente.setOnClickListener(v -> validarYProcesar());
    }

    private void inicializarVistas() {
        tokenManager = new TokenManager(this);

        // Layouts para errores
        tilNombre = findViewById(R.id.tilNombre);
        tilCiudad = findViewById(R.id.tilCiudad);
        tilNombreContacto = findViewById(R.id.tilNombreContacto);
        tilParentesco = findViewById(R.id.tilParentesco);
        tilTelefonoEmergencia = findViewById(R.id.tilTelefonoEmergencia);
        tilPinCode = findViewById(R.id.tilPinCode);

        // Inputs
        etNombre = findViewById(R.id.etNombre);
        etCiudad = findViewById(R.id.etCiudad);
        etAlergias = findViewById(R.id.etAlergias);
        etCondiciones = findViewById(R.id.etCondiciones);
        etMedicamentos = findViewById(R.id.etMedicamentos);
        etNombreContacto = findViewById(R.id.etNombreContacto);
        etParentesco = findViewById(R.id.etParentesco);
        etTelefonoEmergencia = findViewById(R.id.etTelefonoEmergencia);
        etPinCode = findViewById(R.id.etPinCode);

        // Botones y Loader
        btnAgregarPaciente = findViewById(R.id.btnAgregarPaciente);
        pbLoading = findViewById(R.id.pbLoading);

        // Foto
        ivSeleccionarFoto = findViewById(R.id.ivSeleccionarFoto);
    }

    private void cargarDatosSiEsEdicion() {
        idPacienteEdit = getIntent().getLongExtra("EXTRA_ID", -1);

        if (idPacienteEdit != -1L) {
            etNombre.setText(getIntent().getStringExtra("EXTRA_NOMBRE"));
            etCiudad.setText(getIntent().getStringExtra("EXTRA_CIUDAD"));
            etAlergias.setText(getIntent().getStringExtra("EXTRA_ALERGIAS"));
            etCondiciones.setText(getIntent().getStringExtra("EXTRA_CONDICIONES"));
            etMedicamentos.setText(getIntent().getStringExtra("EXTRA_MEDICAMENTOS"));

            // Cargar los nuevos campos
            etNombreContacto.setText(getIntent().getStringExtra("EXTRA_NOMBRE_CONTACTO"));
            etParentesco.setText(getIntent().getStringExtra("EXTRA_PARENTESCO"));
            etTelefonoEmergencia.setText(getIntent().getStringExtra("EXTRA_TELEFONO"));
            etPinCode.setText(getIntent().getStringExtra("EXTRA_PIN"));

            qrTokenActual = getIntent().getStringExtra("EXTRA_TOKEN");
            fotoUrlActual = getIntent().getStringExtra("EXTRA_FOTO");

            btnAgregarPaciente.setText("Actualizar Perfil");
        } else {
            idPacienteEdit = null;
        }
    }

    private void validarYProcesar() {
        limpiarErrores();

        String nom = etNombre.getText().toString().trim();
        String ciu = etCiudad.getText().toString().trim();
        String ale = etAlergias.getText().toString().trim();
        String con = etCondiciones.getText().toString().trim();
        String med = etMedicamentos.getText().toString().trim();
        String nomCont = etNombreContacto.getText().toString().trim();
        String paren = etParentesco.getText().toString().trim();
        String tel = etTelefonoEmergencia.getText().toString().trim();
        String pin = etPinCode.getText().toString().trim();

        boolean isValid = true;

        // Validaciones rigurosas
        if (nom.isEmpty()) { tilNombre.setError("Requerido"); isValid = false; }
        if (ciu.isEmpty()) { tilCiudad.setError("Requerido"); isValid = false; }
        if (nomCont.isEmpty()) { tilNombreContacto.setError("Requerido para emergencias"); isValid = false; }
        if (paren.isEmpty()) { tilParentesco.setError("Requerido (Ej: Hijo)"); isValid = false; }
        if (tel.isEmpty()) { tilTelefonoEmergencia.setError("Requerido"); isValid = false; }
        if (pin.length() < 4) { tilPinCode.setError("Debe tener 4 dígitos"); isValid = false; }

        if (isValid) {
            ejecutarPeticionRed(nom, ciu, ale, con, med, nomCont, paren, tel, pin);
        }
    }

    private void limpiarErrores() {
        tilNombre.setError(null);
        tilCiudad.setError(null);
        tilNombreContacto.setError(null);
        tilParentesco.setError(null);
        tilTelefonoEmergencia.setError(null);
        tilPinCode.setError(null);
    }

    private void ejecutarPeticionRed(String n, String c, String a, String con, String med, String nomCont, String paren, String tel, String pin) {
        setLoadingState(true);
        String authHeader = "Bearer " + tokenManager.getToken();

        Paciente paciente = new Paciente(n, c, a, con, med, nomCont, paren, tel, pin);
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
                    Toast.makeText(AddPacienteActivity.this, "Fallo de conexión", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void manejarSubidaFoto(Long id) {
        if (fotoUriSeleccionada != null) {
            subirFotoAlServidor(id);
        } else {
            setLoadingState(false);
            Toast.makeText(this, "¡Perfil guardado correctamente!", Toast.LENGTH_SHORT).show();
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

            api.uploadPhoto(authHeader, id, body).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    setLoadingState(false);
                    Toast.makeText(AddPacienteActivity.this, "Guardado con éxito", Toast.LENGTH_SHORT).show();
                    finish();
                }
                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    setLoadingState(false);
                    Toast.makeText(AddPacienteActivity.this, "Perfil guardado, pero falló la subida de foto", Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        } catch (Exception e) {
            setLoadingState(false);
            Toast.makeText(this, "Error procesando la imagen", Toast.LENGTH_SHORT).show();
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

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            btnAgregarPaciente.setText("");
            btnAgregarPaciente.setEnabled(false);
            pbLoading.setVisibility(View.VISIBLE);
        } else {
            btnAgregarPaciente.setText(idPacienteEdit != null ? "Actualizar Perfil" : "Guardar Perfil");
            btnAgregarPaciente.setEnabled(true);
            pbLoading.setVisibility(View.GONE);
        }
    }
}