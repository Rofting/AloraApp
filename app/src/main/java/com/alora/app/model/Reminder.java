package com.alora.app.model;

public class Reminder {
    private Long id;
    private String title;
    private String time; // Formato "HH:mm"
    private boolean isActive;

    public Reminder(String title, String time) {
        this.title = title;
        this.time = time;
        this.isActive = true;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}