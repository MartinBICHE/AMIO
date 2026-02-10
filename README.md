# 📱 IoT Light Monitor

**IoT Light Monitor** est une application Android native conçue pour surveiller en temps réel l'intensité lumineuse de capteurs distants via l'API REST de l'IoT Lab. Elle allie un dashboard moderne et un système d'alerte intelligent automatisé.

---

## 🚀 Fonctionnalités Principales

### 1. Dashboard Intelligent & Gestuel
* **Visualisation Temps Réel :** Liste complète des capteurs avec conversion des données JSON en affichage lisible (ID du mote, type et valeur en Lux).
* **Indicateurs Visuels :** Changement de couleur dynamique des cartes (Jaune pour "Allumé", Gris pour "Éteint") basé sur un seuil de **200 Lux**.
* **Mise à jour par Geste (Swipe-to-Refresh) :** L'utilisateur peut rafraîchir manuellement les données à tout moment en effectuant un glissement vers le bas sur la liste.
* **Filtrage Rapide :** Un switch permet d'isoler instantanément les capteurs détectant de la lumière.



### 2. Service de Surveillance (Arrière-plan)
L'application intègre un `Foreground Service` qui assure une veille constante toutes les 30 secondes, même si l'application est fermée ou si le téléphone est verrouillé.

* **Notifications :** Alerte visuelle et vibratoire en cas de détection lumineuse en semaine (19h-23h).
* **Alertes Emails :** Envoi automatique d'emails pour les événements critiques (Nuit de 23h à 06h ou durant le weekend).
* **Auto-Start :** Grâce au `BootReceiver`, la surveillance reprend automatiquement dès le démarrage du smartphone.



### 3. Panneau de Configuration & Ergonomie
Une interface de réglages dédiée, accessible via un **bouton flottant (FAB)** en bas de l'écran, permet de personnaliser l'expérience :

* **Configuration :** Saisie de l'adresse email cible et ajustement des plages horaires de surveillance.
* **Navigation Fluide :** Transition animée "Slide-to-right" pour revenir au dashboard (le panneau de réglages glisse vers la droite pour révéler l'accueil).
* **Détails des Capteurs :** Un clic sur un élément de la liste déploie un volet d'information (Bottom Sheet) sans changer de page.



---

## 🛠 Architecture Technique
L'application utilise un `CoordinatorLayout` pour permettre au bouton de réglages de remonter automatiquement lors de l'ouverture des détails d'un capteur, garantissant ainsi qu'aucun élément interactif ne soit masqué.

---
*Projet réalisé en 2026 - Surveillance IoT Intelligente*
