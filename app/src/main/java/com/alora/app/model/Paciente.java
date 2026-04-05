package com.alora.app.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "pacientes") // Indica que es una tabla en la BD local
public class Paciente {

    @PrimaryKey // Identificador único para SQLite
    @SerializedName("id")
    private Long id;

    @SerializedName("fullName")
    private String nombre;

    @SerializedName("city")
    private String ciudad;

    @SerializedName("allergies")
    private String alergias;

    @SerializedName("medicalConditions")
    private String condicionesMedicas;

    @SerializedName("medications")
    private String medicamentos;

    @SerializedName("emergencyContactPhone")
    private String telefonoEmergencia;

    @SerializedName("pinCode")
    private String pinCode;

    @SerializedName("photoUrl")
    private String foto;

    @SerializedName("qrToken")
    private String qrToken;

    public Paciente() {}

    // Constructor completo
    public Paciente(String nombre, String ciudad, String alergias, String condicionesMedicas,
                    String medicamentos, String telefonoEmergencia, String pinCode) {
        this.nombre = nombre;
        this.ciudad = ciudad;
        this.alergias = alergias;
        this.condicionesMedicas = condicionesMedicas;
        this.medicamentos = medicamentos;
        this.telefonoEmergencia = telefonoEmergencia;
        this.pinCode = pinCode;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public String getAlergias() { return alergias; }
    public void setAlergias(String alergias) { this.alergias = alergias; }
    public String getCondicionesMedicas() { return condicionesMedicas; }
    public void setCondicionesMedicas(String condicionesMedicas) { this.condicionesMedicas = condicionesMedicas; }
    public String getMedicamentos() { return medicamentos; }
    public void setMedicamentos(String medicamentos) { this.medicamentos = medicamentos; }
    public String getTelefonoEmergencia() { return telefonoEmergencia; }
    public void setTelefonoEmergencia(String telefonoEmergencia) { this.telefonoEmergencia = telefonoEmergencia; }
    public String getPinCode() { return pinCode; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }
    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }
    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }
}