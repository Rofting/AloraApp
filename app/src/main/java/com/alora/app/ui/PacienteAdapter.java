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
import com.alora.app.api.ApiClient;
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

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), PatientDetailActivity.class);
            intent.putExtra("EXTRA_ID", paciente.getId());
            intent.putExtra("EXTRA_NOMBRE", paciente.getNombre());
            intent.putExtra("EXTRA_CIUDAD", paciente.getCiudad());
            intent.putExtra("EXTRA_ALERGIAS", paciente.getAlergias());
            intent.putExtra("EXTRA_CONDICIONES", paciente.getCondicionesMedicas());
            intent.putExtra("EXTRA_MEDICAMENTOS", paciente.getMedicamentos());
            intent.putExtra("EXTRA_TELEFONO", paciente.getTelefonoEmergencia());
            intent.putExtra("EXTRA_PIN", paciente.getPinCode());
            intent.putExtra("EXTRA_FOTO", paciente.getFoto());
            intent.putExtra("EXTRA_TOKEN", paciente.getQrToken());
            v.getContext().startActivity(intent);
        });

        String imageUrl = ApiClient.getImageUrl(paciente.getFoto());
        if (imageUrl == null) {
            Glide.with(holder.itemView.getContext())
                    .load(R.drawable.ic_modern_person)
                    .into(holder.ivPaciente);
        } else {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_modern_person)
                    .error(R.drawable.ic_modern_person)
                    .into(holder.ivPaciente);
        }
    }

    @Override
    public int getItemCount() {
        return listaPacientes != null ? listaPacientes.size() : 0;
    }

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
