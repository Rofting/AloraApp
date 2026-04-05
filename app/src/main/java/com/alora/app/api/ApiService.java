package com.alora.app.api;

import com.alora.app.model.CareLog;
import com.alora.app.model.LoginRequest;
import com.alora.app.model.LoginResponse;
import com.alora.app.model.Paciente;
import com.alora.app.model.Reminder;
import com.alora.app.ui.RegisterActivity;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @POST("auth/register")
    Call<LoginResponse> register(@Body RegisterActivity.RegisterRequest registerRequest);

    @GET("api/profiles")
    Call<List<Paciente>> getPacientes(@Header("Authorization") String token);

    @POST("api/profiles")
    Call<Paciente> crearPaciente(@Header("Authorization") String authHeader, @Body Paciente paciente);

    @PUT("api/profiles/{id}")
    Call<Paciente> updatePaciente(
            @Header("Authorization") String authHeader,
            @Path("id") Long id,
            @Body Paciente paciente
    );

    @DELETE("api/profiles/{id}")
    Call<Void> borrarPaciente(@Header("Authorization") String authHeader, @Path("id") Long id);

    @Multipart
    @POST("api/profiles/{id}/photo")
    Call<String> subirFoto(
            @Header("Authorization") String token,
            @Path("id") Long id,
            @Part MultipartBody.Part file
    );

    @GET("api/profiles/{profileId}/logs")
    Call<List<CareLog>> getCareLogs(@Header("Authorization") String token, @Path("profileId") Long profileId);

    @POST("api/profiles/{profileId}/logs")
    Call<CareLog> createCareLog(@Header("Authorization") String token, @Path("profileId") Long profileId, @Body CareLog log);

    @GET("api/profiles/{profileId}/reminders")
    Call<List<Reminder>> getReminders(@Header("Authorization") String token, @Path("profileId") Long profileId);

    @POST("api/profiles/{profileId}/reminders")
    Call<Reminder> createReminder(@Header("Authorization") String token, @Path("profileId") Long profileId, @Body Reminder reminder);

    @DELETE("api/profiles/{profileId}/reminders/{reminderId}")
    Call<Void> deleteReminder(@Header("Authorization") String token, @Path("profileId") Long profileId, @Path("reminderId") Long id);

    @PUT("api/profiles/{profileId}/logs/{logId}")
    Call<CareLog> updateCareLog(
            @Header("Authorization") String token,
            @Path("profileId") Long profileId,
            @Path("logId") Long logId,
            @Body CareLog log
    );

    @DELETE("api/profiles/{profileId}/logs/{logId}")
    Call<Void> deleteCareLog(
            @Header("Authorization") String token,
            @Path("profileId") Long profileId,
            @Path("logId") Long logId
    );
}