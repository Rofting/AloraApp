package com.alora.app.api;

import com.alora.app.model.CareLog;
import com.alora.app.model.CareLogPage;
import com.alora.app.model.LoginRequest;
import com.alora.app.model.LoginResponse;
import com.alora.app.model.Paciente;
import com.alora.app.model.RegisterRequest;
import com.alora.app.model.Reminder;
import com.alora.app.model.UserInfo;

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
import retrofit2.http.Query;

public interface ApiService {

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @POST("auth/register")
    Call<LoginResponse> register(@Body RegisterRequest request);

    @GET("api/me")
    Call<UserInfo> getMe(@Header("Authorization") String token);

    @GET("api/profiles")
    Call<List<Paciente>> getPacientes(@Header("Authorization") String token);

    @POST("api/profiles")
    Call<Paciente> crearPaciente(@Header("Authorization") String authHeader, @Body Paciente paciente);

    @PUT("api/profiles/{id}")
    Call<Paciente> updatePaciente(@Header("Authorization") String authHeader, @Path("id") Long id, @Body Paciente paciente);

    @DELETE("api/profiles/{id}")
    Call<Void> deletePaciente(@Header("Authorization") String authHeader, @Path("id") Long id);

    @Multipart
    @POST("api/profiles/{id}/photo")
    Call<String> uploadPhoto(
            @Header("Authorization") String token,
            @Path("id") Long id,
            @Part MultipartBody.Part file
    );

    @GET("api/profiles/{profileId}/logs")
    Call<CareLogPage> getCareLogs(
            @Header("Authorization") String token,
            @Path("profileId") Long profileId,
            @Query("page") int page,
            @Query("size") int size
    );

    @POST("api/profiles/{profileId}/logs")
    Call<CareLog> createCareLog(@Header("Authorization") String token, @Path("profileId") Long profileId, @Body CareLog log);

    @GET("api/profiles/{profileId}/reminders")
    Call<List<Reminder>> getReminders(@Header("Authorization") String token, @Path("profileId") Long profileId);

    @POST("api/profiles/{profileId}/reminders")
    Call<Reminder> createReminder(@Header("Authorization") String token, @Path("profileId") Long profileId, @Body Reminder reminder);

    @DELETE("api/profiles/{profileId}/reminders/{reminderId}")
    Call<Void> deleteReminder(@Header("Authorization") String token, @Path("profileId") Long profileId, @Path("reminderId") Long id);

    @PUT("api/profiles/{profileId}/reminders/{reminderId}")
    Call<Reminder> updateReminder(
            @Header("Authorization") String token,
            @Path("profileId") Long profileId,
            @Path("reminderId") Long reminderId,
            @Body Reminder reminder
    );

    @GET("api/profiles/{profileId}/reminders/{reminderId}/speak")
    Call<SpeakResponse> getSpeakText(
            @Header("Authorization") String token,
            @Path("profileId") Long profileId,
            @Path("reminderId") Long reminderId
    );

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

    @POST("api/profiles/{profileId}/chat")
    Call<ChatResponse> enviarMensajeIA(
            @Header("Authorization") String token,
            @Path("profileId") Long profileId,
            @Body ChatRequest request
    );

    @GET("public/profile/{qrToken}")
    Call<PublicProfile> getPublicProfile(@Path("qrToken") String qrToken);

    @POST("public/profile/{qrToken}/unlock")
    Call<PublicProfile> unlockProfile(
            @Path("qrToken") String qrToken,
            @Body UnlockRequest request
    );

    class ChatRequest {
        public String mensaje;
        public String contexto;
        public String historial;
        public ChatRequest(String mensaje) { this.mensaje = mensaje; }
        public ChatRequest(String mensaje, String contexto, String historial) {
            this.mensaje = mensaje;
            this.contexto = contexto;
            this.historial = historial;
        }
    }

    class ChatResponse {
        public String respuesta;
        public String accion;
    }

    class SpeakResponse {
        public String texto;
    }

    class UnlockRequest {
        public String pin;
        public UnlockRequest(String pin) { this.pin = pin; }
    }

    class PublicProfile {
        public Long id;
        public String fullName;
        public String city;
        public String allergies;
        public String medicalConditions;
        public String medications;
        public String emergencyContactName;
        public String relationship;
        public String emergencyContactPhone;
    }
}
