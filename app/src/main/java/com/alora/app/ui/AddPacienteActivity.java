package com.alora.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.alora.app.R;
import com.alora.app.api.ApiClient;
import com.alora.app.api.ApiService;
import com.alora.app.model.Paciente;
import com.alora.app.util.TokenManager;

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

    private EditText etNombre, etCiudad, etAlergias;
    private Button btnGuardar;
    private TokenManager tokenManager;

    // Variables para la foto
    private ImageView ivSeleccionarFoto;
    private Uri fotoUriSeleccionada = null;

    // Lanzador para abrir la galería
    private final ActivityResultLauncher<Intent> galeriaLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    fotoUriSeleccionada = result.getData().getData();
                    ivSeleccionarFoto.setImageURI(fotoUriSeleccionada); // Mostramos la foto en pantalla
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_paciente);

        etNombre = findViewById(R.id.etNombre);
        etCiudad = findViewById(R.id.etCiudad);
        etAlergias = findViewById(R.id.etAlergias);
        btnGuardar = findViewById(R.id.btnGuardar);
        ivSeleccionarFoto = findViewById(R.id.ivSeleccionarFoto); // Enlazamos el ImageView

        tokenManager = new TokenManager(this);

        //  Cuando toquen la imagen, abrimos la galería
        ivSeleccionarFoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galeriaLauncher.launch(intent);
        });

        //  Cuando toquen guardar
        btnGuardar.setOnClickListener(v -> {
            String nom = etNombre.getText().toString();
            String ciu = etCiudad.getText().toString();
            String ale = etAlergias.getText().toString();

            if (nom.isEmpty() || ciu.isEmpty() || ale.isEmpty()) {
                Toast.makeText(this, "Rellena todos los campos de texto", Toast.LENGTH_SHORT).show();
            } else {
                guardarEnServidor(nom, ciu, ale);
            }
        });
    }

    // PASO 1: Crear paciente (texto)
    private void guardarEnServidor(String n, String c, String a) {
        String token = tokenManager.getToken();
        String authHeader = "Bearer " + token;

        Paciente nuevo = new Paciente(n, c, a);
        ApiService api = ApiClient.getClient().create(ApiService.class);

        api.crearPaciente(authHeader, nuevo).enqueue(new Callback<Paciente>() {
            @Override
            public void onResponse(Call<Paciente> call, Response<Paciente> response) {
                if (response.isSuccessful() && response.body() != null) {

                    Long nuevoId = response.body().getId(); // Recuperamos el ID que nos da el servidor

                    // Si el servidor nos dio un ID y el usuario eligió foto, subimos la foto
                    if (fotoUriSeleccionada != null && nuevoId != null) {
                        subirFotoAlServidor(nuevoId);
                    } else {
                        Toast.makeText(AddPacienteActivity.this, "Paciente creado (Sin foto)", Toast.LENGTH_SHORT).show();
                        finish(); // Cerramos si no hay foto
                    }

                } else {
                    Toast.makeText(AddPacienteActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Paciente> call, Throwable t) {
                Toast.makeText(AddPacienteActivity.this, "Error de red al crear", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // PASO 2: Subir la foto
    private void subirFotoAlServidor(Long idPaciente) {
        try {
            // Convertimos la Uri en un Archivo real
            File archivoFoto = crearArchivoTemporalDesdeUri(fotoUriSeleccionada);

            // Preparamos las partes (Multipart)
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), archivoFoto);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", archivoFoto.getName(), requestFile);

            String authHeader = "Bearer " + tokenManager.getToken();
            ApiService api = ApiClient.getClient().create(ApiService.class);

            api.subirFoto(authHeader, idPaciente, body).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    Toast.makeText(AddPacienteActivity.this, "¡Paciente y foto guardados con éxito!", Toast.LENGTH_SHORT).show();
                    finish(); // Terminamos el proceso con éxito
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Toast.makeText(AddPacienteActivity.this, "Paciente creado, pero falló la subida de foto", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // MÉTODO AYUDANTE: Copia la imagen de la galería a la caché temporal de la app
    private File crearArchivoTemporalDesdeUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        File archivoTemporal = new File(getCacheDir(), "foto_upload.jpg");
        FileOutputStream outputStream = new FileOutputStream(archivoTemporal);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        outputStream.close();
        if (inputStream != null) {
            inputStream.close();
        }
        return archivoTemporal;
    }
}