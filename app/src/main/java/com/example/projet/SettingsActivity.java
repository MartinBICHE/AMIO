package com.example.projet;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;

/**
 * Activité de configuration
 */
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Ajouter la flèche de retour dans la barre du haut
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setTitle("Paramètres");
        }

        addPreferencesFromResource(R.xml.preferences);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        updateSummary("email");
        updateSummary("heure_debut_semaine");
        updateSummary("heure_fin_semaine");
        updateSummary("heure_debut_nuit");
        updateSummary("heure_fin_nuit");
    }

    // 2. Gérer le clic sur la flèche de retour
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Utilise la méthode standard de retour
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 3. Ajouter une animation de glissement (Optionnel mais recommandé)
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Animation : la page actuelle sort vers la droite
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateSummary(key);
    }

    private void updateSummary(String key) {
        Preference pref = findPreference(key);
        if (pref instanceof EditTextPreference) {
            EditTextPreference editPref = (EditTextPreference) pref;
            String value = editPref.getText();
            if (key.equals("email")) {
                pref.setSummary(value != null && !value.isEmpty() ? value : "Aucune adresse configurée");
            } else {
                pref.setSummary(value != null && !value.isEmpty() ? value + "h" : "Non configuré");
            }
        }
    }
}