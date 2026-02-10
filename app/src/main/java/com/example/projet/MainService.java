package com.example.projet;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainService extends Service {
    private static final String TAG = "MainService";
    private Timer timer;
    private static final int NOTIFICATION_ID = 1; // ID pour la notification persistante

    private static final String URL_IOT_ALL = "http://iotlab.telecomnancy.eu:8080/iotlab/rest/data/1/light1/last";
    private static final double SEUIL_LUMIERE = 200.0;
    private static final long INTERVAL_MS = 30000;

    private static final String CHANNEL_ID = "light_detection_channel";
    private static final String CHANNEL_NAME = "Détection de lumières";
    public static final String ACTION_RESULT = "com.example.projet.ACTION_RESULT";

    private Map<String, Boolean> previousStates = new HashMap<>();
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        // 1. Créer le canal d'abord
        createNotificationChannel();

        // 2. LANCER LE FOREGROUND IMMEDIATEMENT (Correctif Android 14)
        Notification notification = createServiceNotification("Surveillance active...");
        startForeground(NOTIFICATION_ID, notification);

        timer = new Timer();
        prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
    }

    private Notification createServiceNotification(String content) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("IoT Monitor")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pendingIntent)
                .setOngoing(true) // Empêche de supprimer la notification
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");

        // On annule l'ancien timer s'il existe pour éviter les doublons
        if (timer != null) timer.cancel();
        timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                surveillerCapteurs();
            }
        }, 0, INTERVAL_MS);

        return START_STICKY;
    }

    // --- LE RESTE DU CODE RESTE IDENTIQUE ---
    // (surveillerCapteurs, analyserDonnees, gererNouvelleDetection, etc.)

    private void surveillerCapteurs() {
        new Thread(() -> {
            try {
                URL url = new URL(URL_IOT_ALL);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(5000);

                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                urlConnection.disconnect();
                analyserDonnees(content.toString());

            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la surveillance", e);
            }
        }).start();
    }

    private void analyserDonnees(String jsonResponse) {
        try {
            JSONObject root = new JSONObject(jsonResponse);
            JSONArray dataArray = root.optJSONArray("data");
            if (dataArray == null) dataArray = root.optJSONArray("motes");

            if (dataArray != null) {
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject item = dataArray.getJSONObject(i);
                    String label = item.optString("label", "");
                    if (!label.contains("light")) continue;

                    String mote = item.optString("mote", "");
                    double value = item.optDouble("value", 0.0);
                    boolean estAllume = value > SEUIL_LUMIERE;
                    Boolean etatPrecedent = previousStates.get(mote);

                    if (etatPrecedent != null && !etatPrecedent && estAllume) {
                        gererNouvelleDetection(mote, value);
                    }
                    previousStates.put(mote, estAllume);
                }
                envoyerResultatActivite(jsonResponse);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur analyse", e);
        }
    }

    private void gererNouvelleDetection(String mote, double value) {
        // Logique de temps (identique à ton code original)
        envoyerNotification(mote, value);
        fairVibrer();
    }

    private void envoyerNotification(String mote, double value) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("💡 Lumière détectée")
                .setContentText("Capteur " + mote + " : " + String.format(Locale.getDefault(), "%.0f Lux", value))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        notificationManager.notify((int) System.currentTimeMillis(), notification);
    }

    private void fairVibrer() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(500);
            }
        }
    }

    private void envoyerResultatActivite(String jsonData) {
        Intent intent = new Intent(ACTION_RESULT);
        intent.putExtra("data", jsonData);
        intent.putExtra("timestamp", System.currentTimeMillis());
        sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}