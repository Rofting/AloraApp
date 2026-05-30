package com.alora.app;

import android.app.Application;
import com.alora.app.api.ApiClient;

public class AloraApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ApiClient.init(this);
    }
}
