package com.alora.app.ui;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alora.app.R;
import com.alora.app.model.Paciente;
import com.bumptech.glide.Glide;

import java.util.List;

public class PacienteAdapter extends RecyclerView.Adapter<PacienteAdapter.PacienteViewHolder> {

    private List<Paciente> listaPacientes;

    public PacienteAdapter(List<Paciente> pacientes) {
        this.listaPacientes = pacientes;
    }

    @NonNull
    @Override
    public PacienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_paciente, parent, false);
        return new PacienteViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull PacienteViewHolder holder, int position) {
        Paciente paciente = listaPacientes.get(position);

        holder.tvNombre.setText(paciente.getNombre());
        holder.tvCiudad.setText(paciente.getCiudad());

        // 1️⃣ EVENTO DE CLIC (Solo uno, limpio y directo)
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Viajamos al Panel de Control (PatientDetailActivity)
                Intent intent = new Intent(v.getContext(), PatientDetailActivity.class);

                // Enviamos los datos (ID, Token y Nombre)
                intent.putExtra("EXTRA_ID", paciente.getId());
                intent.putExtra("EXTRA_TOKEN", paciente.getQrToken());
                intent.putExtra("EXTRA_NOMBRE", paciente.getNombre());

                // Iniciamos la pantalla
                v.getContext().startActivity(intent);
            }
        });

        // 2️⃣ CARGA DE LA IMAGEN (Fuera del evento de clic)
        String imageUrl = "http://192.168.1.196:8080/images/" + paciente.getFoto();

        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.stat_notify_error)
                .into(holder.ivPaciente);
    }

    @Override
    public int getItemCount() {
        return listaPacientes != null ? listaPacientes.size() : 0;
    }

    // Métodos para borrar pacientes
    public Paciente getPacienteAt(int position) {
        return listaPacientes.get(position);
    }

    public void removePaciente(int position) {
        listaPacientes.remove(position);
        notifyItemRemoved(position);
    }

    public static class PacienteViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvCiudad;
        ImageView ivPaciente;

        public PacienteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvCiudad = itemView.findViewById(R.id.tvCiudad);
            ivPaciente = itemView.findViewById(R.id.ivPaciente);
        }
    }
}