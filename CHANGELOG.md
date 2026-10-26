# Changelog — KN2000 Tools

Historique des versions des outils Web MIDI pour Technics SX-KN2000.
Format inspiré de [Keep a Changelog](https://keepachangelog.com/) — versions [semver-like] simplifiées (vMAJOR.MINOR.PATCH à la discrétion du projet).

Outils :
- **Sound Editor** — `sound-editor.html` — éditeur de sons Sound Memory (144 octets)
- **Panel Console** — `panel-console.html` — console de pilotage du clavier
- **Sauvegarde / Catalogue** — `backup.html` — dump et catalogue de l'état complet du clavier

---

## [Non publié] / Unreleased

## 2026-10-26

### Sauvegarde / Catalogue v0.4.2 — restauration : SOUND MEMORY par défaut dans le sélecteur (retour terrain v0.4.1)
- **Retour terrain v0.4.1** : Yves ne trouvait pas la restauration — le sélecteur d'aire s'ouvrait sur **PANEL** (barré « à valider ») → le bouton ↩️ Restaurer apparaissait désactivé et la fonction semblait absente.
- **Correctif** : le sélecteur d'aire de la carte Restauration s'ouvre par défaut sur **SOUND MEMORY** (la seule aire restaurable à ce jour), libellée « ✅ restaurable » ; les autres affichent « — à valider » avec tooltip explicite. Le bouton reste actif dès qu'un patch Sound existe et que la connexion est établie.

### Sauvegarde / Catalogue v0.4.1 — écart 32 du bloc 40 PANEL normalisé (retour terrain v0.4.0)
- **Retour terrain v0.4.0** : premiers tests OK (aires enchaînées, export nommé `KN2000_YL-….json`, patches Sound/Composer/Sequencer, import dédupliqué) — mais le patch PANEL était **refusé à tort** : « 32 octets manquants ». C'est l'**écart 32 documenté du bloc 40** (s'arrête systématiquement à 1056/1088, jamais émis par le clavier — constat terrain 18-26/10), pas une perte.
- **Normalisation** : le garde-fou « octets manquants » accepte désormais l'écart 32 du bloc 40 du PANEL (patch catalogué avec les octets connus, badge « écart 32 documenté » sur la ligne du patch, log explicite). Toute autre perte réelle reste **refusée** (bloc, aire ou Sound incomplet).
- **Diff aligné** : dans le croisement du tableau de bord, un octet manquant **des deux côtés** (dump et patch, ex. l'écart 32) compte comme identique — un dump PANEL re-importé comparé à lui-même affiche 🟢 conforme.
- Validation hors clavier : 6 tests ciblés (écart accepté, dédup panel, vraie perte refusée, Sound incomplet toujours refusé, diff null=null, store final) + les 16 tests v0.4.0 inchangés.

### Sauvegarde / Catalogue v0.4.0 — finalisation usage : onglets Dumps/Catalogue, patches dédupliqués, restauration complète ou différentielle
- **Deux onglets** : 📡 **Dumps** (dump par aire + « Sauvegarder tout », export JSON unique désormais nommé) et 📚 **Catalogue** — la page devient l'outil de sauvegarde/restauration courant, socle d'une future appli Android. Le panneau technique (enables 📡, adresses de base, N o/trame, pace, envoi manuel bloc par bloc, 🔍 Comparer) déménage dans la **page privée `tools.html`** (moteur v0.3.1 inchangé), liée depuis la page principale — lien retirable quand la page sera complète.
- **Patches versionnés dédupliqués par contenu** : « 💾 Cataloguer » sauvegarde le dernier dump d'une aire comme patch (nom auto + date). Contenu identique au patch précédent → **seule la date est mise à jour** ; contenu différent → nouvelle version. Les anciennes versions ne sont **jamais écrasées** (suppression 🗑 explicite uniquement). L'import d'un JSON kn2000-catalog-v1 crée les patches par aire avec la même déduplication ; les dumps incomplets (octets `--`) sont refusés.
- **Tableau de bord « croisement »** : par aire — ce qui réside dans le KN2000 (dernier dump, **persisté en localStorage**, date + volume) ↔ patch courant du catalogue (🟢 conforme / 🟡 N octets différents / ❌ jamais dumpé), **date du dernier envoi vers le KN2000**, et statut de restauration par aire (affichage permanent de ce qui est validé tant que la page est « en construction »).
- **Restauration Sound Memory** (✅ validée terrain) : **Complète** = entête 80 o + 36 sons en **73 trames ITW** (120 o max/trame, pause 60 ms ≈ 5 s) ; **Différentielle** = ne renvoyer que l'entête/les sons qui diffèrent du dernier dump (0 trame si conforme). Confirmation obligatoire avec rappel mémoires volatiles (~80 min) ; Panel/Composer/Sequencer **barrées « à valider »** jusqu'à preuve d'écriture. Infos d'entête par patch (blocs, tailles) + **36 noms de sons décodés** pour la Sound Memory (16 o, aligné sur la leçon v0.6.4 du Sound Editor).
- **Validation hors clavier** : 16 tests logiques verts (trames complètes 73 et adresses exactes `30 00 50 + n×90`/`+120`, différentiel 0 ou 2 trames, dédup, refus d'incomplet, diffBytes) + rejeu de l'import réel du catalogue terrain du 26/10 (re-dump post-restauration : patch créé, entête + sons extraits, « A-1 Distortion Piano » décodé, 73 trames complètes, différentiel correct).

### Sauvegarde / Catalogue v0.3.1 — limite des trames d'écriture établie : 120 octets max (bissection terrain)
- **Bissection terrain complète** (méthode de la lettre-témoin : son 2 avec 1ᵉʳ octet du nom modifié) : trames ITW 2C acceptées à 1, 16, 64, 96, 120 octets originaux — refusées à 121, 128, 144. **Limite exacte : 120 octets originaux = 255 octets sur le fil** — cohérent avec un buffer de 256 octets côté clavier. Le clavier ne répond jamais aux écritures (ni ACK ni ERR) : il applique en silence, ou jette en silence.
- **Mode sons v0.3.1** : chaque son de 144 o part en **2 trames (120 + 24)** aux adresses `30 00 50 + n×90` → restauration complète de la Sound Memory (5184 o) en **72 trames ≈ 4 s** à 60 ms. Plafond N = 120 octets en mode « trames de N octets » (défaut 120).
- **Milestones terrain de la journée** : première écriture sysex validée (Transpose `20 00 02`), écriture Sound Memory validée (nom du son A1 modifié et restauré par trames de 16 o), boucle sauvegarde→JSON→réécriture→vérification fermée. Hypothèses invalidées au passage : le refus initial des trames 144 o était une question de TAILLE (pas d'adresse ni de rythme) ; la piste « octet par octet obligatoire » était fausse (16 et 120 o passent).

### Sauvegarde / Catalogue v0.3 — import JSON + envoi vers le KN2000 (ITW 2C, expérimental) ; dumps sans page BULK DUMP
- **Import catalogue** : la nouvelle section « 3 — Import catalogue & envoi » charge un JSON kn2000-catalog-v1 (📂 Importer), liste les aires/blocs (avec comptage des octets manquants `--`) et refuse l'envoi d'un bloc incomplet.
- **Envoi vers le KN2000 (première écriture bulk, expérimental)** : trames **ITW (2C)** — miroir des ITR, format du Sound Editor (`f0 50 2c 01 18 10 ADR 00 Sh Sl <nibbles> 00 CK f7`) — en deux modes : **36 sons × 144 octets** (36 trames aux adresses `30 00 50 + n×90`) ou **trames de N octets** à adresse de base incrémentée (défaut `30 00 50`, N=144, pause 300 ms). L'inconnue du test terrain : la réaction du clavier aux trames multi-octets (le Sound Editor écrivait octet par octet).
- **Vérification 🔍 Comparer** : diff octet par octet entre le bloc importé et le bloc de même position du re-dump de l'aire en session — rapport identiques/différents/manquants + premières différences. Procédure : Envoyer → observer le clavier → ~5 s → re-dumper → Comparer.
- **Découverte terrain : pas besoin de la page SYSEX BULK DUMP du clavier** — les dumps se lancent directement depuis la page (validé 26/10). Tous les textes, titres et messages y faisant référence ont été retirés (tip d'en-tête, boutons d'aire, enchaînement, timeouts). Les 4 aires + leurs comportements sont documentés comme validés terrain dans l'aide.
- **Validation hors clavier** : rejeu de la capture panel 18/10 sans régression ; import d'un catalogue factice + envoi mode sons → 36 trames ITW exactes (adresses `30 02 70` pour le slot 3 = `0x50+2×0x90`… `30 28 00` pour le 36e), checksums OK, SIZ 144 nibblisé correct.

### Sauvegarde / Catalogue v0.2.2 — SOUND MEMORY validée terrain ✅ ; blocs multiples à même adresse conservés
- **Retour terrain (essai SOUND MEMORY)** : dump complet — handshake OK, **2 BTR pour le bloc 30** (SIZ 80 = entête Sound Memory, puis SIZ 5184 = les 36 sons × 144 octets), CDD jusqu'à **5184/5184 exact**, EOK, END. La trame DRQ « 30 00 00 » avec la constante 42 fonctionne telle quelle — deuxième aire validée.
- **Correctif catalogue (v0.2.1 → v0.2.2)** : la v0.2.1 indexait les blocs par adresse seule — le second BTR du bloc 30 écrasait l'entête de 80 octets (perdue du catalogue). Désormais chaque BTR crée une **occurrence distincte** (clé adresse + n° d'ordre, champ « seq » dans le JSON) : l'entête ET les sons sont conservés, dans l'ordre d'arrivée. Le journal affiche « 30 » puis « 30#2 »…
- **Validation hors clavier** : dump SOUND MEMORY synthétique (2 BTR même adresse) → blocs 30#1 et 30#2 conservés avec données distinctes, exactement 1 ACK par paquet (6 pour 6), END → finished ; rejeu de la capture panel 18/10 inchangé (aucune régression).

### Sauvegarde / Catalogue v0.2.1 — retour terrain v0.2 : dump PANEL complet ✅, bouton Vider déplacé à la charte
- **Retour terrain v0.2 (essai 1, PANEL)** : connexion charte OK (ping → réponse → ● connecté → enables 3× ON automatiques), dump PANEL COMPLET — BTR 40 + 8 CDD (1056/1088), BTR 41 + 64 CDD (8064/8064 exact), EOK, END, 9120 octets — identique à la Panel Console v1.0.9. L'avertissement « Bloc 40 clôt INCOMPLET : 1056/1088 (écart 32) » est le mystère documenté (écart 32 systématique, sans conséquence), pas une erreur.
- **Bouton « 🧹 Vider » déplacé dans l'en-tête du Journal MIDI** (charte console : logHead), hors de la rangée Connexion où il passait inaperçu.

### Sauvegarde / Catalogue v0.2 — connexion à la charte des pages (bouton Connecter + ping KN2000)
- **Retour terrain v0.1** : la page affichait « non connecté » sans bouton de connexion — seule la sélection In/Out existait, sans test de réponse du clavier.
- **Connexion identique à la Panel Console (charte des pages)** : bouton **🔌 Connecter** avec test de liaison (ping DRQ 20 00 02 Transpose, SIZ 1) AVANT toute action — pastille **● connecté** seulement si le KN2000 répond, sinon **⚠ pas de réponse** avec vérifications conseillées ; lecture automatique des enables après une connexion réussie.
- **Ports mémorisés par id + nom** (clé dédiée `kn2000_bk_io`), restauration par id puis par nom (les ids Web MIDI peuvent changer entre sessions), **tentative de connexion automatique** au chargement si les deux ports mémorisés sont retrouvés — comme la console.
- **Statut Web MIDI explicite** (ligne sous la connexion) : aucun port détecté, accès refusé (popup Chrome), sysex bloqué (page servie en file://), avec la procédure http://localhost — messages repris de la console.
- Moteur de dump inchangé (protocole v1.0.9) — revalidé par rejeu de la capture terrain 18/10 : ping → réponse → connecté, 70 ACK pour 70 paquets, 1 DRQ panel exact, bloc 40 à 1056/1088, export kn2000-catalog-v1 conforme.

### Sauvegarde / Catalogue v0.1 — nouvel outil : dump de toutes les aires, export JSON unique
- **Nouvelle page dédiée** `backup.html` (demande d'Yves après la validation terrain du dump panel v1.0.9) : cataloguer l'état complet du clavier comme une sauvegarde, en étendant le dump à toutes les aires du Reference Guide (p.24-27).
- **Moteur de dump générique** reprenant le protocole handshake validé par le terrain (v1.0.9 : exactement un ACK `f0 50 23 7e f7` après chaque paquet reçu — BTR, chaque CDD, EOK, END — jamais de re-DRQ) : réutilise la forme de DRQ validée sur le panel (ADR + SIZ `00 00 42` + CN 00 + SM XOR).
- **Quatre aires** (doc p.24-27), à valider une à une par le terrain : PANEL `40 00 00` (validé), SOUND MEMORY `30 00 00`, COMPOSER `50 00 00`, SEQUENCER `60 00 00`. Le type correspondant doit être sélectionné sur la page SYSEX BULK DUMP du clavier avant chaque dump ; le bouton « Enchaîner les aires restantes » les parcourt dans l'ordre (5 s pour changer le type au clavier). Le type « ALL » du clavier sera testé en dernier, une fois les 4 aires validées.
- **Lecture seule des enables sysex** (20 04 00/01/02, SIZ 1) comme la console — lancée automatiquement avant chaque dump si elle n'a pas encore été faite.
- **Export JSON unique horodaté** (`kn2000-catalogue-YYYY-MM-DD_HHhMM.json`), auto-documenté (format `kn2000-catalog-v1`) : métadonnées, état des enables, et pour chaque aire dumpée la liste des blocs (adresse, SIZ annoncé, octets reçus, données brutes dénibblisées en hex minuscule, `--` = octet manquant). Format pensé pour être décodé par une future appli Android de repli terrain — documenté dans la page d'aide.
- **Mystère documenté** : le bloc 40 du panel s'arrête systématiquement à 1056/1088 octets (écart 32 constant, sans conséquence) — l'écart est consigné dans le JSON.
- **Validation hors clavier** : le moteur a été rejoué sur la capture terrain validée du 18/10 (dump panel CtrlrX) — un ACK par paquet (70 ACK pour 70 paquets), fin sur END, bloc 40 à 1056/1088 conforme au terrain, structure d'export vérifiée. Les aires 30/50/60 restent à valider par le terrain.

### Panel Console v1.0.9 — ACK après chaque paquet (protocole handshake complet par le terrain)
- **Retour terrain v1.0.8** : lecture des enables OK (tout ON), handshake complet OK, et l'ACK-BTR a **débloqué le dump** — les paquets CDD ont coulé pour la première fois (245 puis 370/1088 octets). Mais le KN2000 s'est arrêté après le 2ᵉ paquet et a répondu ERR 7E + erreur 42 : il attendait l'ACK **du paquet**.
- **ACK après chaque paquet reçu** : BTR, CHAQUE paquet CDD (checksum OK), EOK, END — exactement un ACK par paquet, jamais deux. C'est le protocole de handshake complet : « état confirmé pendant la transmission » (p.25).
- **Finition** : les ACK « rétroactifs » de fin de bloc de la v1.0.8 sont supprimés (un ACK non précédé d'un paquet est un candidat rejet ERR) ; la clôture de bloc devient purement logique (journal + avertissement si incomplet), déclenchée par le compte d'octets (SIZ atteint), l'arrivée du BTR suivant, ou l'EOK/END.

### Panel Console v1.0.8 — ACK-BTR immédiat + fin de bloc corrigée (retour terrain v1.0.7)
- **Contexte du test terrain v1.0.7** : le handshake complet a fonctionné pour la première fois (DRQ → HRQ → HRT → HRT clavier → ACK → le dump est parti), mais le KN2000 a renvoyé le BTR du bloc 40 (2 puis 3 fois — cf. « 3 tentatives » p.25) puis a répondu ERR 7E avec erreur 42 affichée, **sans jamais envoyer les paquets CDD de suite**. Signature d'un émetteur qui attend une confirmation dès le BTR.
- **ACK immédiat à réception de chaque BTR** (checksum OK) — le correctif central, choix minimal validé par Yves : en handshake, l'état est confirmé PENDANT la transmission (p.25). Les paquets CDD éventuels ne sont pas ACKés à ce stade (à ajuster selon le retour terrain).
- **Correction majeure — fin de bloc** : la v1.0.7 attendait CN=00 dans les paquets CDD pour clore un bloc ; or les captures réelles du 18/10 montrent que **CN=00 n'est pas un marqueur de fin** (il apparaît en plein milieu des blocs). Désormais : ACK de fin de bloc par compte d'octets (SIZ atteint) **et** ACK rétroactif à l'arrivée du BTR suivant ou de l'EOK/END (le KN2000 enchaîne même si le bloc 40 s'arrête à 1056/1088 octets — écart systématique constaté sur toutes les captures, à documenter).
- **Lecture seule de l'état des enables sysex avant chaque bulk** (demande d'Yves) : DRQ sur 20 04 00/01/02 (Non-Real Time Sysex, Real Time Sysex, Real Time Command), SIZ 1 — les réponses sont loguées ON/OFF, **rien n'est écrit sur le clavier** (« on verra ensuite ce qu'on fait des réponses »).
- **ERR 29 gérée par le moteur de dump** : arrêt immédiat de la session (la v1.0.7 laissait filer un timeout fantôme de 9 s après l'erreur) ; message de timeout corrigé pour le cas observé (« BTR reçus mais pas de fin de dump » au lieu de « pas de HRQ reçu »).
- **Journal détaillé du dump** : chaque paquet CDD est logué (octets reçus du bloc en cours) et le récap final liste TOUS les blocs reçus (le bloc 40 était invisible en v1.0.7) avec avertissement si un bloc est clôt incomplet.

### Panel Console v1.0.7 — dump doc-compliant (protocole Reference Guide p.25-26 joué par le livre)
- **Handshake complet lors du dump panel** : sur HRQ (21) du clavier → envoi de la HRT (22) ; sur HRT (22) du clavier → envoi de l'ACK (23) ; fin de chaque bloc (Body of data complet) → ACK ; EOK (27) → ACK ; END (28) → ACK (séquence de confirmation p.25 + pattern p.26). Objectif : mode rapide et sans perte du protocole — l'ACK post-HRT n'avait jamais été testé proprement (la v1.0.5 avait été contaminée par un re-DRQ parasite, suspect n°1 de l'erreur 42).
- **Zéro re-DRQ automatique** (essai unique) : en cas d'échec, message explicite « OFF/ON du clavier ou re-afficher la page SYSEX BULK DUMP, puis Re-dump complet » — relance manuelle, cache-fusion conservé (un bloc 41 complet de 8064 octets fusionne ses 24 banques).
- **Fix message timeout** (bug v1.0.6 « dump muet ») : le timeout distingue désormais « pas de HRQ » (liaison/page BULK DUMP ?), « HRQ reçus mais dump pas parti » (erreur 42 ? écran clavier ?) et « handshake établi mais pas de bloc reçu ».
- **Fix doublons du journal** : le sysex Technics (ID 50) n'est plus re-logué en brut par le moniteur (les trames protocolaires sont déjà loguées par le moteur de dump).
- **Docs** : Firefox 108+ supporte le sysex Web MIDI — les mentions « Chrome/Edge uniquement » sont retirées (erreur de connexion et accueil).
- Évolutions internes depuis v1.0.1 (développement local 25-26/10, jamais publiées) : gestion des 6 fonctions MIDI (lecture/activation/rétablissement, v1.0.2), moteur de dump panel avec cache 24 PM (v1.0.3), filtre « ne traiter que les 2C » — les notes/CC n'étaient plus rejetées en « 2C checksum KO » (v1.0.5-1.0.6).

## 2026-10-25

### Panel Console v1.0.1 — corrections terrain v1.0
- **Trame d'erreur du KN2000 reconnue** (`f0 50 29 <code> f7`, ex. ERR 7E « adresse inexistante ou demande refusée ») : elle était traitée comme une réponse 2C et rejetée en « checksum KO » (diagnostic trompeur). Elle est maintenant identifiée, journalisée avec un message explicite, met `connOk` (la machine répond) et, pendant une lecture de PM, affiche « ✗ erreur KN2000 » dans le champ d'état au lieu d'attendre le timeout « aucune réponse ».
- **🧹 Vider le journal** : bouton dans un en-tête « 📜 Journal MIDI » affiché avec le journal (convention Sound Editor v0.9.15) — il manquait à la console.
- Trames sysex trop courtes (< 12 octets) ignorées proprement au lieu d'un décodage hasardeux.

### Panel Console v1.0 — charte d'ergonomie (alignée Sound Editor v0.9.13+)
- **Barre de badges** : 🎹 KN2000 (état liaison) et 📁 dossier à gauche, **titre centré**, à droite **case 📜 Log MIDI** (remplace le bouton Log ; marque les erreurs en rouge quand le journal est masqué) + bouton **⚙️ Réglages** — le bouton Moniteur quitte le bandeau haut.
- **Menu ⚙️ Réglages** (ouvert automatiquement si le KN2000 n'est pas connecté) : 🔌 Connexion (In/Out, 🔄 Ports, 🔌 Connecter, état), 📁 Dossier de travail, 🔎 Moniteur, 🌐 langue, Aide (SVG livre au trait).
- **Onglets** : ÉTAT · 📚 BIBLIOTHÈQUE · 🎵 SONS PART 4–16 · ⚙️ PANEL MEMORY — les sections dépliables `<details>` deviennent des onglets, chacun avec l'icône d'aide SVG pointant vers son ancre stable (`pc-01-etat` … `pc-04-panel-memory`).
- **Connexion fiabilisée** : ports mémorisés après « Connecter » (localStorage `kn2000_pc_io`) et **restaurés par nom** au chargement (repli ID, repli premier port) ; connexion automatique uniquement si les deux ports mémorisés sont retrouvés ; **test de réponse (DRQ léger) avant toute lecture d'état** ; pill d'état dédiée (⏳ / ● connecté / ⚠ pas de réponse).
- **Dossier de travail partagé** avec le Sound Editor (même base IndexedDB `kn2000_se`) : export de la bibliothèque écrit `kn2000-bibliotheque.json` dans le dossier ; l'import y lit d'abord, sinon sélecteur système / fichier (replis : téléchargement).
- **Squelette i18n FR/EN** (`STR`, localStorage `kn2000_pc_lang`) sur les éléments de la charte (badges, libellés des groupes, bouton langue).
- **Aide restructurée** (`help/panel-console.html`) : sections par onglet avec ancres stables, bloc Réglages, historique v1.0.

### Sound Editor v0.9.17 — icône d'aide en SVG (livre au trait)
- **Les icônes d'aide des 5 onglets et le bouton « Aide » des Réglages passent en SVG inline** (livre dessiné au trait, bleu #38bdf8) — remplace l'emoji 📖/❓ qui s'affichait en carré blanc sur les systèmes sans glyphe correspondant ; rendu garanti, taille et couleur contrôlées, indépendant des polices.

### Sound Editor v0.9.16 — pavé supérieur en 2 lignes, session dans la barre, R1/R2, 📖
- **Titre « 🎹 Sound Editor » remonté et centré** sur la ligne des badges (connexion · Log MIDI · ⚙️ Réglages).
- **Barre de travail en 2 lignes** — ligne 1 : Slot, Nom, ✍️, ⬇️ Export, et à droite le pavé session (état de l'original + ↩️ Rétablir l'original / ✅ Garder le nouveau son, sortis de l'onglet GENERAL) ; ligne 2 : 🔒 Écriture, ⚡ Auto-appliquer, état machine / paquets reçus, et à droite l'état des parts.
- **« 📍 Lire l'état » remplacé par un bouton 🔄** (rafraîchissement, à côté des indicateurs).
- **Indicateurs raccourcis** : RIGHT1 → R1, RIGHT2 → R2 (LEFT inchangé).
- **❓ des onglets remplacé par l'icône 📖** (documentation).

### Sound Editor v0.9.15 — finitions bandeau : ⚙️ à droite des badges, Log MIDI en case à cocher, 🧹 dans le journal
- **⚙️ Réglages** déplacé au niveau des badges de connexion, complètement à droite ; **📜 Journal devient une case à cocher « 📜 Log MIDI »**, juste avant le bouton Réglages.
- **🧹 Vider le log** quitte le bandeau haut et s'installe dans l'en-tête du journal sysex, à droite du titre (le journal ne se vide donc que là où il se lit).

### Sound Editor v0.9.14 — menu ⚙️ Réglages : tout le non-fonctionnel quitte le bandeau haut
- **Menu ⚙️ Réglages** (retour terrain v0.9.13 : les badges changeaient d'onglet alors que les réglages sont ailleurs) : nouveau panneau sous le bandeau regroupant 🔌 connexion KN2000 (🔄 Ports, In/Out, Connecter, état), 🎛 APC (ports, encodage, état — déplacé de l'onglet APC), 📁 dossier de travail (déplacé de l'onglet BANQUE), 🌐 langue · aide · démo (🇫🇷/🇬🇧, ❓ Aide, 🧪 Exemple A-1).
- **Badges** : le clic ouvre le menu Réglages sur la section correspondante (connexion / APC / dossier), plus aucun changement d'onglet.
- **Menu ouvert automatiquement au démarrage si le KN2000 n'est pas connecté** ; il se replie tout seul quand la connexion s'établit (sauf si l'utilisateur l'a ouvert lui-même).
- **Bandeau haut allégé** : ne reste que le fonctionnel — Slot, Nom, ✍️, ⬇️ Export JSON, 🧹/📜 Journal, 🔒 Écriture, ⚡ Auto-appliquer, 📍 Lire l'état, états des parts.

### Sound Editor v0.9.13 — charte d'ergonomie : badge d'état, commandes groupées, aide contextuelle, squelette i18n
- **Badge d'état discret** en haut de page : 🎹 KN2000 (● connecté / ⚠ pas de réponse / ○), 🎛 APC, 📁 dossier de travail — cliquer un badge ouvre l'onglet correspondant (GENERAL / APC / BANQUE). Synchronisé automatiquement avec l'état existant (observateurs), sans toucher aux flux MIDI.
- **Commandes regroupées sous labels** dans les onglets BANQUE et APC (et catalogue) : 🎚 Pilotage → 💾 Sauvegarder → 📂 Charger, même ordre partout — base du référentiel commun avant généralisation aux autres outils.
- **Aide contextuelle** : icône ❓ dans chaque onglet ouvre `help/sound-editor.html` directement sur sa section ; **nomenclature d'ancres stables et ordonnées** `se-<NN>-<onglet>` / `se-<NN>-<onglet>-<MM>-<section>` (`se-01-general`, `se-02-tone`, `se-02-01-enveloppe`, `se-03-effect`, `se-04-banque`, `se-04-01-catalogue`, `se-05-apc`, `se-06-journal`, `se-tips-volatile`) — réutilisable comme plan de documentation ; les ancres suivent la langue affichée (FR/EN).
- **Squelette i18n `STR`** (FR/EN) : dictionnaire en tête de script, clés alignées sur la taxonomie de la charte, bascule 🇫🇷/🇬🇧 dans le header (langue mémorisée) — couvre les éléments de la charte (badges, labels, bouton), migration progressive du reste à chaque MAJ.
- **Bannière allégée** : les avertissements longs migrent dans l'aide (encarts Tips 💡 FR/EN : badge, commandes groupées, aide contextuelle, volatilité ~80 min) ; seuls restent en page les avertissements critiques (dump avant écriture, ~80 min).

### Sound Editor v0.9.12 — dossier de travail généralisé à tous les exports/imports (chantiers b + d)
- **« Dossier de travail » unique** (l'ancien dossier catalogue est promu générique) : un seul bouton 📁, mémorisé par le navigateur (IndexedDB + permission), utilisé par **tous** les flux — catalogue, packages 📦, banque 🏦, export d'un son seul, config APC 🎛️.
- **Export config APC via le dossier** (chantier b) : plus aucun téléchargement Blob par défaut — la config s'écrit directement dans le dossier de travail (`kn2000-apc-config.json`) ; l'import lit d'abord ce fichier dans le dossier, sinon ouvre le sélecteur directement dedans. Repli Téléchargements/sélecteur classique si aucun dossier n'est choisi.
- **Tous les exports/import passent par le dossier** (chantier d, volet Sound Editor) : export banque, import banque, export d'un son seul suivent la même logique (écriture directe dans le dossier ; import : lecture du fichier attendu dans le dossier, sinon sélecteur ouvert dedans, sinon sélecteur classique).
- Le même mécanisme sera appliqué aux autres outils du projet (panel console, sysex tool, tone-table reader) lors de leur prochaine mise à jour.

### Sound Editor v0.9.11 — enveloppe : plateau Sus1→Dcy2 supprimé, règle « 50 = pente nulle » généralisée
- **Plateau artificiel supprimé** entre le point Sus 1 et la pente Dcy 2 (retour terrain : « ce bout d'horizontale, d'où vient-il ? Dcy 2 fait le lien direct ») : Dcy 2 part maintenant **exactement du point Sus 1**. Dcy 2 = 0 → bascule abrupte (verticale) sur la valeur Sus 2.
- **Règle « 50 = pente nulle » généralisée à tous les paramètres de pente** (Atk, Dcy 1, Dcy 2, Rls — validée terrain pour Release et confirmée pour Dcy 2) : à 50, la courbe part à l'horizontale jusqu'au bord droit avec un chevron — le stade ne se termine jamais (limite théorique, le clavier plafonne à 50). Atk 50 = ligne plate au départ, conforme à l'affichage du KN2000.
- Seul le plateau Sus 2 subsiste (note enfoncée), jusqu'au marqueur key-off.

### Sound Editor v0.9.10 — correctif enveloppe : échelle de temps fixe (chaque pente indépendante)
- **Correctif du bug v0.9.9** : l'échelle de temps était normalisée à chaque cas (remplissage du cadre), donc changer un paramètre redimensionnait toutes les pentes — Decay 1 modifiait visuellement la pente d'attaque. L'échelle est maintenant **fixe** : la pente de chaque segment (Atk, Dcy 1, Dcy 2, Rls) ne dépend que de son propre paramètre, comme sur l'écran du KN2000.
- **Sens de Sustain confirmé au clavier** : Sus 1 = 0 → la pente Dcy 1 court sur toute sa largeur jusqu'au sol ; Sus 1 = 50 → la pente disparaît (distance 0, palier direct au maximum).
- Si les réglages rendent la courbe plus longue que le cadre, elle est **coupée au bord droit avec un chevron** — le son continue au-delà (comme la ligne plate du KN2000 à Atk 50). Sinon, le silence/plateau final est prolongé jusqu'au bord.

### Sound Editor v0.9.9 — graphique d'enveloppe refait selon le modèle « points et pentes »
- **Graphique d'enveloppe redessiné** (onglet TONE) selon le modèle validé au clavier : Atk/Dcy1/Dcy2/Rls fixent des **pentes** (même loi de temps perceptive, exponentielle — validé : Atk perceptible dès 14, ~1-2 s à 34, ~5 s à 40), Sus1/Sus2 fixent des **niveaux**. Une pente court jusqu'au niveau cible : changer Sustain ne change plus la pente, seulement son point d'arrêt (comportement identique à l'affichage du KN2000). Dcy2 se dessine **montant** si Sus1 < Sus2. **Rls 50 = asymptote** : ligne horizontale, fin ouverte, le son ne s'éteint jamais (validé terrain). Valeur 0 = transition verticale. Échelle de temps indépendante des niveaux.
- **Documentation** : le modèle « points et pentes » (pentes vs niveaux, séquence ouverture → chute → palier → chute → palier → fermeture, tout jusqu'à Sus2 joue touche tenue) est intégré à l'aide FR/EN. La courbe reste une **indication visuelle**, pas un outil de mesure.
- **Orthographe** : titre de section « Envelope » → « Enveloppe ».

### Sound Editor v0.9.8 — section Filter réorganisée
- **Section Filter réorganisée** dans l'ordre de l'écran KN2000 : Filter type (THRU-6) en tête, puis Touch depth / Touch curve / Slope, et la paire Flt key lim en fin de section. Tout est regroupé dans une seule section (avant : type et key limits sur une ligne isolée hors section). Le slider Cutoff est conservé (présent dans la structure 144 octets même s'il n'apparaît pas sur cet écran).
- Validation terrain v0.9.7 enregistrée : Enveloppe Key Follow OK (slopes, limits, présentation) — offsets Release (bloc+25/26) confirmés par contrôle croisé.

### Sound Editor v0.9.7 — section EKF réorganisée + key limits de Release
- **Section « Enveloppe Key Follow » réorganisée** comme sur l'écran du KN2000 : pour chacun des trois étages ATK / DCY / REL, le Slope et la paire de Key Limits sont regroupés côte à côte (avant : slopes dispersés dans l'enveloppe, key limits d'Atk/Dcy seules sur une ligne, Release absent).
- **Key limits de Release ajoutées** : la paire Rel KF lim est maintenant affichée et éditable (sélecteurs C0→G8, écriture read-modify-write armé 🔓), aux offsets bloc+25/+26 déjà lus par le parseur. ⚠️ Offsets Release restant à valider par contrôle croisé (éditer au clavier → re-dump → comparer l'octet).

### Sound Editor v0.9.6 — correctifs Sustain pedal et affichage key limits
- **Sustain pedal corrigé** : le segment LONG/HOLD (onglet GENERAL, carte Controllers) était absent de la page — l'entrée `sustain` était accidentellement avalée par un commentaire de code. Le segment s'affiche et écrit à nouveau (bit 2 de 0x10, armé 🔓 requis).
- **Affichage des key limits aligné sur le clavier** : la page affichait un octave trop haut (C5 sur la page = C4 sur le clavier) ; les libellés sont maintenant alignés sur l'affichage du KN2000 (octave −1). Plage des sélecteurs étendue 24..127 (C0..G8) : une limite réglée sur C0 au clavier n'affichait plus de valeur (24 était hors de l'ancienne plage 29..127).

### Sound Editor v0.9.5 — restauration des ports par nom + key limits modifiables
- **Restauration des ports par nom** : au rechargement, si l'ID Web MIDI mémorisé n'est plus trouvé (les IDs peuvent changer entre sessions), l'éditeur retombe sur le port de même **nom** avant de prendre le premier de la liste. Les noms In/Out sont mémorisés aux côtés des IDs ; logs explicites (« Ports mémorisés : In = … restaurés »).
- **Key limits modifiables** : les limites de clavier par tone (Atk KF lim, Dcy KF lim, Flt key lim — bas 29..127, affichage F1→G9) sont maintenant éditables via des sélecteurs et écrites dans le son (read-modify-write, armé 🔓 requis), au lieu d'être seulement affichées.
- Aucun autre changement : Mod Wave (0x19 bits 0-1), Mod Mode (bits 4-5), LEDs pads (0x96) et protocole de connexion v0.9.3 sont conservés tels quels (validés terrain).

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

