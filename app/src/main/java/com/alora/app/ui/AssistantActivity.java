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
  import com.alora.app.util.AlarmHelper;
  import com.alora.app.util.TokenManager;
  import com.google.android.material.floatingactionbutton.FloatingActionButton;

  import java.util.ArrayList;
  import java.util.Calendar;
  import java.util.List;
  import java.util.Locale;

  import retrofit2.Call;
  import retrofit2.Callback;
  import retrofit2.Response;

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
      private Long idRecordatorioActual = null;
      private String tituloRecordatorioActual = "";

      private static final String INSTRUCCIONES_ACCIONES =
              "\n\nACCIONES DISPONIBLES (usa el campo 'accion' cuando sea apropiado):\n" +
              "- COMPLETADO: el paciente confirma que tomó la medicación, realizó la tarea, o dice que ya lo
  hizo\n" +
              "- POSPONER: el paciente no puede hacerlo ahora (ej: 'no estoy en casa', 'estoy acostada', 'ahora
  no puedo', 'en un rato')\n" +
              "- CREAR_RECORDATORIO:Título;HH:MM:00;DIAS: cuando el paciente pide crear o programar un
  recordatorio. " +
              "DIAS opciones: TODOS, LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO (separados por
  coma si son varios).\n" +
              "Ejemplo de accion para crear: CREAR_RECORDATORIO:Pastilla
  azul;10:00:00;LUNES,MIERCOLES,VIERNES\n" +
              "Si el paciente solo dice la hora sin días concretos, usa TODOS.";

      private final ActivityResultLauncher<String> requestPermissionLauncher =
              registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                  if (isGranted) iniciarEscucha();
              });

      @Override
      protected void onCreate(Bundle savedInstanceState) {
          super.onCreate(savedInstanceState);
          setContentView(R.layout.activity_assistant);

          tvTextoPaciente = findViewById(R.id.tvTextoPaciente);
          tvRespuestaIA = findViewById(R.id.tvRespuestaIA);
          fabMic = findViewById(R.id.fabMic);
          etTextoSimulado = findViewById(R.id.etTextoSimulado);
          findViewById(R.id.btnBack).setOnClickListener(v -> finish());

          idPaciente = getIntent().getLongExtra("EXTRA_ID", -1);
          tokenManager = new TokenManager(this);

          idRecordatorioActual = getIntent().getLongExtra("EXTRA_RECORDATORIO_ID", -1L);
          if (idRecordatorioActual == -1L) idRecordatorioActual = null;
          tituloRecordatorioActual = getIntent().getStringExtra("EXTRA_RECORDATORIO_TITULO");
          if (tituloRecordatorioActual == null) tituloRecordatorioActual = "";

          textToSpeech = new TextToSpeech(this, status -> {
              if (status == TextToSpeech.SUCCESS) {
                  textToSpeech.setLanguage(new Locale("es", "ES"));
                  if (idRecordatorioActual != null && !tituloRecordatorioActual.isEmpty()) {
                      ejecutarAvisoProactivoDeAlarma(tituloRecordatorioActual);
                  }
              }
          });

          cargarRecordatoriosParaLaIA();
          configurarReconocedorDeVoz();

          etTextoSimulado.addTextChangedListener(new TextWatcher() {
              @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
              @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                  if (s.toString().trim().isEmpty()) {
                      fabMic.setImageResource(android.R.drawable.ic_btn_speak_now);
                  } else {
                      fabMic.setImageResource(android.R.drawable.ic_menu_send);
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

      private void enviarTextoEscrito() {
          String texto = etTextoSimulado.getText().toString().trim();
          if (!texto.isEmpty()) {
              tvTextoPaciente.setText(texto);
              etTextoSimulado.setText("");
              procesarConInteligenciaArtificial(texto);
          }
      }

      private void ejecutarAvisoProactivoDeAlarma(String tituloAlarma) {
          tvTextoPaciente.setText("[Alarma: " + tituloAlarma + "]");
          procesarConInteligenciaArtificial(
                  "ALERTA DE ALARMA ACTIVA: Es la hora de: " + tituloAlarma +
                  ". Pregunta al paciente si se va a tomar/hacer ahora o si necesita posponerlo."
          );
      }

      private void cargarRecordatoriosParaLaIA() {
          if (idPaciente == -1) return;
          ApiService api = ApiClient.getClient().create(ApiService.class);
          api.getReminders("Bearer " + tokenManager.getToken(), idPaciente).enqueue(new
  Callback<List<Reminder>>() {
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
                                  primero = true;
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
          tvRespuestaIA.setText("Escribiendo...");
          ApiService api = ApiClient.getClient().create(ApiService.class);

          String mensajeConContexto =
                  "Contexto médico actual:\n" + memoriaRecordatorios +
                  "\nID Alarma en Foco: " + (idRecordatorioActual != null ? idRecordatorioActual : "Ninguno") +
                  "\nTítulo Alarma en Foco: " + (tituloRecordatorioActual.isEmpty() ? "Ninguno" :
  tituloRecordatorioActual) +
                  INSTRUCCIONES_ACCIONES +
                  "\nEntrada del paciente: " + textoPaciente;

          api.enviarMensajeIA("Bearer " + tokenManager.getToken(), idPaciente, new
  ApiService.ChatRequest(mensajeConContexto))
                  .enqueue(new Callback<ApiService.ChatResponse>() {
                      @Override
                      public void onResponse(Call<ApiService.ChatResponse> call,
  Response<ApiService.ChatResponse> response) {
                          if (response.isSuccessful() && response.body() != null) {
                              String respuestaHablada = response.body().respuesta;
                              String accionInvocada = response.body().accion;

                              tvRespuestaIA.setText(respuestaHablada);
                              if (textToSpeech != null)
                                  textToSpeech.speak(respuestaHablada, TextToSpeech.QUEUE_FLUSH, null, null);

                              guardarEnBitacora("IA: " + textoPaciente + " → " + respuestaHablada);

                              if (accionInvocada != null && !accionInvocada.trim().isEmpty()) {
                                  procesarAccion(accionInvocada.trim());
                              }
                          }
                      }
                      @Override public void onFailure(Call<ApiService.ChatResponse> call, Throwable t) {
                          tvRespuestaIA.setText("Sin conexión. Inténtalo de nuevo.");
                      }
                  });
      }

      private void procesarAccion(String accion) {
          String accionUpper = accion.toUpperCase();

          if (accionUpper.equals("POSPONER") && idRecordatorioActual != null) {
              posponerRecordatorio(idRecordatorioActual, tituloRecordatorioActual);
          } else if (accionUpper.equals("COMPLETADO") && idRecordatorioActual != null) {
              borrarRecordatorio(idRecordatorioActual, "¡Bien hecho! Hito registrado en la bitácora.");
          } else if (accionUpper.startsWith("CREAR_RECORDATORIO:")) {
              parsearYCrearRecordatorio(accion);
          }
      }

      private void parsearYCrearRecordatorio(String accion) {
          String datos = accion.substring("CREAR_RECORDATORIO:".length());
          String[] partes = datos.split(";");
          if (partes.length < 2) {
              Toast.makeText(this, "No pude interpretar el recordatorio, inténtalo de nuevo",
  Toast.LENGTH_SHORT).show();
              return;
          }
          String titulo = partes[0].trim();
          String hora = partes[1].trim();
          String dias = partes.length > 2 ? partes[2].trim() : "TODOS";

          ApiService api = ApiClient.getClient().create(ApiService.class);
          Reminder nuevo = new Reminder(titulo, hora, dias);
          api.createReminder("Bearer " + tokenManager.getToken(), idPaciente, nuevo).enqueue(new
  Callback<Reminder>() {
              @Override
              public void onResponse(Call<Reminder> call, Response<Reminder> response) {
                  if (response.isSuccessful() && response.body() != null) {
                      Reminder guardado = response.body();
                      AlarmHelper.programarAlarma(AssistantActivity.this, guardado.getId(), idPaciente, titulo,
  hora);
                      Toast.makeText(AssistantActivity.this,
                              "Recordatorio \"" + titulo + "\" creado para las " + hora.substring(0, 5),
                              Toast.LENGTH_LONG).show();
                      cargarRecordatoriosParaLaIA();
                  } else {
                      Toast.makeText(AssistantActivity.this, "Error al crear el recordatorio",
  Toast.LENGTH_SHORT).show();
                  }
              }
              @Override public void onFailure(Call<Reminder> call, Throwable t) {
                  Toast.makeText(AssistantActivity.this, "Sin conexión al crear el recordatorio",
  Toast.LENGTH_SHORT).show();
              }
          });
      }

      private void borrarRecordatorio(Long idRec, String mensajeExito) {
          ApiService api = ApiClient.getClient().create(ApiService.class);
          api.deleteReminder("Bearer " + tokenManager.getToken(), idPaciente, idRec).enqueue(new
  Callback<Void>() {
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

      private void posponerRecordatorio(Long idAntiguo, String titulo) {
          borrarRecordatorio(idAntiguo, "Posponiendo 30 minutos...");

          Calendar cal = Calendar.getInstance();
          cal.add(Calendar.MINUTE, 30);
          String nuevaHora = String.format(Locale.getDefault(), "%02d:%02d:00",
                  cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));

          ApiService api = ApiClient.getClient().create(ApiService.class);
          Reminder nuevoRec = new Reminder(titulo, nuevaHora, "TODOS");

          api.createReminder("Bearer " + tokenManager.getToken(), idPaciente, nuevoRec).enqueue(new
  Callback<Reminder>() {
              @Override
              public void onResponse(Call<Reminder> call, Response<Reminder> response) {
                  if (response.isSuccessful() && response.body() != null) {
                      Reminder guardado = response.body();
                      AlarmHelper.programarAlarma(AssistantActivity.this, guardado.getId(), idPaciente, titulo,
  nuevaHora);
                      idRecordatorioActual = guardado.getId();
                      Toast.makeText(AssistantActivity.this,
                              "Pospuesto a las " + nuevaHora.substring(0, 5), Toast.LENGTH_LONG).show();
                      cargarRecordatoriosParaLaIA();
                  } else {
                      Toast.makeText(AssistantActivity.this, "Error al posponer: " + response.code(),
  Toast.LENGTH_SHORT).show();
                  }
              }
              @Override public void onFailure(Call<Reminder> call, Throwable t) {}
          });
      }

      private void guardarEnBitacora(String registro) {
          if (idPaciente == -1) return;
          ApiService api = ApiClient.getClient().create(ApiService.class);
          api.createCareLog("Bearer " + tokenManager.getToken(), idPaciente, new CareLog("INTERACCIÓN IA",
  registro))
                  .enqueue(new Callback<CareLog>() {
                      @Override public void onResponse(Call<CareLog> call, Response<CareLog> response) {}
                      @Override public void onFailure(Call<CareLog> call, Throwable t) {}
                  });
      }

      private void configurarReconocedorDeVoz() {
          speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
          speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
          speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
  RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
          speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");
          speechRecognizer.setRecognitionListener(new RecognitionListener() {
              @Override public void onReadyForSpeech(Bundle params) { tvTextoPaciente.setText("Escuchando...");
  }
              @Override public void onResults(Bundle results) {
                  ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                  if (matches != null && !matches.isEmpty()) {
                      String texto = matches.get(0);
                      tvTextoPaciente.setText(texto);
                      procesarConInteligenciaArtificial(texto);
                  }
                  detenerEscucha();
              }
              @Override public void onError(int error) { detenerEscucha(); }
              @Override public void onBeginningOfSpeech() {}
              @Override public void onRmsChanged(float rmsdB) {}
              @Override public void onBufferReceived(byte[] buffer) {}
              @Override public void onEndOfSpeech() {}
              @Override public void onPartialResults(Bundle partialResults) {}
              @Override public void onEvent(int eventType, Bundle params) {}
          });
      }

      private void verificarPermisoYEmpezarAEscuchar() {
          if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
  PackageManager.PERMISSION_GRANTED) {
              iniciarEscucha();
          } else {
              requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
          }
      }

      private void iniciarEscucha() {
          speechRecognizer.startListening(speechIntent);
          isListening = true;
          fabMic.setImageResource(android.R.drawable.presence_audio_online);
      }

      private void detenerEscucha() {
          speechRecognizer.stopListening();
          isListening = false;
          fabMic.setImageResource(android.R.drawable.ic_btn_speak_now);
      }

      @Override
      protected void onDestroy() {
          if (textToSpeech != null) { textToSpeech.shutdown(); }
          if (speechRecognizer != null) { speechRecognizer.destroy(); }
          super.onDestroy();
      }
  }
