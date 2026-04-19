# OBF-Utilities — Mod Fabric 1.20.1

Mod client Fabric développé par **HamtaBot** pour le serveur OBF.  
Overlay HUD configurable avec statistiques de session, autotool, fullbright et outils de débug.

---

## Fonctionnalités

### HUD de session
- **Blocs posés** — session + total + blocs/sec en temps réel
- **Blocs minés** — session + total + blocs/sec en temps réel
- **Mobs tués** — compatible stacker (lit les nameplates de stack)
- **Blocs custom** — tracker plusieurs blocs spécifiques simultanément (posé ou miné), chacun avec session + total + /sec
- **Timer de pub** — détecte tes messages avec `[i]` ou `[item]` et lance un countdown de 15 minutes avec notification en titre quand disponible
- **Durée de session** — affichée à côté du titre, remise à zéro au reset
- **Taux /sec** — affiché en temps réel, disparaît automatiquement à l'inactivité
- **Séparateurs de milliers** avec points (`1.234.567`)
- **Overlay déplaçable** — position mémorisée

### AutoTool
- Sélection automatique du meilleur outil dans la hotbar en fonction du bloc ciblé
- Supporte : pioche, pelle, hache, houe, cisaille, épée
- Chaque outil activable/désactivable individuellement
- Option pour ignorer les outils à moins de 10 de durabilité
- Très réactif — conçu pour le oneblock

### FullBright
- Contrôle de la luminosité de 0% à 1500%
- Bouton ON/OFF
- Réglage par paliers de 10% et 100%
- Barre de progression visuelle

### Débug
- **FPS** — couleur dynamique (vert ≥60, jaune ≥30, rouge <30)
- **Coordonnées** — X/Y/Z + direction cardinale
- **RAM** — utilisée / allouée en Mo avec pourcentage
- Chaque overlay déplaçable indépendamment (en jeu ou depuis la config)

### Stats globales serveur
- À la connexion, envoie automatiquement une commande et lit la réponse
- Les totaux blocs posés, minés et mobs tués sont récupérés du serveur
- Les messages de réponse sont cachés du chat
- Bouton **Actualiser** dans la config (cooldown 15 minutes)
- Les totaux sont calculés à la connexion uniquement

---

##  Touches (configurables dans Options → Contrôles → OBF Utilities)

| Touche par défaut | Action |
|---|---|
| `K` | Afficher / Cacher le HUD |
| `O` | Ouvrir la configuration |

---

## Interface de configuration

Appuie sur `O` en jeu pour ouvrir plusieurs panneaux déplaçables indépendamment :

| Panneau | Contenu |
|---|---|
| **◆ Paramètres** | Toggles sections HUD, reset session, actualiser stats |
| **◆ Blocs custom** | Ajouter/supprimer des blocs custom avec sélecteur de bloc et icônes |
| **◆ AutoTool** | ON/OFF, skip durabilité, sélection des outils autorisés |
| **◆ FullBright** | ON/OFF, réglage de la luminosité |
| **◆ Débug** | ON/OFF pour FPS, coordonnées, RAM |

Les positions des panneaux sont **mémorisées** entre les sessions.

---

## Prérequis

- **Java 17** ou supérieur
- **Minecraft 1.20.1** avec **Fabric Loader ≥ 0.14.22**
- **Fabric API** pour 1.20.1

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

Le `.jar` compilé se trouve dans `build/libs/obf-utilities-2.2.2.jar`.

---

## Installation

1. Installe [Fabric Loader](https://fabricmc.net/use/installer/) pour Minecraft 1.20.1
2. Télécharge [Fabric API](https://modrinth.com/mod/fabric-api/versions?g=1.20.1) pour 1.20.1
3. Copie `obf-utilities-2.0.0.jar` **et** `fabric-api-xxx.jar` dans `.minecraft/mods/`
4. Lance Minecraft avec le profil **Fabric 1.20.1**

---

## Fichiers créés

| Fichier | Contenu |
|---|---|
| `config/obfutilities.json` | Configuration complète (sections, blocs custom, positions des panneaux, etc.) |
| `obfutilities_hud_pos.properties` | Position de l'overlay HUD |

---

## Structure du projet

```
src/main/java/com/hamtabot/obfutilities/
├── OBFUtilities.java                  ← Point d'entrée, keybindings, compteurs, taux
├── StatsReader.java                   ← Lecture des stats Minecraft vanilla (blocs custom)
├── autotool/
│   └── AutoTool.java                  ← Sélection automatique d'outil
├── config/
│   └── ModConfig.java                 ← Configuration JSON (positions, toggles, blocs custom)
├── debug/
│   └── DebugOverlay.java              ← Overlays FPS / Coords / RAM
├── fullbright/
│   └── FullBright.java                ← Logique dans LightTextureMixin
├── hud/
│   ├── OBFHud.java                    ← Rendu du HUD principal
│   └── OBFConfigScreen.java           ← Interface de configuration multi-panneaux
└── mixin/
    ├── BlockPlaceMixin.java           ← Détection blocs posés
    ├── BlockMineMixin.java            ← Détection blocs minés
    ├── KillMixin.java                 ← Détection mobs tués (vanilla)
    ├── StackKillMixin.java            ← Détection mobs tués (stacker)
    ├── ChatMixin.java                 ← Pub + interception réponse stats
    ├── JoinMixin.java                 ← Envoi auto stats à la connexion
    ├── AutoToolMixin.java             ← Hook sur le minage pour l'autotool
    └── LightTextureMixin.java         ← FullBright via LightmapTextureManager
```
