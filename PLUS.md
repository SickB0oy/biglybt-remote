# Torrent TV (BiglyBT Plus)

Version modifiée de [BiglyBT pour Android](https://github.com/BiglySoftware/BiglyBT-Android)
(licence GPL), mise à jour et corrigée. Nom de paquet distinct
(`app.torrenttv`) : s'installe à côté de BiglyBT officiel.

## Corrections

- **Disques USB / cartes SD sur Android TV** (ticket #250) : demande l'autorisation
  « Accès à tous les fichiers », sans laquelle les torrents contenant des dossiers échouaient
  avec « Folder Permissions Needed ».
- **Android 14 à 16** : cible Android 15, déclare le type du service en premier plan
  (`specialUse`, sans limite de durée et autorisé au démarrage de l'appareil), conserve
  l'affichage classique (pas d'edge-to-edge forcé).
- **Plantage au démarrage du service** : quand Android refusait le passage en premier plan,
  l'erreur était ignorée et le système tuait l'appli quelques secondes plus tard ; le service
  s'arrête désormais proprement.
- **Accès à distance** :
  - le mot de passe n'est plus écrit en clair dans les journaux ;
  - quand « exiger un mot de passe » est coché, il est exigé aussi pour les appareils du réseau
    local (avant, toutes les adresses 192.168.x.x, 10.x.x.x et 172.16-31.x.x en étaient dispensées) ;
  - le mot de passe par défaut est réellement aléatoire (des lettres, et non une suite de
    chiffres à cause d'une erreur de type), enregistré une fois pour toutes, et exigé par défaut.
- **Option « empêcher la mise en veille »** : utilisait un verrou Wi-Fi sans effet depuis
  Android 10 ; utilise désormais le mode haute performance.

## Compilation

Outils mis à jour (Android Gradle Plugin 8.7, Gradle 8.11). L'APK est compilé et publié par
GitHub Actions à chaque modification de `master`. Secrets nécessaires pour signer :
`KEYSTORE_BASE64` et `KEYSTORE_PASSWORD` (alias de clé `libretorrent-remote`).
