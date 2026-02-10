package com.example.projet; // Vérifie que c'est bien ton package

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import java.util.Timer;
import java.util.TimerTask;

public class MainService extends Service {
    private Timer timer;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MainService", "Service created");
        timer = new Timer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("MainService", "Service started");

        // Tâche périodique (Exercice 2 Q2)
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Log.d("MainService", "Tâche périodique exécutée...");
                // Plus tard, on mettra ici la requête HTTP vers l'IoT Lab
            }
        }, 0, 5000); // Exécution toutes les 5 secondes

        return START_STICKY; // Redémarrage auto si tué [cite: 39]
    }

    @Override
    public void onDestroy() {
        Log.d("MainService", "Service destroyed");
        if (timer != null) {
            timer.cancel(); // Arrêt du timer [cite: 48]
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Pas nécessaire pour l'instant
    }
}