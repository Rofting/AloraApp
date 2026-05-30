package com.alora.app.api;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.alora.app.ui.LoginActivity;
import com.alora.app.util.TokenManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    public static final String BASE_URL = "http://10.0.2.2:8080/";
    private static Retrofit retrofit = null;
    private static Context appContext;

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static String getImageUrl(String photoFileName) {
        if (photoFileName == null || photoFileName.trim().isEmpty() || photoFileName.equals("null")) {
            return null;
        }
        return BASE_URL + "images/" + photoFileName;
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(chain -> {
                        Request request = chain.request();
                        okhttp3.Response response = chain.proceed(request);
                        // Solo redirigir a login si es una petición autenticada con 401
                        if (response.code() == 401
                                && request.header("Authorization") != null
                                && appContext != null) {
                            new TokenManager(appContext).clearToken();
                            new Handler(Looper.getMainLooper()).post(() -> {
                                Intent intent = new Intent(appContext, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                appContext.startActivity(intent);
                            });
                        }
                        return response;
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }
}
