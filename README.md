📱 IoT Light Monitor
IoT Light Monitor est une application Android native permettant de surveiller en temps réel l'état de luminosité de capteurs distants via une API REST. Elle intègre un système d'alerte intelligent basé sur des plages horaires configurables.

🚀 Fonctionnalités Principales
1. Dashboard en temps réel

Affichage dynamique : Liste des capteurs actifs avec leurs valeurs relevées en Lux.

Mise en évidence visuelle : Les capteurs détectant une lumière active (seuil > 200 Lux) sont mis en évidence en jaune.

Filtrage intelligent : Un switch permet d'afficher uniquement les capteurs allumés.

Détails avancés : Un clic sur un capteur ouvre un "Bottom Sheet" élégant affichant l'historique et les détails techniques.

2. Système de Surveillance & Alertes

L'application utilise un service d'arrière-plan (Foreground Service) pour interroger l'API toutes les 30 secondes, même lorsque l'application est fermée.

Notifications (Semaine 19h-23h) : Émission d'une notification système si une nouvelle lumière est allumée.

Alertes Email (Weekend ou Nuit) : Envoi automatique d'un email si un événement survient le weekend (19h-23h) ou en semaine durant la nuit (23h-06h).

Persistance : Grâce au BootReceiver, la surveillance redémarre automatiquement après un reboot du téléphone.

3. Configuration Personnalisée

Un menu dédié (accessible via un bouton flottant ergonomique) permet de configurer l'application selon vos besoins :

Saisie de l'adresse email de destination des alertes.

Modification des plages horaires de surveillance pour la semaine, le weekend et la nuit.
