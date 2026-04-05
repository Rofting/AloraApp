package com.alora.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alora.app.R;
import com.alora.app.model.CareLog;
import java.util.List;

public class CareLogAdapter extends RecyclerView.Adapter<CareLogAdapter.LogViewHolder> {

    private List<CareLog> logList;
    private final OnLogItemLongClickListener longClickListener;

    // Interfaz para escuchar clics largos
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

        holder.tvLogNote.setText(log.getNote());

        String fecha = log.getCreatedAt() != null ? log.getCreatedAt().split("T")[0] : "Fecha desconocida";
        holder.tvLogDate.setText(log.getLogType() + " | " + fecha);

        // Configurar clic largo para mostrar menú de editar/eliminar
        holder.itemView.setOnLongClickListener(v -> {
            showPopupMenu(v, log);
            return true;
        });
    }

    private void showPopupMenu(View view, CareLog log) {
        PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        popupMenu.getMenu().add("Editar");
        popupMenu.getMenu().add("Eliminar");

        popupMenu.setOnMenuItemClickListener(item -> {
            if ("Editar".equals(item.getTitle().toString())) {
                longClickListener.onEditLog(log);
                return true;
            } else if ("Eliminar".equals(item.getTitle().toString())) {
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