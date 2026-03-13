package com.alora.app.model;

import com.google.gson.annotations.SerializedName;

public class CareLog {

    @SerializedName("id")
    private Long id;

    @SerializedName("profileId")
    private Long profileId;

    @SerializedName("logType")
    private String logType; // Ej: "SINTOMAS", "MEDICACION"

    @SerializedName("note")
    private String note;

    @SerializedName("createdAt")
    private String createdAt;

    // Constructor vacío requerido por Gson
    public CareLog() {
    }

    // Constructor para cuando el cuidador crea una nota nueva
    public CareLog(String logType, String note) {
        this.logType = logType;
        this.note = note;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }

    public String getLogType() { return logType; }
    public void setLogType(String logType) { this.logType = logType; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}