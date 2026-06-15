package com.alora.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alora.app.R;
import com.alora.app.model.Reminder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    private final List<Reminder> reminders;
    private final OnReminderClickListener listener;

    public interface OnReminderClickListener {
        void onEditClick(Reminder reminder);
        void onDeleteClick(Reminder reminder);
        void onToggleActive(Reminder reminder, boolean activo);
    }

    public ReminderAdapter(List<Reminder> reminders, OnReminderClickListener listener) {
        this.reminders = reminders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        Reminder reminder = reminders.get(position);
        Context ctx = holder.itemView.getContext();

        holder.tvTitulo.setText(reminder.getTitle());

        if (reminder.getTime() != null && reminder.getTime().length() >= 5) {
            holder.tvHora.setText(reminder.getTime().substring(0, 5));
        } else {
            holder.tvHora.setText(reminder.getTime());
        }

        holder.tvDias.setText(diasLegibles(ctx, reminder.getDaysOfWeek()));

        // Switch sin disparar el listener durante el reciclado
        holder.swActivo.setOnCheckedChangeListener(null);
        holder.swActivo.setChecked(reminder.isActive());
        aplicarOpacidad(holder, reminder.isActive());
        holder.swActivo.setOnCheckedChangeListener((btn, activo) -> {
            aplicarOpacidad(holder, activo);
            listener.onToggleActive(reminder, activo);
        });

        holder.btnEditar.setOnClickListener(v -> listener.onEditClick(reminder));
        holder.btnBorrar.setOnClickListener(v -> listener.onDeleteClick(reminder));
    }

    private void aplicarOpacidad(ReminderViewHolder holder, boolean activo) {
        float alpha = activo ? 1f : 0.45f;
        holder.tvHora.setAlpha(alpha);
        holder.tvTitulo.setAlpha(alpha);
        holder.tvDias.setAlpha(alpha);
    }

    /** Convierte la pauta "LUNES,MIERCOLES" en texto localizado. */
    private String diasLegibles(Context ctx, String pauta) {
        if (pauta == null || pauta.trim().isEmpty()
                || pauta.equalsIgnoreCase("TODOS") || pauta.equalsIgnoreCase("EVERYDAY")) {
            return ctx.getString(R.string.every_day);
        }
        List<String> nombres = new ArrayList<>();
        for (String dia : pauta.toUpperCase().split(",")) {
            switch (dia.trim()) {
                case "LUNES": nombres.add(ctx.getString(R.string.day_monday)); break;
                case "MARTES": nombres.add(ctx.getString(R.string.day_tuesday)); break;
                case "MIERCOLES": case "MIÉRCOLES": nombres.add(ctx.getString(R.string.day_wednesday)); break;
                case "JUEVES": nombres.add(ctx.getString(R.string.day_thursday)); break;
                case "VIERNES": nombres.add(ctx.getString(R.string.day_friday)); break;
                case "SABADO": case "SÁBADO": nombres.add(ctx.getString(R.string.day_saturday)); break;
                case "DOMINGO": nombres.add(ctx.getString(R.string.day_sunday)); break;
            }
        }
        return nombres.isEmpty() ? ctx.getString(R.string.every_day) : String.join(", ", nombres);
    }

    @Override
    public int getItemCount() {
        return reminders != null ? reminders.size() : 0;
    }

    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvHora, tvDias;
        ImageView btnEditar, btnBorrar;
        SwitchMaterial swActivo;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvHora = itemView.findViewById(R.id.tvHora);
            tvDias = itemView.findViewById(R.id.tvDiasRecordatorio);
            btnEditar = itemView.findViewById(R.id.btnEditarRecordatorio);
            btnBorrar = itemView.findViewById(R.id.btnBorrarRecordatorio);
            swActivo = itemView.findViewById(R.id.swActivo);
        }
    }
}
