# Filtres serveur (catégorie, tags, versions, mods) et navigation sans requête

## Constat

`GET /api/schematics` supporte (source `parseAPISearchQuery`) : `category` (clé), `tag` (clés, virgules), `mcv`, `cv` (version Create ou groupe `~6.0`), `rating` (min 0-5), `mod`/`mods`, `sort` 1-8. Sans `query` ni `sort`, l'API renvoie le **trending**.

Aujourd'hui le mod ne filtre catégorie/taille que côté client, sur la page courante. Le tri est serveur depuis v0.3.0.

`GET /api/schematics/filters` renvoie les listes d'options (catégories, versions MC, versions Create, tags, mods).

## Travail

- [ ] `ApiClient.getFilters()` + cache TTL long (1 j).
- [ ] Dropdown catégorie alimenté par `/filters` (clé envoyée, nom affiché) au lieu des catégories de la page.
- [ ] Filtre version Create auto-sélectionné depuis `Services.CREATE.getCreateVersion()` (ex. `~6.0`).
- [ ] Filtre version MC par défaut = `Constants.MC_VERSION`.
- [ ] Filtre « compatible avec mes mods » : `mods` = mods installés.
- [ ] Ouverture du navigateur : charger le trending (requête vide) au lieu de l'écran vide. Retirer le `isBlank()` de `performSearch` pour l'onglet recherche initial.
- [ ] Étendre `SearchFilters` et la clé de cache.
- [ ] Taille : reste client (pas de param serveur `blockCount`).
