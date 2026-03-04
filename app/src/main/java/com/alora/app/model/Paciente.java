package com.alora.app.model;

import com.google.gson.annotations.SerializedName;

public class Paciente {

    @SerializedName("fullName")
    private String nombre;

    @SerializedName("city")
    private String ciudad;

    @SerializedName("allergies")
    private String alergias;

    @SerializedName("photoUrl")
    private String foto;

    public Paciente() {}

    public Paciente(String nombre, String ciudad, String alergias) {
        this.nombre = nombre;
        this.ciudad = ciudad;
        this.alergias = alergias;
    }

    public String getNombre() { return nombre; }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCiudad() { return ciudad; }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getAlergias() { return alergias; }

    public void setAlergias(String alergias) {
        this.alergias = alergias;
    }

    public String getFoto() { return foto; }

    public void setFoto(String foto) {
        this.foto = foto;
    }
}