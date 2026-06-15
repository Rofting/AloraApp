package com.alora.app.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alora.app.R;
import com.alora.app.model.CareLog;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CareLogAdapter extends RecyclerView.Adapter<CareLogAdapter.LogViewHolder> {

    private List<CareLog> logList;
    private final OnLogItemLongClickListener longClickListener;

    public interface OnLogItemLongClickListener {
        void onEditLog(CareLog log);
        void onDeleteLog(CareLog log);
    }

    public CareLogAdapter(List<CareLog> logList, OnLogItemLongClickListener longClickListener) {
        this.logList = logList;
        this.longClickListener = longClickListener;
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
        holder.tvLogNote.setText(log.getNote() != null ? log.getNote() : "(Sin nota)");

        String fecha = formatearFecha(log.getCreatedAt());
        String tipo = log.getLogType() != null ? log.getLogType() : "GENERAL";
        holder.tvLogDate.setText(tipo + " | " + fecha);
        holder.tvLogDate.setTextColor(colorParaTipo(tipo));

        holder.itemView.setOnLongClickListener(v -> {
            showPopupMenu(v, log);
            return true;
        });
    }

    private String formatearFecha(String raw) {
        if (raw == null) return "—";
        try {
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat display = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            Date date = iso.parse(raw);
            return date != null ? display.format(date) : raw.split("T")[0];
        } catch (Exception e) {
            return raw.split("T")[0];
        }
    }

    private int colorParaTipo(String tipo) {
        switch (tipo) {
            case "MEDICACION":     return Color.parseColor("#EF4444");
            case "SINTOMAS":       return Color.parseColor("#F59E0B");
            case "COMIDA":         return Color.parseColor("#10B981");
            case "CONTROL":        return Color.parseColor("#3B82F6");
            case "INTERACCIÓN IA": return Color.parseColor("#8B5CF6");
            default:               return Color.parseColor("#6B7280");
        }
    }

    private void showPopupMenu(View view, CareLog log) {
        PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        String editar = view.getContext().getString(com.alora.app.R.string.edit);
        String eliminar = view.getContext().getString(com.alora.app.R.string.remove);
        popupMenu.getMenu().add(editar);
        popupMenu.getMenu().add(eliminar);
        popupMenu.setOnMenuItemClickListener(item -> {
            if (editar.equals(item.getTitle().toString())) {
                longClickListener.onEditLog(log);
                return true;
            } else if (eliminar.equals(item.getTitle().toString())) {
                longClickListener.onDeleteLog(log);
                return true;
            }
            return false;
        });
        popupMenu.show();
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
