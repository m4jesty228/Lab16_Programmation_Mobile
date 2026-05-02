# 📱 ServiceChronometreJava — Foreground & Bound Service Android (Java)

> **Auteur :** DOSSAH Landry
> **Cours :** Programmation Mobile — ENSA Marrakech
> **Date :** Mai 2026
> **Stack :** Java · Android SDK · Foreground Service · Bound Service · Notifications

---

## 🚀 Objectif du projet

Ce projet consiste à développer une application Android de chronomètre basée sur les **Services Android** afin de comprendre leur fonctionnement avancé.

L’application met en œuvre :

* 🔴 Foreground Service (obligatoire Android 8+)
* 🔗 Bound Service (communication Activity ↔ Service)
* 🔔 Notification persistante mise à jour en temps réel
* ⏱️ Chronomètre fonctionnant en arrière-plan
* 📲 Contrôle démarrer / arrêter depuis l’interface

---

## 🧠 Concepts maîtrisés

* Cycle de vie des Services Android (`onCreate`, `onStartCommand`, `onBind`, `onDestroy`)
* Foreground Service et Notification Channel
* START_STICKY et gestion du redémarrage système
* Bound Service avec Binder
* Threads avec ScheduledExecutorService
* Communication Activity ↔ Service

---

## 🏗️ Architecture
```
MainActivity
   │
   ├── startForegroundService()
   ├── bindService()
   │
   ▼
ChronometreService
   ├── Foreground Service (notification live)
   ├── Bound Service (Binder)
   └── Timer (ScheduledExecutor)
```
---

## ⚙️ Fonctionnalités

| Fonctionnalité   | Description                         |
| ---------------- | ----------------------------------- |
| ▶️ Démarrer      | Lance le service + chronomètre      |
| ⏱️ Chrono live   | Mise à jour chaque seconde          |
| 🔔 Notification  | Affichage temps réel                |
| 🔗 Bound Service | Communication directe avec Activity |
| ⛔ Arrêt          | Stop propre du service              |

---

## 📱 Interface

* ⏱️ Affichage du temps (TextView)
* ▶️ Bouton démarrer
* ⛔ Bouton arrêter

---

## 🔴 Foreground Service

Le service s’exécute en premier plan avec une notification obligatoire depuis Android 8.0.

👉 Il empêche l’arrêt automatique par le système.

---

## 🔗 Bound Service

Permet à l’Activity de récupérer l’instance du service via un **Binder**, pour une communication directe.

---

## ⏱️ Gestion du temps

Le chronomètre utilise :

* ScheduledExecutorService
* incrémentation toutes les 1 seconde
* mise à jour de la notification en live

---

## 📄 Permissions

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
```

---

## 📌 Déclaration du Service

```xml
<service
    android:name=".ChronometreService"
    android:foregroundServiceType="dataSync"
    android:exported="false" />
```

---

## ▶️ Exécution

1. Lancer l’application
2. Cliquer sur **DÉMARRER SERVICE**
3. Observer la notification + chronomètre actif
4. Fermer l’application → le service continue
5. Cliquer sur **ARRÊTER SERVICE** pour stopper

---

## 🎥 Démonstration

👉 Vidéo du projet :

> 

https://github.com/user-attachments/assets/b5b6de56-4c89-48b6-b7c8-4466da3dc564

---

## 🧠 Conclusion

Ce projet m'a permis de comprendre :

* les Services Android en profondeur
* le fonctionnement du Foreground Service
* la communication Activity ↔ Service
* la gestion des tâches longues en arrière-plan

---

## 👨‍💻 Auteur

**DOSSAH Landry**
ENSA Marrakech — Programmation Mobile
