# Vignettes : formats non décodables par Minecraft (WebP)

## Constat

- L'API renvoie `featuredImage` et `gallery` comme noms de fichiers ; le mod construit `https://createmod.com/api/files/schematics/{id}/{fichier}` (v0.3.0).
- `?thumb=WxH` renvoie du **WebP** (`files.go`, cache `_thumbs/.../*.webp`). Le mod ne l'utilise donc pas et télécharge l'image originale (plus lourde).
- `NativeImage.read` (STB) ne décode que PNG (et quelques formats via STB) ; pas le WebP. Si l'original est en WebP ou JPEG selon l'upload, la vignette échoue.

## Options

1. Demander à uberswe un paramètre `format=png` sur `?thumb=` (issue 02). Meilleure solution : image petite + format lisible.
2. Décodage côté client : `javax.imageio.ImageIO` lit PNG/JPEG/BMP/GIF (pas WebP) puis réencodage PNG → `NativeImage`. Couvre JPEG, pas WebP.
3. Décodeur WebP embarqué : contraire à la règle « pas de dépendance externe ».

## Travail

- [ ] Vérifier en jeu le format réel des images (issue 01).
- [ ] Implémenter l'option 2 dans `ThumbnailWidget` en repli.
- [ ] Passer à `?thumb=` dès que l'option 1 existe.
- [ ] Placeholder propre quand l'image est indécodable.
