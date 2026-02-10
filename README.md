# 📱 IoT Light Monitor

**IoT Light Monitor** est une application Android native conçue pour surveiller en temps réel l'intensité lumineuse de capteurs distants via l'API REST de l'IoT Lab. Elle allie un dashboard moderne et un système d'alerte intelligent automatisé.

---

## 🚀 Fonctionnalités Principales

### 1. Dashboard Intelligent
* **Visualisation Temps Réel :** Liste complète des capteurs avec conversion des données JSON en affichage lisible (ID du mote, type et valeur en Lux).
* **Indicateurs Visuels :** Changement de couleur dynamique des cartes (Jaune pour "Allumé", Gris pour "Éteint") basé sur un seuil de **200 Lux**.
* **Filtrage Rapide :** Un switch permet d'isoler instantanément les capteurs détectant de la lumière.
* **Interface Moderne :** Utilisation d'un `Floating Action Button` (FAB) pour les réglages et d'un `SwipeRefreshLayout` pour actualiser les données manuellement.



### 2. Service de Surveillance (Arrière-plan)
L'application intègre un `Foreground Service` qui assure une veille constante toutes les 30 secondes, même si l'application est fermée ou si le téléphone est verrouillé.

* **Notifications :** Alerte visuelle et vibratoire en cas de détection lumineuse en semaine (19h-23h).
* **Alertes Emails :** Envoi automatique d'emails pour les événements critiques (Nuit de 23h à 06h ou durant le weekend).
* **Auto-Start :** Grâce au `BootReceiver`, la surveillance reprend automatiquement dès le démarrage du smartphone.

### 3. Panneau de Configuration
Une interface de réglages dédiée permet de personnaliser l'expérience :
* Configuration de l'adresse email cible.
* Ajustement des plages horaires de surveillance.
* **Navigation Fluide :** Transition animée "Slide-to-right" pour revenir au dashboard.
