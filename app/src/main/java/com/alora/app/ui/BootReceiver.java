package com.alora.app.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.alora.app.util.AlarmReschedulerWorker;
import com.alora.app.util.TokenManager;

import java.util.concurrent.TimeUnit;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        if (new TokenManager(context).getToken() == null) return;

        // Esperar 30s a que la red esté disponible antes de reprogramar alarmas
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(AlarmReschedulerWorker.class)
                .setInitialDelay(30, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(context).enqueue(workRequest);
    }
}
