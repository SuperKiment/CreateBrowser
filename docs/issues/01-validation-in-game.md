# Validation in-game de la v0.3.0 (Forge 1.20.1) avant toute release

## Contexte

Le mod n'a jamais été testé en jeu contre la vraie API createmod.com. Les tests unitaires (71, verts) utilisent des fixtures. Le client a été aligné sur le code source réel de createmod.com (`uberswe/createmod.com` : `internal/router/main.go`, `internal/pages/api_public.go`, `internal/models/schematic.go`), mais rien n'a été vérifié en conditions réelles : createmod.com renvoie 403 (Cloudflare) aux environnements cloud/CI.

Bloque : publication (issue 08).

## Prérequis

1. Compte createmod.com.
2. Clé API sur https://createmod.com/settings/api-keys (self-service, 120 req/min par défaut).
3. `./gradlew :forge:runClient`, touche `N`, ⚙, coller la clé.

## Checklist

### Recherche
- [ ] Résultats affichés (titre, auteur, stats).
- [ ] `ui.pageSize = 8` dans le TOML donne 8 résultats par page (param `per_page`).
- [ ] Tri Pertinence / Plus récents / Mieux notés / Plus vus : ordre global (re-requête, `sort` = 1/2/4/6). Plus téléchargés : tri client de la page seulement.
- [ ] Filtre catégorie : noms corrects (API renvoie des objets `{id,key,name}`).
- [ ] Filtre taille Petit/Moyen/Grand selon `blockCount`.
- [ ] Pagination 1, 2, 1.
- [ ] Clé vide : « Clé API manquante — renseignez-la dans les Paramètres ».
- [ ] Clé changée dans ⚙ : prise en compte sans redémarrage.

### Détail
- [ ] Description (`content`), dimensions (`dimX/dimY/dimZ`), `blockCount`, matériaux (JSON encodé en string), mods requis + badges.
- [ ] Vignettes (voir issue 05 : WebP non décodable par `NativeImage`).

### Téléchargement
- [ ] `GET /api/schematics/{name}/download` + clé : 302 suivi, `.nbt` écrit dans `schematics/`.
- [ ] Validation GZip, message chat, historique.
- [ ] Fichier visible dans la Schematic Table sans redémarrage.

### Intégration Create
- [ ] Bouton « Browse Online » visible en dev **et** en production (jar dans une vraie instance). Le Mixin cible `init` + `m_7856_` (SRG). Avant ce correctif, le bouton n'apparaissait jamais hors dev.
- [ ] Position correcte aux GUI scale 1, 2, 3, Auto (actuellement relative au centre écran, pas au fond du GUI).
- [ ] Cohabitation avec Create: Schematic Helper (injecte aussi dans `SchematicTableScreen.init`, priorité 1500).
- [ ] Create 0.5.1 et Create 6.0.x (1.20.1) : `com.simibubi.create.content.schematics.table.SchematicTableScreen` présent.
- [ ] Sans Create : chargement OK, pas de crash Mixin.

### Upload
- [ ] `/api/schematics/upload-anonymous` : lien dans le chat.

### Divers
- [ ] Hors-ligne : bannière + cache.
- [ ] Onglets Favoris / Historique / Mes schematics.
- [ ] Aucun freeze du game thread.

## Si un point échoue

Référence API : `CLAUDE.md`, section API createmod.com. Le code serveur fait foi, pas `/api/openapi.json` (partiellement en retard, ex. `type` de `/api/mod/download` : code = `schematic`/`upload`, OpenAPI = `name`/`id`).
