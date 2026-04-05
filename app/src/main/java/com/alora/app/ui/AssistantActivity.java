package com.alora.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.alora.app.R;
import com.alora.app.api.ApiClient;
import com.alora.app.api.ApiService;
import com.alora.app.model.CareLog;
import com.alora.app.model.Reminder;
import com.alora.app.util.TokenManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class AssistantActivity extends AppCompatActivity {

    private TextView tvTextoPaciente, tvRespuestaIA;
    private FloatingActionButton fabMic;
    private EditText etTextoSimulado;

    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private boolean isListening = false;
    private TextToSpeech textToSpeech;

    private Long idPaciente;
    private TokenManager tokenManager;

    private String memoriaRecordatorios = "Sin recordatorios pendientes.";

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    iniciarEscucha();
                } else {
                    Toast.makeText(this, "Necesito usar el micrófono para ayudarte", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant);

        tvTextoPaciente = findViewById(R.id.tvTextoPaciente);
        tvRespuestaIA = findViewById(R.id.tvRespuestaIA);
        fabMic = findViewById(R.id.fabMic);
        etTextoSimulado = findViewById(R.id.etTextoSimulado);

        idPaciente = getIntent().getLongExtra("EXTRA_ID", -1);
        tokenManager = new TokenManager(this);

        cargarRecordatoriosParaLaIA();

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("es", "ES"));
            }
        });

        configurarReconocedorDeVoz();

        // Magia estilo WhatsApp: Cambiar el icono del micro a "Enviar" si hay texto
        etTextoSimulado.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    // Si está vacío, mostramos el icono del micrófono
                    fabMic.setImageResource(android.R.drawable.ic_btn_speak_now);
                } else {
                    // Si hay texto, mostramos el icono de enviar (flecha)
                    fabMic.setImageResource(android.R.drawable.ic_menu_send);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Acción inteligente del botón flotante (Micro o Enviar dependiendo del texto)
        fabMic.setOnClickListener(v -> {
            String textoActual = etTextoSimulado.getText().toString().trim();

            if (!textoActual.isEmpty()) {
                // Si el usuario escribió algo, llamamos a la función para enviarlo
                enviarTextoEscrito();
            } else {
                // Si está vacío, activamos la funcionalidad de voz
                if (isListening) {
                    detenerEscucha();
                } else {
                    verificarPermisoYEmpezarAEscuchar();
                }
            }
        });

        // Evento adicional: Soporte para la tecla "Enter" del teclado virtual
        etTextoSimulado.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND) {
                enviarTextoEscrito();
                return true;
            }
            return false;
        });
    }

    // Método auxiliar para procesar y enviar el texto escrito sin repetir código
    private void enviarTextoEscrito() {
        String textoDictado = etTextoSimulado.getText().toString().trim();

        if (!textoDictado.isEmpty()) {
            tvTextoPaciente.setText(textoDictado);
            etTextoSimulado.setText(""); // Limpiamos el input (el icono volverá a ser micro solo)
            etTextoSimulado.clearFocus(); // Ocultamos el foco/teclado
            procesarConInteligenciaArtificial(textoDictado);
        } else {
            Toast.makeText(this, "Escribe algo primero", Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarRecordatoriosParaLaIA() {
        if (idPaciente == -1) return;

        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.getReminders("Bearer " + tokenManager.getToken(), idPaciente).enqueue(new Callback<List<Reminder>>() {
            @Override
            public void onResponse(Call<List<Reminder>> call, Response<List<Reminder>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    StringBuilder resumen = new StringBuilder("Recordatorios pendientes:\n");
                    for (Reminder r : response.body()) {
                        if (r.isActive()) {
                            resumen.append("- ID: ").append(r.getId())
                                    .append(" | Título: '").append(r.getTitle())
                                    .append("' | Hora: ").append(r.getTime()).append("\n");
                        }
                    }
                    memoriaRecordatorios = resumen.toString();
                } else {
                    memoriaRecordatorios = "Sin recordatorios pendientes.";
                }
            }

            @Override
            public void onFailure(Call<List<Reminder>> call, Throwable t) {
                // Falla silenciosamente
            }
        });
    }

    private void configurarReconocedorDeVoz() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { tvTextoPaciente.setText("Escuchando..."); }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String textoDictado = matches.get(0);
                    tvTextoPaciente.setText(textoDictado);
                    procesarConInteligenciaArtificial(textoDictado);
                }
                detenerEscucha();
            }
            @Override public void onError(int error) {
                tvTextoPaciente.setText("No he podido entenderte bien. ¿Puedes repetirlo?");
                detenerEscucha();
            }
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
        fabMic.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#10B981")));
        fabMic.setImageResource(android.R.drawable.presence_audio_online);
    }

    private void detenerEscucha() {
        speechRecognizer.stopListening();
        isListening = false;
        fabMic.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3B82F6")));
        // Si no hay texto, restauramos el micro. Si lo hay, restauramos la flecha de enviar.
        if (etTextoSimulado.getText().toString().trim().isEmpty()) {
            fabMic.setImageResource(android.R.drawable.ic_btn_speak_now);
        } else {
            fabMic.setImageResource(android.R.drawable.ic_menu_send);
        }
    }

    private void procesarConInteligenciaArtificial(String textoPaciente) {
        tvRespuestaIA.setText("Escribiendo...");

        String apiKey = "Ingresar aqui";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        String prompt = "Eres Alora, una asistente médica muy empática. " +
                "El paciente te dice: '" + textoPaciente + "'.\n" +
                "Sus recordatorios actuales son:\n" + memoriaRecordatorios + "\n\n" +
                "INSTRUCCIONES:\n" +
                "1. Responde de forma cálida (máximo 2 líneas). Si acaba de tomar la medicación, pregúntale cómo se siente.\n" +
                "2. Si confirma que HA TOMADO un recordatorio de la lista, la accion es 'COMPLETADO'.\n" +
                "3. Si dice que NO se lo ha tomado o quiere hacerlo LUEGO, la accion es 'POSPONER'.\n" +
                "4. DEVUELVE ÚNICAMENTE UN JSON válido, sin etiquetas de markdown (nada de json), con esta estructura exacta:\n" +
                "{\n" +
                "  \"respuesta\": \"tu respuesta hablada aquí\",\n" +
                "  \"accion\": \"COMPLETADO\" o \"POSPONER\" o \"NINGUNA\",\n" +
                "  \"id_recordatorio\": <ID del recordatorio afectado o 0>,\n" +
                "  \"titulo_recordatorio\": \"<Título exacto o vacío>\"\n" +
                "}";

        String promptSeguro = prompt.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");

        String jsonBody = "{\"contents\": [{\"parts\":[{\"text\": \"" + promptSeguro + "\"}]}]}";

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonBody
        );

        Request request = new Request.Builder().url(url).post(body).build();
        OkHttpClient client = new OkHttpClient();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                runOnUiThread(() -> tvRespuestaIA.setText("Lo siento, no tengo conexión a internet en este momento."));
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                String responseBody = response.body().string();
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        String textoIA = jsonObject.getJSONArray("candidates")
                                .getJSONObject(0).getJSONObject("content")
                                .getJSONArray("parts").getJSONObject(0).getString("text");

                        textoIA = textoIA.replace("```json", "").replace("```", "").trim();

                        JSONObject jsonIA = new JSONObject(textoIA);
                        String respuestaHablada = jsonIA.getString("respuesta");
                        String accion = jsonIA.optString("accion", "NINGUNA");
                        long idRec = jsonIA.optLong("id_recordatorio", 0);
                        String tituloRec = jsonIA.optString("titulo_recordatorio", "");

                        runOnUiThread(() -> {
                            tvRespuestaIA.setText(respuestaHablada);
                            textToSpeech.speak(respuestaHablada, TextToSpeech.QUEUE_FLUSH, null, null);

                            guardarEnBitacora("Paciente: " + textoPaciente + "\nAlora: " + respuestaHablada);

                            if ("COMPLETADO".equals(accion) && idRec > 0) {
                                borrarRecordatorio(idRec, "¡Perfecto! He marcado tu recordatorio como completado.");
                            } else if ("POSPONER".equals(accion) && idRec > 0) {
                                posponerRecordatorio(idRec, tituloRec);
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> tvRespuestaIA.setText("Perdona, me he confundido un poco. ¿Podrías decirlo de otra forma?"));
                    }
                } else {
                    runOnUiThread(() -> tvRespuestaIA.setText("Error al conectar con mi cerebro: " + response.code()));
                }
            }
        });
    }

    private void borrarRecordatorio(Long idRec, String mensajeExito) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.deleteReminder("Bearer " + tokenManager.getToken(), idPaciente, idRec).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AssistantActivity.this, mensajeExito, Toast.LENGTH_SHORT).show();
                    cargarRecordatoriosParaLaIA();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void posponerRecordatorio(Long idAntiguo, String titulo) {
        borrarRecordatorio(idAntiguo, "Posponiendo...");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, 30);
        String nuevaHora = String.format(Locale.getDefault(), "%02d:%02d:00", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));

        ApiService api = ApiClient.getClient().create(ApiService.class);
        Reminder nuevoRec = new Reminder(titulo, nuevaHora);

        api.createReminder("Bearer " + tokenManager.getToken(), idPaciente, nuevoRec).enqueue(new Callback<Reminder>() {
            @Override
            public void onResponse(Call<Reminder> call, Response<Reminder> response) {
                if(response.isSuccessful()) {
                    Toast.makeText(AssistantActivity.this, "He pospuesto la alarma a las " + nuevaHora, Toast.LENGTH_LONG).show();
                    cargarRecordatoriosParaLaIA();
                }
            }
            @Override public void onFailure(Call<Reminder> call, Throwable t) {}
        });
    }

    private void guardarEnBitacora(String registroCompleto) {
        if (idPaciente == -1) return;

        ApiService api = ApiClient.getClient().create(ApiService.class);
        String authHeader = "Bearer " + tokenManager.getToken();

        CareLog nuevaNota = new CareLog("INTERACCIÓN IA", registroCompleto);

        api.createCareLog(authHeader, idPaciente, nuevaNota).enqueue(new Callback<CareLog>() {
            @Override
            public void onResponse(Call<CareLog> call, Response<CareLog> response) {}
            @Override
            public void onFailure(Call<CareLog> call, Throwable t) {}
        });
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }
}
