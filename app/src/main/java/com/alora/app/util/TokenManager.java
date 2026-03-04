package com.alora.app.util;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {

    private static final String PREF_NAME = "AloraPrefs";
    private static final String KEY_TOKEN = "token";
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public TokenManager(Context context) {
        // Inicializamos las preferencias en modo privado (solo esta App puede leerlas)
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = prefs.edit();
    }

    //Metodo para guardar el token
    public void saveToken(String token) {
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    //Metodo para obtener el token
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }
}
