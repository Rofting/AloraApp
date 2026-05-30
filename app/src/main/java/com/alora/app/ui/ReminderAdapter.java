package com.alora.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alora.app.R;
import com.alora.app.model.Reminder;
import java.util.List;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    private final List<Reminder> reminders;
    private final OnReminderClickListener listener;

    public interface OnReminderClickListener {
        void onEditClick(Reminder reminder);
        void onDeleteClick(Reminder reminder);
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
        holder.tvTitulo.setText(reminder.getTitle());

        // Limpieza de formato de hora seguro (HH:mm)
        if (reminder.getTime() != null && reminder.getTime().length() >= 5) {
            holder.tvHora.setText(reminder.getTime().substring(0, 5));
        } else {
            holder.tvHora.setText(reminder.getTime());
        }

        // Renderizado estilizado de la pauta de repetición semanal
        if (holder.tvDias != null) {
            String pauta = reminder.getDaysOfWeek();
            if (pauta == null || pauta.trim().isEmpty() || pauta.equalsIgnoreCase("TODOS") || pauta.equalsIgnoreCase("EVERYDAY")) {
                holder.tvDias.setText("Todos los días");
                holder.tvDias.setTextColor(android.graphics.Color.parseColor("#94A3B8")); // Gris neutral
            } else {
                String textoLimpio = pauta.toLowerCase().replace(",", ", ");
                // Primera letra en mayúscula por estética de interfaz
                textoLimpio = textoLimpio.substring(0, 1).toUpperCase() + textoLimpio.substring(1);
                holder.tvDias.setText(textoLimpio);
                holder.tvDias.setTextColor(android.graphics.Color.parseColor("#10B981")); // Verde pauta activa
            }
        }

        holder.btnEditar.setOnClickListener(v -> listener.onEditClick(reminder));
        holder.btnBorrar.setOnClickListener(v -> listener.onDeleteClick(reminder));
    }

    @Override
    public int getItemCount() {
        return reminders != null ? reminders.size() : 0;
    }

    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvHora, tvDias;
        ImageView btnEditar, btnBorrar;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvHora = itemView.findViewById(R.id.tvHora);
            tvDias = itemView.findViewById(R.id.tvDiasRecordatorio);
            btnEditar = itemView.findViewById(R.id.btnEditarRecordatorio);
            btnBorrar = itemView.findViewById(R.id.btnBorrarRecordatorio);
        }
    }
}