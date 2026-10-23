# 📦 OBF-Utilities — Mod client Fabric 1.20.1

> Développé par **OneBlockFrance**
> 🔗 GitHub : https://github.com/HamtaBot/OBF-Utilities

Tout se règle depuis le menu de config (`O` en jeu). Le bouton « Organiser », en bas du menu, permet de ranger les catégories dans l'ordre de ton choix.

---

## 🖥️ HUD de session
Tes stats en temps réel, directement à l'écran :

- **Blocs posés** : session, total serveur et blocs/sec
- **Blocs minés** : session, total serveur et blocs/sec
- **Mobs tués** : session, total serveur et mobs/sec, stacks de mobs compris
- **Blocs suivis** : compte n'importe quel bloc précis, posé ou miné, avec son icône
- **Durée de session** affichée en haut du HUD
- Option pour **garder la session** après un redémarrage du jeu

Les totaux serveur sont récupérés automatiquement à la connexion, sans polluer le chat. Un bouton permet de les actualiser (une fois toutes les 15 min).

> Le HUD se déplace à la souris et se redimensionne par le coin en bas à droite. Il s'aimante aux bords et au centre de l'écran.

## ⏰ Publicité
- Enregistre ta pub dans le mod et envoie-la en un clic
- Détecte `[i]` ou `[item]` dans tes messages et lance un compte à rebours de 15 min
- Message à l'écran quand tu peux renvoyer ta pub

## 💤 AFK Timer
- Compte le temps passé dans ta zone AFK : il démarre quand tu y entres et s'arrête quand tu en sors
- **Tu définis toi-même la zone** : place-toi sur deux coins opposés et clique sur « Ma position »
- Bouton de réinitialisation
- Option pour garder le temps après un redémarrage du jeu

## ⏱️ Timers
- Jusqu'à 10 timers : comptes à rebours ou chronomètres
- Title et son à la fin d'un compte à rebours
- Basés sur l'heure réelle : un compte à rebours continue même jeu fermé (pratique pour les votes)
- Affichables à l'écran, déplaçables et redimensionnables

## ⛏️ AutoTool
Prend automatiquement le meilleur outil de ta hotbar selon le bloc visé, et ton arme quand tu frappes un mob.
- Pioche, pelle, hache, houe, cisailles et épée, chacune activable séparément
- Option pour ignorer les outils à 10 de durabilité ou moins
- Très réactif, pensé pour le OneBlock

## 🚀 Optimisations
- **Cacher l'équipement** des autres joueurs : armure, objets en main et en seconde main, et ce qu'ils portent sur la tête (le tien reste visible)
- **Cacher les cadres** sans cacher l'objet ou la carte qu'ils contiennent
- **Désactiver toutes les particules**
- **Couper l'animation d'ouverture** des coffres, coffres de l'Ender et shulker boxes
- **Cacher le mob qui tourne** dans les spawners

## 🌀 Macro Warps
- À chaque connexion, le mod récupère la liste des `/warp` du serveur
- Assigne une touche du clavier à n'importe quel warp : un appui et tu es téléporté
- Aucune touche assignée par défaut
- Les warps supprimés du serveur disparaissent de la liste avec leur touche, les autres gardent la leur après un redémarrage

## 📥 Unité stockage
- Bouton **Tout déposer** : dépose dans l'unité tous les objets de ton inventaire identiques au bloc qu'elle contient
- Bouton **Tout prendre** : prend des stacks jusqu'à remplir ton inventaire
- Un clic par tick : l'inventaire se remplit ou se vide en quelques secondes
- Chaque bouton s'active ou se désactive dans la config

## 🌑 Blocs sombres
- Applique une texture sombre aux blocs de ton choix, plus reposante pour les yeux
- Fonctionne partout, icônes de l'inventaire comprises, et avec Sodium

## ☀️ Visuel
- **FullBright** : vision nocturne côté client
- **Overlays**, déplaçables chacun à la souris :
  - FPS en couleur (vert ≥ 60, jaune ≥ 30, rouge < 30)
  - Coordonnées X/Y/Z et direction (N/S/E/O)
  - RAM utilisée / allouée avec une jauge

## 📍 Waypoints
- Marqueurs dans le monde avec nom et couleur, visibles à travers les blocs jusqu'à 1000 blocs
- Un affichage par dimension (Overworld, Nether, End)
- **Partage en un clic** : le bouton « Partager » copie le waypoint, colle-le dans le chat et les autres joueurs qui ont le mod l'importent en cliquant dessus

## 🎮 Contrôles
- **Double appui pour jeter** : évite de drop un objet par erreur
- **Remplacer le clic gauche ou droit** par une touche du clavier, directement depuis le mod (plus besoin de changer la touche dans les paramètres)

---

## ⌨️ Touches par défaut

| Touche | Action |
|---|---|
| `K` | Afficher / cacher le HUD |
| `O` | Ouvrir la configuration |
| `N` | Créer un waypoint |
| `G` | Ouvrir les waypoints |

*Modifiables dans Options → Contrôles → Touches, catégorie OBF Utilities.*

---

## Installation

1. Installe [Fabric Loader](https://fabricmc.net/use/installer/) pour Minecraft 1.20.1
2. Télécharge [Fabric API](https://modrinth.com/mod/fabric-api/versions?g=1.20.1) pour 1.20.1
3. Copie le `.jar` d'OBF-Utilities **et** celui de Fabric API dans `.minecraft/mods/`
4. Lance Minecraft avec le profil **Fabric 1.20.1**

### Prérequis
- **Minecraft 1.20.1**
- **Fabric Loader 0.15.0** ou plus récent
- **Fabric API** pour 1.20.1
- **Java 17** ou plus récent

### Compatibilité
- Fonctionne avec les launchers qui acceptent les mods Fabric (launcher officiel, Prism, MultiMC, Modrinth, Feather...)
- **Lunar Client n'est pas compatible** : il n'accepte pas les mods Fabric ajoutés à la main

---

## Compiler

**Windows :**
```cmd
gradlew.bat build
```

**Linux / macOS :**
```bash
chmod +x gradlew
./gradlew build
```

Le `.jar` se trouve ensuite dans `build/libs/`.

---

## Fichiers créés

| Fichier | Contenu |
|---|---|
| `config/obfutilities.json` | Configuration du mod (options, blocs suivis, timers, zone AFK, touches des warps...) |
| `config/obfutilities_waypoints.json` | Waypoints |
| `config/obfutilities_session.json` | Session sauvegardée (si « Garder la session » est activé) |
| `config/obfutilities_afk.json` | Temps AFK sauvegardé (si l'option est activée) |
| `obfutilities_hud_pos.properties` | Position du HUD |
