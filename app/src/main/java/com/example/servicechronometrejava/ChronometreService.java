package com.example.servicechronometrejava;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ChronometreService — Service principal de l'application.
 *
 * Fonctionne en mode Foreground (obligatoire depuis Android 8.0)
 * pour éviter que le système Android ne le tue en arrière-plan.
 * Utilise un Bound Service pour exposer ses méthodes à l'Activity.
 *
 * Cycle de vie :
 *   onCreate() → onStartCommand() → [tourne en fond] → onDestroy()
 */
public class ChronometreService extends Service {

    // ----------------------------------------------------------------
    // Constantes
    // ----------------------------------------------------------------

    // ID unique de la notification persistante — doit être > 0
    private static final int NOTIF_ID = 2025;

    // ID du canal de notification — chaîne unique par canal
    private static final String CANAL_ID = "canal_chronometre";

    // Action envoyée depuis l'Activity pour stopper proprement le service
    public static final String ACTION_STOP = "ACTION_STOP_CHRONO";

    // ----------------------------------------------------------------
    // État interne
    // ----------------------------------------------------------------

    // Nombre de secondes écoulées depuis le démarrage du chronomètre
    private int tempsEcoule = 0;

    // Indique si le chronomètre est actuellement en cours d'exécution
    private boolean enMarche = false;

    // Exécuteur planifié — incrémente le temps toutes les secondes
    // sur un thread dédié séparé du thread principal (UI thread)
    private ScheduledExecutorService executeur;

    // Gestionnaire système des notifications — mis en cache dans onCreate()
    private NotificationManager gestionnaireNotif;

    // ----------------------------------------------------------------
    // Binder — communication Activity ↔ Service
    // ----------------------------------------------------------------

    // Instance unique du binder, retournée à chaque connexion
    private final IBinder binder = new ChronoBinder();

    /**
     * ChronoBinder — Pont entre l'Activity et ce Service.
     *
     * L'Activity appelle getService() pour obtenir une référence directe
     * au service et ainsi accéder à ses méthodes publiques (getTemps, etc.).
     */
    public class ChronoBinder extends Binder {
        /**
         * Retourne l'instance courante de ChronometreService.
         * Permet à l'Activity d'appeler les méthodes du service directement.
         */
        public ChronometreService getService() {
            return ChronometreService.this;
        }
    }

    // ================================================================
    // Cycle de vie du Service
    // ================================================================

    /**
     * onCreate() — Appelé une seule fois à la création du service.
     *
     * C'est ici qu'on initialise les ressources lourdes qui persistent
     * pendant toute la durée de vie du service.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // Récupère le service système de gestion des notifications
        gestionnaireNotif = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);

        // Crée le canal de notification (requis depuis Android 8.0 / API 26)
        initialiserCanalNotification();
    }

    /**
     * onStartCommand() — Appelé à chaque fois que startService() est invoqué.
     *
     * C'est le point d'entrée principal du service. On y gère :
     *   - L'action STOP pour un arrêt propre
     *   - Le démarrage en mode Foreground avec notification
     *   - Le lancement du chronomètre
     *
     * @return START_STICKY — si le système tue le service, il le redémarre
     *                        automatiquement avec un intent null.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Extrait l'action de l'intent (peut être null si le système redémarre le service)
        String action = (intent != null) ? intent.getAction() : null;

        // Si l'Activity demande l'arrêt → on stoppe proprement
        if (ACTION_STOP.equals(action)) {
            stopSelf(); // déclenche onDestroy()
            return START_NOT_STICKY; // pas besoin de redémarrer dans ce cas
        }

        // Démarre le Foreground Service seulement s'il n'est pas déjà actif
        // (évite de réinitialiser le chrono si onStartCommand est rappelé)
        if (!enMarche) {
            enMarche = true;

            // startForeground() est OBLIGATOIRE depuis Android 8.0
            // Sans cela, le service est tué après quelques secondes en arrière-plan
            startForeground(NOTIF_ID, construireNotification());

            lancerMinuterie();
        }

        // START_STICKY : Android redémarre ce service s'il est tué par manque de RAM
        return START_STICKY;
    }

    /**
     * onBind() — Appelé quand l'Activity se connecte au service via bindService().
     *
     * @return Le binder qui permet à l'Activity d'appeler nos méthodes.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // On retourne notre binder personnalisé pour la communication bidirectionnelle
        return binder;
    }

    /**
     * onDestroy() — Appelé en dernier, juste avant que le service soit détruit.
     *
     * On y libère toutes les ressources : thread, notification, état.
     */
    @Override
    public void onDestroy() {
        enMarche = false;

        // Arrête proprement le thread planifié (évite les fuites mémoire)
        if (executeur != null && !executeur.isShutdown()) {
            executeur.shutdown();
        }

        // Supprime la notification persistante de la barre de statut
        stopForeground(true);

        super.onDestroy();
    }

