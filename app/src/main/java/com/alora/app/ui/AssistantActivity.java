package com.alora.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.alora.app.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class AssistantActivity extends AppCompatActivity {

    private TextView tvTextoPaciente, tvRespuestaIA;
    private FloatingActionButton fabMic;

    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private boolean isListening = false;
    private Long idPaciente; // Guardamos el ID para el futuro

    // El "Lanzador" para pedir permiso de micrófono
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    iniciarEscucha();
                } else {
                    Toast.makeText(this, "Necesito usar el micrófono para poder ayudarte", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant);

        tvTextoPaciente = findViewById(R.id.tvTextoPaciente);
        tvRespuestaIA = findViewById(R.id.tvRespuestaIA);
        fabMic = findViewById(R.id.fabMic);

        // Recuperamos el ID del paciente que nos pasaron desde el Panel de Control
        idPaciente = getIntent().getLongExtra("EXTRA_ID", -1);

        configurarReconocedorDeVoz();

        // Al tocar el micrófono, comprobamos permiso y escuchamos
        fabMic.setOnClickListener(v -> {
            if (isListening) {
                detenerEscucha();
            } else {
                verificarPermisoYEmpezarAEscuchar();
            }
        });
    }

    // PREPARAMOS EL MOTOR DE VOZ
    private void configurarReconocedorDeVoz() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        // Le decimos que escuche lenguaje natural libre
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES"); // Configurado para español

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                // Cuando está listo para que hablemos
                tvTextoPaciente.setText("Te escucho...");
            }

            @Override
            public void onResults(Bundle results) {
                // ¡AQUÍ LLEGA EL TEXTO TRADUCIDO!
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String textoDictado = matches.get(0);
                    tvTextoPaciente.setText(textoDictado);

                    // TODO: Misión 2 -> Enviar "textoDictado" a la IA de Gemini
                    tvRespuestaIA.setText("Procesando respuesta...");
                }
                detenerEscucha();
            }

            @Override
            public void onError(int error) {
                String mensajeError = "Error desconocido";
                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO: mensajeError = "Error de audio (Revisa el micro)"; break;
                    case SpeechRecognizer.ERROR_CLIENT: mensajeError = "Error del cliente"; break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: mensajeError = "Faltan permisos"; break;
                    case SpeechRecognizer.ERROR_NETWORK: mensajeError = "Error de red"; break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: mensajeError = "Tiempo de red agotado"; break;
                    case SpeechRecognizer.ERROR_NO_MATCH: mensajeError = "No he entendido las palabras"; break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: mensajeError = "El reconocedor está ocupado"; break;
                    case SpeechRecognizer.ERROR_SERVER: mensajeError = "Error del servidor de Google"; break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: mensajeError = "No has dicho nada (Silencio)"; break;
                }

                tvTextoPaciente.setText("Fallo (" + error + "): " + mensajeError);
                detenerEscucha();
            }

            // Métodos obligatorios vacíos (no los necesitamos ahora)
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void verificarPermisoYEmpezarAEscuchar() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            iniciarEscucha();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void iniciarEscucha() {
        speechRecognizer.startListening(speechIntent);
        isListening = true;
        fabMic.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50"))); // Cambia a verde
    }

    private void detenerEscucha() {
        speechRecognizer.stopListening();
        isListening = false;
        fabMic.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF4081"))); // Vuelve a rosa
    }

    // Es muy importante apagar el micrófono cuando cerramos la pantalla para no gastar batería
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}