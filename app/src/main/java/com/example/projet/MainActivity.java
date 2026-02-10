package com.example.projet;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color; // N'est plus très utilisé grâce à colors.xml mais on garde au cas où
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

// IMPORT IMPORTANT POUR LE NOUVEAU POPUP
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.cardview.widget.CardView; // Pour la pastille de couleur
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // Pour récupérer les couleurs proprement
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat; // Pour la date
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private ListView listSensors;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Switch switchFilter;

    private SensorAdapter adapter;
    private ArrayList<JSONObject> allSensorsList = new ArrayList<>();
    private ArrayList<JSONObject> displayedList = new ArrayList<>();

    // SEUIL (Pourra être déplacé dans les paramètres plus tard)
    private static final double SEUIL_LUMIERE = 200.0;
    private static final String URL_IOT_ALL = "http://iotlab.telecomnancy.eu:8080/iotlab/rest/data/1/light1/last";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Init Views ---
        tvStatus = findViewById(R.id.tv_status);
        listSensors = findViewById(R.id.list_sensors);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        switchFilter = findViewById(R.id.switch_filter);
        ToggleButton toggleButton = findViewById(R.id.toggle_service);

        // --- Init Adapter ---
        adapter = new SensorAdapter(this, displayedList);
        listSensors.setAdapter(adapter);

        // --- Listeners ---
        swipeRefreshLayout.setOnRefreshListener(this::recupererDonnees);

        switchFilter.setOnCheckedChangeListener((buttonView, isChecked) -> mettreAJourListeFiltree());

        // Clic sur un item -> Ouvre le nouveau BottomSheet
        listSensors.setOnItemClickListener((parent, view, position, id) -> {
            JSONObject sensor = displayedList.get(position);
            afficherDetails(sensor);
        });

        toggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Intent intent = new Intent(MainActivity.this, MainService.class);
            if (isChecked) {
                startService(intent);
                Toast.makeText(this, "Service démarré", Toast.LENGTH_SHORT).show();
            } else {
                stopService(intent);
                Toast.makeText(this, "Service arrêté", Toast.LENGTH_SHORT).show();
            }
        });

        // Chargement initial
        swipeRefreshLayout.setRefreshing(true); // Montre le chargement au démarrage
        recupererDonnees();
    }

    // --- Logique Réseau (Inchangée) ---
    private void recupererDonnees() {
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

                final String jsonResponse = content.toString();

                runOnUiThread(() -> {
                    traiterReponseJSON(jsonResponse);
                    swipeRefreshLayout.setRefreshing(false);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    tvStatus.setText("Erreur Réseau : " + e.getMessage());
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
                    // Filtre sur les lumières uniquement
                    if (item.optString("label", "").contains("light")) {
                        allSensorsList.add(item);
                    }
                }
                mettreAJourListeFiltree();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                tvStatus.setText("Dernière maj : " + sdf.format(new Date()));
            }
        } catch (Exception e) {
            tvStatus.setText("Erreur JSON");
            e.printStackTrace();
        }
    }

    private void mettreAJourListeFiltree() {
        displayedList.clear();
        boolean filtreActif = switchFilter.isChecked();

        for (JSONObject sensor : allSensorsList) {
            double value = sensor.optDouble("value", 0.0);
            if (filtreActif) {
                if (value > SEUIL_LUMIERE) displayedList.add(sensor);
            } else {
                displayedList.add(sensor);
            }
        }
        adapter.notifyDataSetChanged();
    }

    // --- NOUVELLE MÉTHODE D'AFFICHAGE DES DÉTAILS (BottomSheet) ---
    private void afficherDetails(JSONObject sensor) {
        // 1. Créer le dialogue
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);

        // 2. Charger le nouveau layout stylé
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_sensor_details, null);
        bottomSheetDialog.setContentView(view);

        // 3. Récupérer les vues du layout
        TextView tvTitle = view.findViewById(R.id.detail_title);
        TextView tvValueBig = view.findViewById(R.id.detail_value_big);
        TextView tvStatusText = view.findViewById(R.id.detail_status_text);
        TextView tvType = view.findViewById(R.id.detail_type);
        TextView tvTime = view.findViewById(R.id.detail_time);
        ImageView iconBig = view.findViewById(R.id.detail_icon);

        // 4. Extraire les données
        String mote = sensor.optString("mote", "?");
        String label = sensor.optString("label", "?");
        double value = sensor.optDouble("value", 0.0);
        long timestamp = sensor.optLong("timestamp", System.currentTimeMillis());

        // 5. Formater les données
        tvTitle.setText("Capteur " + mote);
        tvValueBig.setText(String.format(Locale.getDefault(), "%.1f Lux", value));
        tvType.setText("Type : " + label);

        // Formater la date (Timestamp -> Heure lisible)
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy à HH:mm:ss", Locale.getDefault());
        tvTime.setText("Relevé : " + sdf.format(new Date(timestamp)));

        // Gestion de l'état (Couleur et texte)
        if (value > SEUIL_LUMIERE) {
            tvStatusText.setText("État : ALLUMÉ 💡");
            tvStatusText.setTextColor(ContextCompat.getColor(this, R.color.state_on));
            tvValueBig.setTextColor(ContextCompat.getColor(this, R.color.state_on));
        } else {
            tvStatusText.setText("État : Éteint 🌑");
            tvStatusText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvValueBig.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }

        // 6. Afficher
        bottomSheetDialog.show();
    }

    // --- Adapter Modernisé ---
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

            TextView nameView = convertView.findViewById(R.id.sensor_name);
            TextView labelView = convertView.findViewById(R.id.sensor_label);
            TextView valueView = convertView.findViewById(R.id.sensor_value);
            // Utilisation de CardView pour la pastille ronde
            CardView statusCard = convertView.findViewById(R.id.sensor_status_card);

            try {
                String moteId = sensor.optString("mote", "Inconnu");
                String label = sensor.optString("label", "light");
                double value = sensor.optDouble("value", 0.0);

                nameView.setText("Capteur " + moteId);
                labelView.setText(label);
                valueView.setText(String.format(Locale.getDefault(), "%.0f Lux", value)); // %.0f pour pas de décimale dans la liste

                if (value > SEUIL_LUMIERE) {
                    // Allumé : Jaune
                    statusCard.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.state_on));
                    valueView.setTextColor(ContextCompat.getColor(getContext(), R.color.state_on));
                } else {
                    // Éteint : Gris
                    statusCard.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.state_off));
                    valueView.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
                }

            } catch (Exception e) { e.printStackTrace(); }

            return convertView;
        }
    }
}