    // ================================================================
    // Logique interne
    // ================================================================

    /**
     * Lance la minuterie qui incrémente le temps toutes les secondes.
     *
     * ScheduledExecutorService est préféré à Handler/Timer car :
     *   - Thread-safe par conception
     *   - Gestion propre des exceptions
     *   - Pas de fuite mémoire liée au contexte Android
     */
    private void lancerMinuterie() {
        executeur = Executors.newSingleThreadScheduledExecutor();

        executeur.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                // S'exécute sur un thread secondaire — pas de UI ici
                tempsEcoule++;

                // Met à jour la notification avec le nouveau temps
                rafraichirNotification();
            }
        }, 0, 1, TimeUnit.SECONDS); // délai initial = 0s, période = 1s
    }

    /**
     * Crée le canal de notification requis depuis Android 8.0 (API 26).
     *
     * IMPORTANCE_LOW : pas de son, pas de popup — discret mais visible.
     * Un canal ne peut pas être reconfiguré après sa création par l'utilisateur.
     */
    private void initialiserCanalNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_ID,
                    "Chronomètre en arrière-plan",
                    NotificationManager.IMPORTANCE_LOW  // silencieux mais persistant
            );
            canal.setDescription("Affiche le temps écoulé du chronomètre");
            gestionnaireNotif.createNotificationChannel(canal);
        }
    }

    /**
     * Construit et retourne la notification affichée dans la barre de statut.
     *
     * setOngoing(true) : l'utilisateur ne peut pas la faire glisser pour la fermer.
     * C'est ce qui distingue visuellement un Foreground Service d'une notif normale.
     */
    private Notification construireNotification() {
        return new NotificationCompat.Builder(this, CANAL_ID)
                .setContentTitle("⏱ Chronomètre actif")
                .setContentText("Temps écoulé : " + formaterTemps(tempsEcoule))
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)   // bloque la suppression manuelle par l'utilisateur
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    /**
     * Met à jour la notification existante avec le temps actuel.
     *
     * On réutilise le même NOTIF_ID — Android remplace la notification
     * existante au lieu d'en créer une nouvelle.
     */
    private void rafraichirNotification() {
        gestionnaireNotif.notify(NOTIF_ID, construireNotification());
    }

    // ================================================================
    // Méthodes publiques — accessibles par l'Activity via le Binder
    // ================================================================

    /**
     * Retourne le temps écoulé formaté (MM:SS).
     * Appelé par l'Activity pour mettre à jour l'affichage.
     */
    public String getTempsFormate() {
        return formaterTemps(tempsEcoule);
    }

    /**
     * Indique si le chronomètre tourne actuellement.
     * Utilisé par l'Activity pour adapter l'état des boutons.
     */
    public boolean estEnMarche() {
        return enMarche;
    }

    /**
     * Convertit un nombre de secondes en format lisible MM:SS.
     * Ex : 75 secondes → "01:15"
     */
    private String formaterTemps(int totalSecondes) {
        int minutes = totalSecondes / 60;
        int secondes = totalSecondes % 60;
        return String.format("%02d:%02d", minutes, secondes);
    }
}