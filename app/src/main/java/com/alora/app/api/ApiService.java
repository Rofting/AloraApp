package com.alora.app.api;

import com.alora.app.model.CareLog;
import com.alora.app.model.LoginRequest;
import com.alora.app.model.LoginResponse;
import com.alora.app.model.Paciente;
import com.alora.app.ui.RegisterActivity;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

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
    //5. BORRAR UN PACIENTE
    @DELETE("api/profiles/{id}")
    Call<Void> borrarPaciente(@Header("Authorization") String authHeader, @Path("id") Long id);
    // 6. SUBIR FOTO DEL PACIENTE
    @Multipart
    @POST("api/profiles/{id}/photo")
    Call<String> subirFoto(
            @Header("Authorization") String token,
            @Path("id") Long id,
            @Part okhttp3.MultipartBody.Part file
    );

    // 7. OBTENER LA BITÁCORA DE UN PACIENTE
    @GET("api/profiles/{profileId}/logs")
    Call<List<CareLog>> getCareLogs(
            @Header("Authorization") String token,
            @Path("profileId") Long profileId
    );

    // 8. CREAR UNA NUEVA NOTA EN LA BITÁCORA
    @POST("api/profiles/{profileId}/logs")
    Call<CareLog> createCareLog(
            @Header("Authorization") String token,
            @Path("profileId") Long profileId,
            @Body CareLog log
    );

}