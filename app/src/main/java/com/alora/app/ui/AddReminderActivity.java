package com.alora.app.ui;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alora.app.R;
import com.alora.app.api.ApiClient;
import com.alora.app.api.ApiService;
import com.alora.app.model.Reminder;
import com.alora.app.util.AlarmHelper;
import com.alora.app.util.TokenManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddReminderActivity extends AppCompatActivity {

    private TextInputEditText etTitulo;
    private TextView tvHoraSeleccionada, tvTituloPantalla;
    private MaterialButton btnGuardar;
    private View cardSeleccionarHora;
    private CheckBox tgLun, tgMar, tgMie, tgJue, tgVie, tgSab, tgDom;

    private TokenManager tokenManager;
    private Long idPaciente;
    private Long idRecordatorio;

    private int horaSelec = 8;
    private int minSelec = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);

        etTitulo = findViewById(R.id.etTituloRecordatorio);
        tvHoraSeleccionada = findViewById(R.id.tvHoraSeleccionada);
        tvTituloPantalla = findViewById(R.id.tvTituloPantalla);
        btnGuardar = findViewById(R.id.btnGuardarRecordatorio);
        cardSeleccionarHora = findViewById(R.id.cardSeleccionarHora);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tgLun = findViewById(R.id.tgLun); tgMar = findViewById(R.id.tgMar);
        tgMie = findViewById(R.id.tgMie); tgJue = findViewById(R.id.tgJue);
        tgVie = findViewById(R.id.tgVie); tgSab = findViewById(R.id.tgSab);
        tgDom = findViewById(R.id.tgDom);

        aplicarAnimacion(tgLun); aplicarAnimacion(tgMar); aplicarAnimacion(tgMie);
        aplicarAnimacion(tgJue); aplicarAnimacion(tgVie); aplicarAnimacion(tgSab); aplicarAnimacion(tgDom);

        tokenManager = new TokenManager(this);

        idPaciente = getIntent().getLongExtra("EXTRA_PACIENTE_ID", -1);
        if (idPaciente == -1) idPaciente = getIntent().getLongExtra("EXTRA_ID", -1);

        idRecordatorio = getIntent().getLongExtra("EXTRA_REMINDER_ID", -1);

        if (idRecordatorio != -1) {
            tvTituloPantalla.setText(getString(R.string.edit_alarm));
            btnGuardar.setText(getString(R.string.update));
            etTitulo.setText(getIntent().getStringExtra("EXTRA_TITULO"));
            marcarDiasEnBotonera(getIntent().getStringExtra("EXTRA_DIAS"));

            String horaIntent = getIntent().getStringExtra("EXTRA_HORA");
            if (horaIntent != null && horaIntent.contains(":")) {
                String[] partes = horaIntent.split(":");
                horaSelec = Integer.parseInt(partes[0]);
                minSelec = Integer.parseInt(partes[1]);
            }
        } else {
            Calendar c = Calendar.getInstance();
            horaSelec = c.get(Calendar.HOUR_OF_DAY);
            minSelec = c.get(Calendar.MINUTE);
        }
        actualizarRelojUI();

        cardSeleccionarHora.setOnClickListener(v ->
                new TimePickerDialog(this, (view, h, m) -> {
                    horaSelec = h;
                    minSelec = m;
                    actualizarRelojUI();
                }, horaSelec, minSelec, true).show()
        );

        btnGuardar.setOnClickListener(v -> {
            String titulo = etTitulo.getText().toString().trim();
            if (titulo.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_write_title), Toast.LENGTH_SHORT).show();
                return;
            }
            String horaBackend = String.format(Locale.getDefault(), "%02d:%02d:00", horaSelec, minSelec);
            String pautaDias = compilarDiasSeleccionados();

            if (idRecordatorio != -1) {
                actualizarEnServidor(titulo, horaBackend, pautaDias);
            } else {
                guardarEnServidor(titulo, horaBackend, pautaDias);
            }
        });
    }

    private void actualizarRelojUI() {
        tvHoraSeleccionada.setText(String.format(Locale.getDefault(), "%02d:%02d", horaSelec, minSelec));
    }

    private void aplicarAnimacion(CheckBox checkBox) {
        checkBox.setOnClickListener(v ->
                v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80).withEndAction(() ->
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                ).start()
        );
    }

    private String compilarDiasSeleccionados() {
        List<String> seleccionados = new ArrayList<>();
        if (tgLun.isChecked()) seleccionados.add("LUNES");
        if (tgMar.isChecked()) seleccionados.add("MARTES");
        if (tgMie.isChecked()) seleccionados.add("MIERCOLES");
        if (tgJue.isChecked()) seleccionados.add("JUEVES");
        if (tgVie.isChecked()) seleccionados.add("VIERNES");
        if (tgSab.isChecked()) seleccionados.add("SABADO");
        if (tgDom.isChecked()) seleccionados.add("DOMINGO");
        if (seleccionados.isEmpty() || seleccionados.size() == 7) return "TODOS";
        return String.join(",", seleccionados);
    }

    private void marcarDiasEnBotonera(String dias) {
        if (dias == null || dias.equalsIgnoreCase("TODOS")) return;
        String cadena = dias.toUpperCase();
        if (cadena.contains("LUNES")) tgLun.setChecked(true);
        if (cadena.contains("MARTES")) tgMar.setChecked(true);
        if (cadena.contains("MIERCOLES")) tgMie.setChecked(true);
        if (cadena.contains("JUEVES")) tgJue.setChecked(true);
        if (cadena.contains("VIERNES")) tgVie.setChecked(true);
        if (cadena.contains("SABADO")) tgSab.setChecked(true);
        if (cadena.contains("DOMINGO")) tgDom.setChecked(true);
    }

    /** Edición real con PUT: conserva el ID del recordatorio (antes se borraba y recreaba). */
    private void actualizarEnServidor(String titulo, String hora, String dias) {
        Reminder actualizado = new Reminder(titulo, hora, dias);
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.updateReminder("Bearer " + tokenManager.getToken(), idPaciente, idRecordatorio, actualizado)
                .enqueue(new Callback<Reminder>() {
                    @Override
                    public void onResponse(Call<Reminder> call, Response<Reminder> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            AlarmHelper.cancelarAlarma(AddReminderActivity.this, idRecordatorio);
                            AlarmHelper.programarAlarma(AddReminderActivity.this,
                                    idRecordatorio, idPaciente, titulo, hora, dias);
                            Toast.makeText(AddReminderActivity.this, getString(R.string.toast_saved), Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(AddReminderActivity.this, getString(R.string.toast_update_error), Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<Reminder> call, Throwable t) {
                        Toast.makeText(AddReminderActivity.this, getString(R.string.toast_connection_error), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void guardarEnServidor(String titulo, String hora, String dias) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.createReminder("Bearer " + tokenManager.getToken(), idPaciente, new Reminder(titulo, hora, dias))
                .enqueue(new Callback<Reminder>() {
                    @Override
                    public void onResponse(Call<Reminder> call, Response<Reminder> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            AlarmHelper.programarAlarma(AddReminderActivity.this,
                                    response.body().getId(), idPaciente, titulo, hora, dias);
                            Toast.makeText(AddReminderActivity.this, getString(R.string.toast_saved), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                    @Override public void onFailure(Call<Reminder> call, Throwable t) {
                        Toast.makeText(AddReminderActivity.this, getString(R.string.toast_connection_error), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
