# Changelog — KN2000 Tools

Historique des versions des outils Web MIDI pour Technics SX-KN2000.
Format inspiré de [Keep a Changelog](https://keepachangelog.com/) — versions [semver-like] simplifiées (vMAJOR.MINOR.PATCH à la discrétion du projet).

Outils :
- **Sound Editor** — `sound-editor.html` — éditeur de sons Sound Memory (144 octets)
- **Panel Console** — `panel-console.html` — console de pilotage du clavier

---

## [Non publié] / Unreleased

## 2026-10-24

### Sound Editor v0.8.2
- **Robustesse connexion** : le bouton 🔌 Connecter vérifie désormais que le KN2000 répond (lecture test du slot) avant d'afficher « ● connecté ». Sans réponse : « ⚠ pas de réponse » avec pistes de diagnostic dans le journal (au lieu d'un « connecté » trompeur).
- **Correctif APC critique** : le rattachement de la surface APC ne coupe plus le récepteur sysex du port KN2000 (l'onglet APC remettait à zéro le handler de tous les autres ports, y compris celui du clavier → pertes MIDI après bricolage de la config).
- **Port APC obsolète détecté** : si le port APC mémorisé (localStorage) correspond désormais au port du KN2000 (renumérotation des ports par Windows après rebranchement USB), la config APC est réinitialisée avec un message explicite dans le journal.
- **Docs** : compatibilité navigateurs corrigée — le sysex Web MIDI n'est pas supporté sous Firefox, Chrome/Edge desktop requis.

### Panel Console v1.0
- Première publication sur GitHub Pages.
- Sonorités par part, rythme/tempo, Techni-Chord, Panel Memory (rappel/lecture/écriture), lecture d'état, moniteur MIDI, diff de Panel Memories, bibliothèque d'ensembles (export/import JSON).

### Site
- Ajout de Panel Console à l'accueil (`index.html`).
- Aide en ligne par outil : `help/sound-editor.html`, `help/panel-console.html` (FR/EN).

### Sound Editor v0.8.1
- Première publication sur GitHub Pages.
- Lecture auto du slot à la connexion et au changement de slot, capture automatique de l'original (session), onglets GENERAL/TONE/EFFECT/BANQUE, catalogue de sons, surface Akai APC Key 25 mk2 (4 banques × 8 knobs, pads ON/OFF, LEDs), écriture armée 🔒/🔓, auto-appliquer ⚡.

## 2026-10-21

### Sound Editor v0.6 (développement local, serveur Python)
- Cartographie 144 octets, écrans de base, dump de banque (36 slots).

---

## Conventions pour les versions suivantes
1. Toute nouvelle version d'un outil incrémente : PATCH (correction), MINOR (fonction), MAJOR (rupture).
2. Ajouter une entrée ici **en même temps** que le push, et mettre à jour la section « Historique des versions » de la page d'aide de l'outil concerné.
3. Mise à jour de la carte de l'outil sur `index.html` (numéro de version).
