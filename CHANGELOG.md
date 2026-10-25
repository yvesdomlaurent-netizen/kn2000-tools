# Changelog — KN2000 Tools

Historique des versions des outils Web MIDI pour Technics SX-KN2000.
Format inspiré de [Keep a Changelog](https://keepachangelog.com/) — versions [semver-like] simplifiées (vMAJOR.MINOR.PATCH à la discrétion du projet).

Outils :
- **Sound Editor** — `sound-editor.html` — éditeur de sons Sound Memory (144 octets)
- **Panel Console** — `panel-console.html` — console de pilotage du clavier

---

## [Non publié] / Unreleased

## 2026-10-25

### Sound Editor v0.9.4 — mapping Modulation Wave corrigé
- **Modulation Wave : bits 0-1 de 0x19** (avant : bits 1-2). Validé terrain : l'ancien encodage donnait SQR/SIN décalés (TRI→SQR, SQR→SIN, SAW→SQR) ; avec bits 0-1, l'octet lu par la machine correspond exactement à la valeur affichée. Lecture et écriture alignées (`raw[0x19]&3`).

### Sound Editor v0.9.3 — connexion fiable au rechargement
- **Ports KN2000 mémorisés** : les ports In/Out retenus sont restaurés au rechargement de la page (avant : retour systématique au premier port de la liste).
- **Aucune action MIDI avant vérification de la connexion** : le test de liaison est un ping léger (lecture d'UN octet du slot courant). La lecture automatique du slot ne se déclenche qu'après une réponse confirmée du KN2000 — plus de « ⚠ pas de réponse » pendant qu'une lecture tourne en parallèle.
- **Connexion automatique au chargement uniquement si des ports mémorisés existent** — sinon l'utilisateur choisit ses ports et clique « 🔌 Connecter » (les ports validés sont ensuite mémorisés).
- Nouvel indicateur interne `connOk` : seules les connexions vérifiées autorisent les lectures automatiques (changement de slot).

### Sound Editor v0.9.2 — commandes branchées, luminosité LEDs, avertissements volatilité
- **Les 12 contrôles muets branchés** : tous les segments (Octave shift / Right of split / Left of split, Wheel mode, Sustain pedal, A&T On/Mode/Type/Pattern, Modulation Mode et Wave, Filter type par tone) écrivent maintenant dans le KN2000 par read-modify-write (écriture armée requise, même logique que Distortion type / Effect Pan). Auparavant, ces contrôles ne modifiaient que l'écran — rien n'était envoyé (constaté terrain par Yves sur Octave shift).
- **Correctif lecture** : Modulation Mode était lu dans l'octet 0x16 au lieu de 0x19 — corrigé.
- **Luminosité des LEDs de pads** : les pads RGB sont maintenant pilotés sur le canal 0x96 (allumé fixe 100 %) au lieu de 0x90 (10 %) — les LEDs sont visibles en plein jour. Les boutons périphériques (LED mono) restent en on/off, non réglables. La logique des couleurs de la v0.9.1 est conservée.
- **Avertissements mémoire volatile** : le KN2000 ne conserve les sons que ~80 min hors tension (message de la machine à la sauvegarde) — bandeau, confirmation et journal après l'envoi d'un package, rappel d'archivage après « ✅ Garder le nouveau son ». Garde tes packages/catalogue pour recharger après une coupure.

### Sound Editor v0.9.1 — correctifs LEDs retour terrain
- **Pads ON/OFF : changement d'état de nouveau visible.** La couleur par défaut des tones ON/OFF était le vert (0x21), identique à la couleur d'état ON → aucun changement visible à l'appui. Les tones ON/OFF passent par défaut en **blanc** (état OFF) ; l'état ON reste **vert**. (Couleurs toujours paramétrables fonction par fonction.)
- **Pads sélecteurs de banque : maintenant visibles.** Un pad utilisé comme bouton de banque s'affiche en **gris** (banque inactive) ou **vert** (banque active) — auparavant éteint sauf banque active, et l'éclairage « couleur fonction » pouvait écraser le vert (ordre d'application corrigé : les LEDs de banque sont appliquées en dernier).
- **Protocole LED correct pour les boutons** (Track/Scene/Stop/Play…) utilisés comme sélecteurs de banque : LED simple on/off (00/01), pas la palette RVB des pads.

### Sound Editor v0.9.0 — surface APC étendue
- **8 banques de knobs** (64 paramètres pilotables, contre 4 banques/32 avant) ; migration automatique des configs existantes.
- **N'importe quel bouton de l'APC comme sélecteur de banque** : Knob Control, Track, Scene, Stop, Play… ou un pad — le mécanisme d'apprentissage « 🎓 bouton » accepte tout bouton émettant un Note On.
- **LEDs des pads en continu** : chaque pad assigné reste allumé à la couleur de sa fonction (état OFF) ; vert = fonction ON ; éteint = pad libre. Plus besoin d'appuyer pour voir l'assignation.
- **Couleurs paramétrables par fonction** : palette de 12 couleurs (palette officielle Akai), éditeur dédié dans l'onglet APC.
- **Paramètres manquants ajoutés aux knobs** : enveloppes complètes des 3 tones (Decay 2, Sustain 2, Touch attack, Touch curve, Lvl KF, EKF atk/dcy/rls), filtre complet des 2nd/sub (Touch depth, curve, slope), Auto bend depth/speed, Distortion touch — 74 paramètres au total.
- **Export / import de la config APC** (JSON) : banques, assignations, pads, couleurs et ports — pour copier la config d'une origine à l'autre (localhost ↔ github.io), d'un navigateur ou d'un PC à l'autre.
- À faire (demande d'Yves) : faire passer l'export config APC par le dossier 📁 déjà configuré (comme banques/packages), avec repli sur téléchargement.

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
