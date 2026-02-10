package com.example.projet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.cardview.widget.CardView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private ListView listSensors;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Switch switchFilter;
    private ToggleButton toggleButton;
    private FloatingActionButton fabSettings; // Nouveau bouton rond

    private SensorAdapter adapter;
    private ArrayList<JSONObject> allSensorsList = new ArrayList<>();
    private ArrayList<JSONObject> displayedList = new ArrayList<>();

    private static final double SEUIL_LUMIERE = 200.0;
    private static final String URL_IOT_ALL = "http://iotlab.telecomnancy.eu:8080/iotlab/rest/data/1/light1/last";

    private BroadcastReceiver serviceReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Initialisation des Vues ---
        tvStatus = findViewById(R.id.tv_status);
        listSensors = findViewById(R.id.list_sensors);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        switchFilter = findViewById(R.id.switch_filter);
        toggleButton = findViewById(R.id.toggle_service);
        fabSettings = findViewById(R.id.fab_settings); // Liaison du FAB

        // --- Configuration de l'Adapter ---
        adapter = new SensorAdapter(this, displayedList);
        listSensors.setAdapter(adapter);

        // --- Listeners ---
        swipeRefreshLayout.setOnRefreshListener(this::recupererDonnees);

        switchFilter.setOnCheckedChangeListener((buttonView, isChecked) -> mettreAJourListeFiltree());

        listSensors.setOnItemClickListener((parent, view, position, id) -> {
            JSONObject sensor = displayedList.get(position);
            afficherDetails(sensor);
        });

        // Toggle du service (Foreground Service pour Android 14)
        toggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Intent intent = new Intent(MainActivity.this, MainService.class);
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
                Toast.makeText(this, "Surveillance activée", Toast.LENGTH_SHORT).show();
            } else {
                stopService(intent);
                Toast.makeText(this, "Surveillance arrêtée", Toast.LENGTH_SHORT).show();
            }
        });

        // ACTION DU BOUTON ROND (RÉGLAGES)
        fabSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        setupBroadcastReceiver();

        // Premier chargement
        swipeRefreshLayout.setRefreshing(true);
        recupererDonnees();
    }

    private void setupBroadcastReceiver() {
        serviceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (MainService.ACTION_RESULT.equals(intent.getAction())) {
                    String jsonData = intent.getStringExtra("data");
                    if (jsonData != null) {
                        traiterReponseJSON(jsonData);
                    }
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(MainService.ACTION_RESULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(serviceReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(serviceReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(serviceReceiver);
        } catch (Exception e) { /* Ignoré */ }
    }

    // --- RÉSEAU ET DONNÉES ---

    private void recupererDonnees() {
        new Thread(() -> {
            try {
                URL url = new URL(URL_IOT_ALL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) content.append(line);
                in.close();

                runOnUiThread(() -> {
                    traiterReponseJSON(content.toString());
                    swipeRefreshLayout.setRefreshing(false);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvStatus.setText("Erreur réseau");
                    swipeRefreshLayout.setRefreshing(false);
                });
            }
        }).start();
    }

    private void traiterReponseJSON(String jsonResponse) {
        try {
            JSONObject root = new JSONObject(jsonResponse);
            JSONArray dataArray = root.optJSONArray("data");
            if (dataArray == null) dataArray = root.optJSONArray("motes");

            if (dataArray != null) {
                allSensorsList.clear();
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject item = dataArray.getJSONObject(i);
                    if (item.optString("label", "").contains("light")) {
                        allSensorsList.add(item);
                    }
                }
                mettreAJourListeFiltree();
                tvStatus.setText("Maj : " + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mettreAJourListeFiltree() {
        displayedList.clear();
        boolean filtre = switchFilter.isChecked();
        for (JSONObject s : allSensorsList) {
            if (!filtre || s.optDouble("value", 0.0) > SEUIL_LUMIERE) {
                displayedList.add(s);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void afficherDetails(JSONObject sensor) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_sensor_details, null);
        dialog.setContentView(view);

        TextView tvTitle = view.findViewById(R.id.detail_title);
        TextView tvValue = view.findViewById(R.id.detail_value_big);
        TextView tvStatusText = view.findViewById(R.id.detail_status_text);

        double val = sensor.optDouble("value", 0.0);
        tvTitle.setText("Capteur " + sensor.optString("mote", "?"));
        tvValue.setText(String.format(Locale.getDefault(), "%.1f Lux", val));

        if (val > SEUIL_LUMIERE) {
            tvStatusText.setText("État : ALLUMÉ 💡");
            tvStatusText.setTextColor(ContextCompat.getColor(this, R.color.state_on));
        } else {
            tvStatusText.setText("État : Éteint 🌑");
            tvStatusText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }

        dialog.show();
    }

    // --- ADAPTER ---

    private class SensorAdapter extends ArrayAdapter<JSONObject> {
        public SensorAdapter(Context context, List<JSONObject> sensors) {
            super(context, 0, sensors);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_sensor, parent, false);
            }

            JSONObject sensor = getItem(position);
            CardView card = convertView.findViewById(R.id.sensor_status_card);
            TextView name = convertView.findViewById(R.id.sensor_name);
            TextView valTxt = convertView.findViewById(R.id.sensor_value);

            double val = sensor.optDouble("value", 0.0);
            name.setText("Capteur " + sensor.optString("mote", ""));
            valTxt.setText(String.format(Locale.getDefault(), "%.0f Lux", val));

            int color = (val > SEUIL_LUMIERE) ? R.color.state_on : R.color.state_off;
            card.setCardBackgroundColor(ContextCompat.getColor(getContext(), color));

            return convertView;
        }
    }
}