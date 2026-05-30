package com.alora.app.model;

import com.google.gson.annotations.SerializedName;

public class Reminder {
    @SerializedName("id")
    private Long id;

    @SerializedName("title")
    private String title;

    @SerializedName("time")
    private String time;

    @SerializedName("active")
    private boolean isActive;

    @SerializedName("daysOfWeek")
    private String daysOfWeek;

    public Reminder(String title, String time, String daysOfWeek) {
        this.title = title;
        this.time = time;
        this.daysOfWeek = daysOfWeek;
        this.isActive = true;
    }

    public Reminder(String title, String time) {
        this.title = title;
        this.time = time;
        this.daysOfWeek = "TODOS";
        this.isActive = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
    public String getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(String daysOfWeek) { this.daysOfWeek = daysOfWeek; }
}
