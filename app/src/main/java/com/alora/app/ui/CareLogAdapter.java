package com.alora.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alora.app.R;
import com.alora.app.model.CareLog;
import java.util.List;

public class CareLogAdapter extends RecyclerView.Adapter<CareLogAdapter.LogViewHolder> {

    private List<CareLog> logList;

    public CareLogAdapter(List<CareLog> logList) {
        this.logList = logList;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_care_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        CareLog log = logList.get(position);

        holder.tvLogNote.setText(log.getNote());

        // Limpiamos un poco la fecha si viene del servidor (ej: 2026-03-10T15:30:00 -> 2026-03-10)
        String fecha = log.getCreatedAt() != null ? log.getCreatedAt().split("T")[0] : "Fecha desconocida";
        holder.tvLogDate.setText(log.getLogType() + " | " + fecha);
    }

    @Override
    public int getItemCount() {
        return logList != null ? logList.size() : 0;
    }

    public static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvLogDate, tvLogNote;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLogDate = itemView.findViewById(R.id.tvLogDate);
            tvLogNote = itemView.findViewById(R.id.tvLogNote);
        }
    }
}