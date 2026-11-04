# KN Bridge

Appli Android minimale (Java, ~40 lignes utiles) qui expose un **port MIDI virtuel** 
via le MidiService d'Android — l'équivalent gratuit de loopMIDI sur tablette.

- Port d'entrée **Commande MS** : MobileSheets y envoie ses commandes
  (Réglages → MIDI → Configure MIDI Connections → bibliothèque **Google/Android**).
- Port de sortie **Vers Page** : tout ce qui arrive sur l'entrée est transmis tel
  quel (octets bruts, sysex F0 7D 00 &lt;msid&gt; F7 inclus) vers la sortie, que
  Chrome écoute (page kn2000-apc-live-v2.html).

L'APK est compilé automatiquement par GitHub Actions à chaque modification :
voir les Releases (tag knbridge-v1) pour télécharger `app-debug.apk`.
