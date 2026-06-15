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
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alora.app.R;
import com.alora.app.api.ApiClient;
import com.alora.app.api.ApiService;
import com.alora.app.model.CareLog;
import com.alora.app.model.Reminder;
import com.alora.app.util.AlarmHelper;
import com.alora.app.util.TokenManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AssistantActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private ChatAdapter chatAdapter;
    private FloatingActionButton fabMic;
    private EditText etTextoSimulado;

    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private boolean isListening = false;
    private boolean esperandoRespuesta = false;
    private TextToSpeech textToSpeech;

    private Long idPaciente;
    private TokenManager tokenManager;

    private String memoriaRecordatorios = "Sin recordatorios pendientes.";
    private Long idRecordatorioActual = null;
    private String tituloRecordatorioActual = "";
    private String diasRecordatorioActual = "TODOS";
    private boolean resultadoRecibido = false;

    /** Últimos turnos de conversación para que la IA tenga memoria. */
    private final List<String> historialConversacion = new ArrayList<>();
    private static final int MAX_TURNOS_HISTORIAL = 8;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    iniciarEscucha();
                } else {
                    Toast.makeText(this, getString(R.string.mic_permission), Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant);

        rvChat = findViewById(R.id.rvChat);
        fabMic = findViewById(R.id.fabMic);
        etTextoSimulado = findViewById(R.id.etTextoSimulado);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvChat.setLayoutManager(lm);
        chatAdapter = new ChatAdapter();
        rvChat.setAdapter(chatAdapter);

        agregarMensajeIA(getString(R.string.chat_greeting));

        findViewById(R.id.btnBack).setOnClickListener(v -> volverAtras());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { volverAtras(); }
        });

        idPaciente = getIntent().getLongExtra("EXTRA_ID", -1);
        tokenManager = new TokenManager(this);

        idRecordatorioActual = getIntent().getLongExtra("EXTRA_RECORDATORIO_ID", -1L);
        if (idRecordatorioActual == -1L) idRecordatorioActual = null;
        tituloRecordatorioActual = getIntent().getStringExtra("EXTRA_RECORDATORIO_TITULO");
        if (tituloRecordatorioActual == null) tituloRecordatorioActual = "";
        String diasIntent = getIntent().getStringExtra("EXTRA_RECORDATORIO_DIAS");
        if (diasIntent != null && !diasIntent.isEmpty()) diasRecordatorioActual = diasIntent;

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(localeAsistente());
                if (idRecordatorioActual != null && !tituloRecordatorioActual.isEmpty()) {
                    ejecutarAvisoProactivoDeAlarma(tituloRecordatorioActual);
                }
            }
        });

        cargarRecordatoriosParaLaIA();

        etTextoSimulado.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isListening) {
                    fabMic.setImageResource(s.toString().trim().isEmpty()
                            ? R.drawable.ic_modern_mic : R.drawable.ic_modern_send);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        fabMic.setOnClickListener(v -> {
            String textoActual = etTextoSimulado.getText().toString().trim();
            if (!textoActual.isEmpty()) {
                enviarTextoEscrito();
            } else {
                if (isListening) detenerEscucha(); else verificarPermisoYEmpezarAEscuchar();
            }
        });
    }

    /** Locale del asistente: inglés si el dispositivo está en inglés, español en caso contrario. */
    private Locale localeAsistente() {
        return esIngles() ? Locale.US : new Locale("es", "ES");
    }

    private boolean esIngles() {
        return Locale.getDefault().getLanguage().equals("en");
    }

    /**
     * Si la pantalla se abrió desde la alarma (FLAG_ACTIVITY_CLEAR_TASK), es la raíz
     * de la tarea y finish() cerraría la app entera: en ese caso volvemos al inicio.
     */
    private void volverAtras() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        finish();
    }

    // ---------- Chat ----------

    private void agregarMensajeUsuario(String texto) {
        chatAdapter.agregar(new ChatAdapter.Mensaje(texto, true));
        rvChat.smoothScrollToPosition(chatAdapter.ultimoIndice());
    }

    private void agregarMensajeIA(String texto) {
        chatAdapter.agregar(new ChatAdapter.Mensaje(texto, false));
        rvChat.smoothScrollToPosition(chatAdapter.ultimoIndice());
    }

    /** Sustituye la burbuja "Escribiendo…" por la respuesta real. */
    private void resolverRespuestaIA(String texto) {
        if (esperandoRespuesta && chatAdapter.ultimoEsDeIA()) {
            chatAdapter.actualizarUltimo(texto);
        } else {
            agregarMensajeIA(texto);
        }
        esperandoRespuesta = false;
        rvChat.smoothScrollToPosition(chatAdapter.ultimoIndice());
    }

    private void mostrarEscribiendo() {
        agregarMensajeIA(getString(R.string.typing));
        esperandoRespuesta = true;
    }

    private void enviarTextoEscrito() {
        String texto = etTextoSimulado.getText().toString().trim();
        if (!texto.isEmpty()) {
            etTextoSimulado.setText("");
            agregarMensajeUsuario(texto);
            procesarConInteligenciaArtificial(texto);
        }
    }

    private void mostrarYHablarRespuesta(String texto) {
        resolverRespuestaIA(texto);
        hablar(texto);
    }

    /**
     * Habla con tolerancia a fallos: si la conexión con el motor TTS ha muerto
     * (DeadObjectException, frecuente al alternar con SpeechRecognizer), se
     * reconecta y repite la frase para que el aviso por voz nunca se pierda.
     */
    private void hablar(String texto) {
        if (texto == null || texto.isEmpty()) return;
        int resultado = TextToSpeech.ERROR;
        if (textToSpeech != null) {
            try {
                resultado = textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "AloraTTS");
            } catch (Exception ignored) {}
        }
        if (resultado == TextToSpeech.ERROR) {
            reconectarTTSYRepetir(texto);
        }
    }

    private void reconectarTTSYRepetir(String texto) {
        try {
            if (textToSpeech != null) textToSpeech.shutdown();
        } catch (Exception ignored) {}
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(localeAsistente());
                try {
                    textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "AloraTTS");
                } catch (Exception ignored) {}
            }
        });
    }

    // ---------- IA ----------

    private void ejecutarAvisoProactivoDeAlarma(String tituloAlarma) {
        agregarMensajeUsuario(getString(R.string.alarm_chip, tituloAlarma));
        mostrarEscribiendo();

        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.getSpeakText("Bearer " + tokenManager.getToken(), idPaciente, idRecordatorioActual)
                .enqueue(new Callback<ApiService.SpeakResponse>() {
                    @Override
                    public void onResponse(Call<ApiService.SpeakResponse> call, Response<ApiService.SpeakResponse> response) {
                        String texto = (response.isSuccessful() && response.body() != null)
                                ? response.body().texto
                                : getString(R.string.alarm_fallback, tituloAlarma);
                        mostrarYHablarRespuesta(texto);
                    }
                    @Override
                    public void onFailure(Call<ApiService.SpeakResponse> call, Throwable t) {
                        mostrarYHablarRespuesta(getString(R.string.alarm_fallback, tituloAlarma));
                    }
                });
    }

    private void cargarRecordatoriosParaLaIA() {
        if (idPaciente == -1) return;
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.getReminders("Bearer " + tokenManager.getToken(), idPaciente).enqueue(new Callback<List<Reminder>>() {
            @Override
            public void onResponse(Call<List<Reminder>> call, Response<List<Reminder>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    StringBuilder resumen = new StringBuilder("Pautas médicas activas:\n");
                    boolean primero = false;
                    for (Reminder r : response.body()) {
                        if (r.isActive()) {
                            if (idRecordatorioActual == null && !primero) {
                                idRecordatorioActual = r.getId();
                                tituloRecordatorioActual = r.getTitle();
                                if (r.getDaysOfWeek() != null) diasRecordatorioActual = r.getDaysOfWeek();
                                primero = true;
                            } else if (r.getId() != null && r.getId().equals(idRecordatorioActual)
                                    && r.getDaysOfWeek() != null) {
                                diasRecordatorioActual = r.getDaysOfWeek();
                            }
                            resumen.append("- '").append(r.getTitle()).append("' a las ").append(r.getTime())
                                    .append(" los días [").append(r.getDaysOfWeek()).append("]\n");
                        }
                    }
                    memoriaRecordatorios = resumen.toString();
                }
            }
            @Override public void onFailure(Call<List<Reminder>> call, Throwable t) {}
        });
    }

    private void procesarConInteligenciaArtificial(String textoPaciente) {
        mostrarEscribiendo();
        ApiService api = ApiClient.getClient().create(ApiService.class);

        String fechaActual = new SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy, HH:mm",
                new Locale("es", "ES")).format(new Date());

        String contexto = "Fecha y hora actual: " + fechaActual +
                "\n" + memoriaRecordatorios +
                "\nAlarma en foco: " + (tituloRecordatorioActual.isEmpty() ? "Ninguna" : tituloRecordatorioActual) +
                (esIngles() ? "\nIMPORTANT: The patient speaks English. Reply ONLY in English." : "");

        String historial = String.join("\n", historialConversacion);

        api.enviarMensajeIA("Bearer " + tokenManager.getToken(), idPaciente,
                        new ApiService.ChatRequest(textoPaciente, contexto, historial))
                .enqueue(new Callback<ApiService.ChatResponse>() {
                    @Override
                    public void onResponse(Call<ApiService.ChatResponse> call, Response<ApiService.ChatResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String respuestaHablada = response.body().respuesta;
                            String accionInvocada = response.body().accion;

                            mostrarYHablarRespuesta(respuestaHablada);
                            registrarTurno(textoPaciente, respuestaHablada);
                            guardarEnBitacora("INTERACCIÓN IA", "IA: " + textoPaciente + " → " + respuestaHablada);

                            if (accionInvocada != null && !accionInvocada.trim().isEmpty()) {
                                procesarAccion(accionInvocada.trim());
                            }
                        } else if (response.code() == 401) {
                            resolverRespuestaIA(getString(R.string.session_expired));
                        } else {
                            resolverRespuestaIA(getString(R.string.chat_error, response.code()));
                        }
                    }
                    @Override public void onFailure(Call<ApiService.ChatResponse> call, Throwable t) {
                        resolverRespuestaIA(getString(R.string.chat_no_connection));
                    }
                });
    }

    private void registrarTurno(String mensajePaciente, String respuestaIA) {
        historialConversacion.add("Paciente: " + mensajePaciente);
        historialConversacion.add("Alora: " + respuestaIA);
        while (historialConversacion.size() > MAX_TURNOS_HISTORIAL * 2) {
            historialConversacion.remove(0);
        }
    }

    // ---------- Acciones ----------

    private void procesarAccion(String accion) {
        String accionUpper = accion.toUpperCase();

        if (accionUpper.startsWith("POSPONER") && idRecordatorioActual != null) {
            int minutos = 30;
            if (accionUpper.contains(":")) {
                try {
                    minutos = Integer.parseInt(accionUpper.split(":")[1].trim());
                } catch (NumberFormatException ignored) {}
            }
            if (minutos < 1) minutos = 30;
            guardarEnBitacora("CONTROL", "Pospuesto " + minutos + " min: " + tituloRecordatorioActual);
            posponerRecordatorio(idRecordatorioActual, tituloRecordatorioActual, minutos);
        } else if (accionUpper.equals("COMPLETADO") && idRecordatorioActual != null) {
            guardarEnBitacora("MEDICACION", "Completado: " + tituloRecordatorioActual);
            borrarRecordatorio(idRecordatorioActual, getString(R.string.completed_logged));
        } else if (accionUpper.startsWith("CREAR_RECORDATORIO:")) {
            parsearYCrearRecordatorio(accion);
        }
    }

    private void parsearYCrearRecordatorio(String accion) {
        String datos = accion.substring("CREAR_RECORDATORIO:".length());
        String[] partes = datos.split(";");
        if (partes.length < 2) {
            Toast.makeText(this, getString(R.string.reminder_parse_error), Toast.LENGTH_SHORT).show();
            return;
        }
        String titulo = partes[0].trim();
        String hora = partes[1].trim();
        String dias = partes.length > 2 ? partes[2].trim() : "TODOS";

        ApiService api = ApiClient.getClient().create(ApiService.class);
        Reminder nuevo = new Reminder(titulo, hora, dias);
        api.createReminder("Bearer " + tokenManager.getToken(), idPaciente, nuevo).enqueue(new Callback<Reminder>() {
            @Override
            public void onResponse(Call<Reminder> call, Response<Reminder> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Reminder guardado = response.body();
                    AlarmHelper.programarAlarma(AssistantActivity.this, guardado.getId(), idPaciente, titulo, hora, dias);
                    Toast.makeText(AssistantActivity.this,
                            getString(R.string.reminder_created, titulo, hora.substring(0, 5)),
                            Toast.LENGTH_LONG).show();
                    cargarRecordatoriosParaLaIA();
                } else {
                    Toast.makeText(AssistantActivity.this, getString(R.string.reminder_create_error), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Reminder> call, Throwable t) {
                Toast.makeText(AssistantActivity.this, getString(R.string.reminder_create_offline), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void borrarRecordatorio(Long idRec, String mensajeExito) {
        AlarmHelper.cancelarAlarma(this, idRec);
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.deleteReminder("Bearer " + tokenManager.getToken(), idPaciente, idRec).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AssistantActivity.this, mensajeExito, Toast.LENGTH_SHORT).show();
                    idRecordatorioActual = null;
                    tituloRecordatorioActual = "";
                    cargarRecordatoriosParaLaIA();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    /**
     * Posponer = editar el recordatorio en el servidor (PUT) con la nueva hora,
     * manteniendo título y días, y reprogramar la alarma local. El cambio queda
     * visible al instante en la lista de alarmas de todos los cuidadores.
     */
    private void posponerRecordatorio(Long idRec, String titulo, int minutos) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, minutos);
        String nuevaHora = String.format(Locale.getDefault(), "%02d:%02d:00",
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
        String horaVisible = nuevaHora.substring(0, 5);
        String dias = diasRecordatorioActual != null ? diasRecordatorioActual : "TODOS";

        Reminder actualizado = new Reminder(titulo, nuevaHora, dias);
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.updateReminder("Bearer " + tokenManager.getToken(), idPaciente, idRec, actualizado)
                .enqueue(new Callback<Reminder>() {
                    @Override
                    public void onResponse(Call<Reminder> call, Response<Reminder> response) {
                        if (response.isSuccessful()) {
                            AlarmHelper.cancelarAlarma(AssistantActivity.this, idRec);
                            AlarmHelper.programarAlarma(AssistantActivity.this, idRec, idPaciente,
                                    titulo, nuevaHora, dias);
                            Toast.makeText(AssistantActivity.this,
                                    getString(R.string.snoozed_until, horaVisible), Toast.LENGTH_LONG).show();
                            cargarRecordatoriosParaLaIA();
                        } else {
                            // Respaldo local: aunque falle el servidor, el aviso sonará igualmente
                            AlarmHelper.programarSnooze(AssistantActivity.this, idRec, idPaciente, titulo, minutos);
                            Toast.makeText(AssistantActivity.this,
                                    getString(R.string.snoozed_until, horaVisible), Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override public void onFailure(Call<Reminder> call, Throwable t) {
                        AlarmHelper.programarSnooze(AssistantActivity.this, idRec, idPaciente, titulo, minutos);
                        Toast.makeText(AssistantActivity.this,
                                getString(R.string.snoozed_until, horaVisible), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void guardarEnBitacora(String logType, String nota) {
        if (idPaciente == -1) return;
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.createCareLog("Bearer " + tokenManager.getToken(), idPaciente, new CareLog(logType, nota))
                .enqueue(new Callback<CareLog>() {
                    @Override public void onResponse(Call<CareLog> call, Response<CareLog> response) {}
                    @Override public void onFailure(Call<CareLog> call, Throwable t) {}
                });
    }

    // ---------- Voz ----------

    private void configurarReconocedorDeVoz() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, esIngles() ? "en-US" : "es-ES");
        speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                etTextoSimulado.setHint(getString(R.string.listening));
            }
            @Override public void onResults(Bundle results) {
                resultadoRecibido = true;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String texto = matches.get(0);
                    agregarMensajeUsuario(texto);
                    procesarConInteligenciaArtificial(texto);
                }
                detenerEscucha();
            }
            @Override public void onError(int error) {
                if (!resultadoRecibido) {
                    agregarMensajeIA(mensajeErrorVoz(error));
                }
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

    private String mensajeErrorVoz(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return getString(R.string.err_voice_network);
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return getString(R.string.err_voice_permission);
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
            case SpeechRecognizer.ERROR_NO_MATCH:
                return getString(R.string.err_voice_no_match);
            default:
                return getString(R.string.err_voice_generic);
        }
    }

    private void verificarPermisoYEmpezarAEscuchar() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, getString(R.string.speech_unavailable), Toast.LENGTH_LONG).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            iniciarEscucha();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void iniciarEscucha() {
        if (textToSpeech != null) textToSpeech.stop();
        // SpeechRecognizer es de un solo uso en Android: hay que destruir y recrear
        if (speechRecognizer != null) speechRecognizer.destroy();
        resultadoRecibido = false;
        configurarReconocedorDeVoz();
        speechRecognizer.startListening(speechIntent);
        isListening = true;
        fabMic.setImageResource(R.drawable.ic_modern_stop);
    }

    private void detenerEscucha() {
        if (speechRecognizer != null && isListening) {
            try { speechRecognizer.stopListening(); } catch (Exception ignored) {}
        }
        isListening = false;
        etTextoSimulado.setHint(getString(R.string.hint_message));
        fabMic.setImageResource(etTextoSimulado.getText().toString().trim().isEmpty()
                ? R.drawable.ic_modern_mic : R.drawable.ic_modern_send);
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) { textToSpeech.shutdown(); }
        if (speechRecognizer != null) { speechRecognizer.destroy(); }
        super.onDestroy();
    }
}
