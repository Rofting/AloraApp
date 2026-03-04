package com.alora.app.api;

import com.alora.app.model.LoginRequest;
import com.alora.app.model.LoginResponse;
import com.alora.app.model.Paciente;
import com.alora.app.ui.RegisterActivity;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {

    // 1. LOGIN
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    // 2. REGISTER
    @POST("auth/register")
    Call<LoginResponse> register(@Body RegisterActivity.RegisterRequest registerRequest);

    // 3. OBTENER LA LISTA DE PACIENTES

    @GET("api/profiles")
    Call<List<Paciente>> getPacientes(@Header("Authorization") String token);

    // 4. CREAR UN PACIENTE NUEVO
    @POST("api/profiles")
    Call<Paciente> crearPaciente(@Header("Authorization") String authHeader, @Body Paciente paciente);
}