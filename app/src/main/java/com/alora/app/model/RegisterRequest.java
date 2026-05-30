package com.alora.app.model;

public class RegisterRequest {
    public String email;
    public String password;
    public String fullName;

    public RegisterRequest(String email, String password, String fullName) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }
}
