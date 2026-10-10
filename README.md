# Cadence — carnet de partitions pour Android (téléphone et tablette)

Une application de lecture de partitions et de grilles d'accords pour musiciens, pensée pour la scène,
avec une interface moderne et claire (thèmes Clair, Sombre et « Scène » en noir pur).

## Fonctions

- **Bibliothèque** : recherche (titre, artiste, tags, paroles), tri A–Z / artiste / récents, favoris, tags.
- **Import** : fichiers ChordPro, texte (accords au-dessus des paroles convertis automatiquement) et PDF.
- **Lecture** : accords au-dessus des paroles, sections, refrains, 1 à 3 colonnes, taille du texte réglable.
- **Transposition**, **capo** (avec « formes » jouées), notation **Nashville**.
- **Défilement automatique** à vitesse réglable, **métronome** (tempo et mesure du morceau), tap tempo.
- **Diagrammes d'accords** guitare et ukulélé : touchez un accord dans la grille.
- **Sets** (listes de concert) réordonnables, lecture enchaînée avec boutons précédent / suivant.
- **Pédale Bluetooth / clavier** : pages suivante et précédente (PageUp/PageDown, flèches, espace).
- **PDF** : affichage page par page, inversion des couleurs en thème sombre.
- **Éditeur** : accords rapides selon la tonalité, sections, conversion « accords au-dessus », PDF joint.
- **Sauvegarde / restauration** JSON, partage ChordPro.
- **Tablette** : rail de navigation et liste + lecture côte à côte.
- Écran maintenu allumé pendant la lecture.
- **Volet masquable** (tablette) : bouton plein écran dans la barre de lecture, ou touche Retour pour le ré-afficher.
- **Transfert Wi-Fi** : Réglages → Transfert Wi-Fi. Ouvrez l'adresse affichée sur un PC du même réseau, saisissez le code
  à 4 chiffres, puis ajoutez, modifiez ou supprimez des morceaux et joignez des PDF (glisser-déposer possible).
- **Import depuis un site** : menu « + » → « Importer depuis un site » (Ultimate Guitar, La Boîte à chansons ou toute page
  d'accords). Importez la page, ou sélectionnez le texte de la chanson puis « Importer la sélection ».
- **Écouter** : bouton casque dans la barre de lecture → Spotify (lecture directe si l'appli est installée) ou YouTube.
- **Diagrammes** guitare, ukulélé **et piano** en touchant un accord.
- **PDF** : menu « + » → « Ajouter un PDF », ou depuis le PC.

## Obtenir l'APK sans Android Studio (GitHub)

1. Créez un dépôt GitHub et envoyez-y tout le contenu de ce dossier.
2. Onglet **Actions** → « Compiler l'APK » → **Run workflow** (ou simplement faites un push).
3. Quand c'est vert, ouvrez l'exécution et téléchargez l'artefact **cadence-apk** : c'est `app-debug.apk`.
4. Copiez-le sur le téléphone ou la tablette et installez-le (autoriser les sources inconnues).

## Avec Android Studio

Ouvrez ce dossier, laissez Gradle synchroniser, puis **Run** ou *Build → Build APK(s)*.

## Technique

Kotlin, Jetpack Compose (Material 3), Room, DataStore. Android 8.0+ (API 26). Aucune connexion Internet requise.
