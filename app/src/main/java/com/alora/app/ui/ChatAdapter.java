package com.alora.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alora.app.R;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MensajeViewHolder> {

    public static class Mensaje {
        public String texto;
        public final boolean esUsuario;

        public Mensaje(String texto, boolean esUsuario) {
            this.texto = texto;
            this.esUsuario = esUsuario;
        }
    }

    private static final int TIPO_USUARIO = 0;
    private static final int TIPO_IA = 1;

    private final List<Mensaje> mensajes = new ArrayList<>();

    public void agregar(Mensaje mensaje) {
        mensajes.add(mensaje);
        notifyItemInserted(mensajes.size() - 1);
    }

    /** Actualiza el texto del último mensaje (p. ej. "Escribiendo…" → respuesta). */
    public void actualizarUltimo(String texto) {
        if (mensajes.isEmpty()) return;
        mensajes.get(mensajes.size() - 1).texto = texto;
        notifyItemChanged(mensajes.size() - 1);
    }

    public boolean ultimoEsDeIA() {
        return !mensajes.isEmpty() && !mensajes.get(mensajes.size() - 1).esUsuario;
    }

    public int ultimoIndice() {
        return mensajes.size() - 1;
    }

    @Override
    public int getItemViewType(int position) {
        return mensajes.get(position).esUsuario ? TIPO_USUARIO : TIPO_IA;
    }

    @NonNull
    @Override
    public MensajeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TIPO_USUARIO ? R.layout.item_chat_user : R.layout.item_chat_ia;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MensajeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MensajeViewHolder holder, int position) {
        holder.tvMensaje.setText(mensajes.get(position).texto);
    }

    @Override
    public int getItemCount() {
        return mensajes.size();
    }

    static class MensajeViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMensaje;

        MensajeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMensaje = itemView.findViewById(R.id.tvMensaje);
        }
    }
}
