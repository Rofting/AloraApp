package com.alora.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alora.app.R;
import com.alora.app.model.Paciente;
import com.bumptech.glide.Glide; // Import necesario
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

        // URL de la imagen (Asegúrate que la IP sea accesible desde el emulador/móvil)
        // Si usas emulador estándar de Android, usa 10.0.2.2 en lugar de 192.168...
        String imageUrl = "http://192.168.1.196:8080/images/" + paciente.getFoto();

        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery) // Imagen de carga
                .error(android.R.drawable.stat_notify_error)     // Imagen de error
                .into(holder.ivPaciente);
    }

    @Override
    public int getItemCount() {
        return listaPacientes != null ? listaPacientes.size() : 0;
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