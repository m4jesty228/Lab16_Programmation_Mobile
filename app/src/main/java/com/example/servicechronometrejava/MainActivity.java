package com.example.servicechronometrejava;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity — Interface utilisateur du chronomètre.
 *
 * Le service tourne en arrière-plan et incrémente le temps.
 * L'Activity utilise un Handler pour interroger le service
 * toutes les secondes et mettre à jour l'affichage (UI thread).
 *
 * Sans ce Handler, le chrono tourne mais l'écran reste figé sur 00:00.
 */
public class MainActivity extends AppCompatActivity {

    // Vues
    private TextView affichageTemps;
    private Button boutonDemarrer, boutonArreter;

    // Bound Service
    private ChronometreService serviceChronos;
    private boolean estLie = false;

    /**
     * Handler + Runnable — Rafraîchit l'affichage toutes les secondes.
     *
     * Fonctionne sur le UI thread (Looper.getMainLooper()) donc
     * on peut modifier les vues directement sans runOnUiThread().
     * postDelayed() replanifie le Runnable après chaque exécution
     * tant que le chrono tourne.
     */
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable mettreAJourAffichage = new Runnable() {
        @Override
        public void run() {
            // Vérifie que le service est connecté avant de lire le temps
            if (estLie && serviceChronos != null) {
                // Récupère le temps formaté depuis le service et l'affiche
                affichageTemps.setText(serviceChronos.getTempsFormate());
            }
            // Replanifie dans 1 seconde — crée une boucle d'actualisation
            handler.postDelayed(this, 1000);
        }
    };

    // ServiceConnection
    private final ServiceConnection connexionService = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName nom, IBinder binder) {
            ChronometreService.ChronoBinder chronoBinder =
                    (ChronometreService.ChronoBinder) binder;
            serviceChronos = chronoBinder.getService();
            estLie = true;

            // Démarre la boucle de rafraîchissement dès la connexion au service
            handler.post(mettreAJourAffichage);

            Toast.makeText(MainActivity.this,
                    "Chronomètre démarré !", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName nom) {
            estLie = false;
            serviceChronos = null;

            // Arrête la boucle si le service se déconnecte inopinément
            handler.removeCallbacks(mettreAJourAffichage);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        affichageTemps = findViewById(R.id.tvTemps);
        boutonDemarrer = findViewById(R.id.btnStart);
        boutonArreter  = findViewById(R.id.btnStop);

        boutonDemarrer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lancerService();
            }
        });

        boutonArreter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                arreterService();
            }
        });
    }

    @Override
    protected void onDestroy() {
        // Stoppe la boucle Handler pour éviter les fuites mémoire
        handler.removeCallbacks(mettreAJourAffichage);

        if (estLie) {
            unbindService(connexionService);
            estLie = false;
        }
        super.onDestroy();
    }

    private void lancerService() {
        Intent intentService = new Intent(this, ChronometreService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intentService);
        } else {
            startService(intentService);
        }

        bindService(intentService, connexionService, Context.BIND_AUTO_CREATE);
    }

    private void arreterService() {
        // Stoppe la boucle d'affichage
        handler.removeCallbacks(mettreAJourAffichage);

        Intent intentStop = new Intent(this, ChronometreService.class);
        intentStop.setAction(ChronometreService.ACTION_STOP);
        stopService(intentStop);

        if (estLie) {
            unbindService(connexionService);
            estLie = false;
            serviceChronos = null;
        }

        affichageTemps.setText("00:00");
        Toast.makeText(this, "Chronomètre arrêté.", Toast.LENGTH_SHORT).show();
    }
}