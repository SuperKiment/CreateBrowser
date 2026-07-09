# STATE.md — État d'avancement de CreateBrowser

> Instantané au 2026-07-08. Basé sur une lecture exhaustive du code (branche `master`, version `0.1.1`).
> Légende : ✅ fait · 🟡 partiel · ⛔ manquant · 🐛 bug/dette

---

## Vue d'ensemble

| Phase | Statut réel | Commentaire |
|---|---|---|
| 0 — Setup | ✅ | Bootstrap Forge 1.20.1, keybind `N`, config TOML, SPI. |
| 1 — Navigateur de base | ✅ | Recherche → détail → download `.nbt` + chat. |
| 2 — Enrichissement | 🟡 | Favoris/historique/local/thumbnails OK ; filtres partiels ; pas d'écran settings. |
| 3 — Upload + intégration Create | 🟡 | Upload anonyme OK ; Mixin bouton OK ; badges compat OK ; **CreateBridge dormant**. |
| 4 — Multi-sources & robustesse | 🟡 | Interface `SchematicSource` + 2 sources ; cache partiel ; **pas de NeoForge**, pas de registre de sources. |
| 5 — Preview 3D / Quick-Share / i18n | 🟡 | i18n FR+EN faite ; **preview 3D et quick-share absents**. |
| 6 — Polish / publication | ⛔ | Tests présents mais non vérifiés ; pas de release. |

**Non commité** : quasiment tout le mod (git n'a que 2 commits Phase 0). Travail Phases 1–3 dans le working tree, pas encore versionné.

---

## Détail par module

### `api/` — Client HTTP ✅
- `ApiClient` : search + detail + upload, retry exponentiel sur 5xx, gestion 401/429. ✅
- `RateLimiter` : token bucket / intervalle min. ✅
- `ModDownloadClient` : download `.nbt` via `POST /api/mod/download`, HMAC-SHA256 + décodage XOR-v1. ✅
- 🟡 **Download gaté par `modSecret`** (secret partagé fourni par uberswe). Sans secret → `no_mod_secret`. La majorité des utilisateurs ne pourront pas télécharger.
- 🟡 Search/detail exigent une `apiKey` (`get()` throw `no_api_key` si vide).
- 🟡 Filtres API : `sort` envoyé mais **le tri est fait client-side** (l'API createmod.com ne supporte pas les params). `category`/`size` jamais appliqués.

### `api/source/` — Sources 🟡
- `SchematicSource` (interface), `CreateModComSource`, `LocalSource`. ✅
- ⛔ **Pas de registre de sources** ni de sélection via config (`sources.active` du CLAUDE.md non implémenté). `BrowserScreen`/`DetailScreen` instancient `CreateModComSource` en dur.
- ⛔ Une seule source réseau (createmod.com). `schematicannon.com` jamais implémentée.

### `gui/` — Écrans 🟡
- `BrowserScreen` : onglets Search/Favorites/History/Local, pagination, tri, états IDLE/LOADING/RESULTS/EMPTY/ERROR. ✅
- `DetailScreen` : titre, auteur, rating, dims, block count, description, badges mods installés/manquants, download. ✅
- `LocalManagerScreen` : rename / delete / publish d'un `.nbt` local. ✅
- `UploadScreen` : confirmation + upload anonyme, garde-fou taille 10 MB. ✅
- `widget/` : `SearchBarWidget` (debounce), `SchematicListWidget`, `PaginationWidget`, `TabBar`, `FilterDropdown`, `ThumbnailWidget` (NativeImage+DynamicTexture async, dédup par URL). ✅
- ⛔ **`SettingsScreen` absent** (listé dans CLAUDE.md). Config uniquement via TOML.
- 🟡 `FilterDropdown` n'expose que le tri. Pas de filtre catégorie/taille en UI.

### `storage/` 🟡
- `SchematicFileManager` : save (validation header GZip `1F 8B`), listLocal, delete, rename, résolution de conflits `_1`, `_2`. ✅
- `FavoritesStore`, `HistoryStore` : JSON Gson. ✅
- `PathResolver`, `LocalFile`. ✅
- `CacheManager` : `getApi`/`putApi` avec TTL, `evictLru(maxBytes)` implémenté. 🟡
- 🐛 **`evictLru` jamais appelé** → cache disque non borné. `cacheMaxSizeMB` = config morte.
- 🐛 **Seuls les résultats de recherche sont cachés.** `DetailScreen` tape l'API à chaque ouverture — pas de cache détail. `detailTtlMin` = config morte.

### `nbt/` ✅
- `NbtMetadataReader` : lecture header `.nbt`, utilisé par `SchematicFileManager`.

### `compat/` — CreateBridge 🟡🐛
- `CreateBridge` (interface + NOOP) + `CreateBridgeForge` (ModList). Chargé via `Services.CREATE` (SPI). ✅
- 🐛 **Bridge quasi inutilisé** : `injectBrowseButton()` jamais appelé (le bouton passe par le Mixin), `getCreateVersion()`/`getInstalledCreateAddons()` jamais consommés par l'UI. Détection compat des mods dans `DetailScreen` passe par `Services.PLATFORM.isModLoaded`, pas par le bridge.

### `config/` ✅
- Interface `BrowserConfig` + `DEFAULT` (fallback tests) + impl Forge `ForgeBrowserConfig` (TOML). ✅
- 🟡 Champs `cacheMaxSizeMB` / `detailTtlMin` exposés mais non utilisés (voir cache).

### `util/` / `platform/` ✅
- `AsyncExecutor` (pool 3 threads daemon, dispatch game thread), `FileNameResolver`. ✅
- SPI `Services` : `IPlatformHelper`, `ChatNotifier`, `BrowserConfig`, `CreateBridge`. ✅

### `forge/` 🟡
- `@Mod` entry, keybind, ModConfig TOML, impls SPI, `SchematicTableScreenMixin` (`required:false`, `targets` string, ciblé sur `...schematics.table.SchematicTableScreen`). ✅
- 🟡 `build.gradle` **ne déclare pas Create en `compileOnly`** — le Mixin cible par string, donc compile sans Create, mais aucune API Create typée n'est disponible côté loader.
- 🐛 `onClientSetup` = placeholder vide « Phase 0 ».

---

## Manques transverses

- ⛔ **NeoForge** : sous-projet absent (README dit Phase 4). Uniquement Forge 1.20.1.
- ⛔ **Fabric** : supprimé (commit `e4153f7`).
- ⛔ **Preview 3D in-game** (Phase 5).
- ⛔ **Quick-Share par lien** (Phase 5).
- ⛔ **Écran de config in-game** (`SettingsScreen`).
- ⛔ **Mode hors-ligne explicite** : clé lang `screen.offline` existe mais aucune bannière/logique de bascule branchée.
- ⛔ **Multi-sources réel** : 2e source réseau + registre config-driven.
- ⛔ **Publication** Modrinth/CurseForge, changelog, release.

## Bugs / dette à traiter

1. 🐛 Cache disque non borné — brancher `evictLru(cacheMaxSizeMB)`.
2. 🐛 Aucun cache sur les détails — ajouter `getApi/putApi` dans `DetailScreen` avec `detailTtlMin`.
3. 🐛 `CreateBridge` dormant — soit le câbler (version/addons/inject), soit le retirer.
4. 🐛 `onClientSetup` vide à nettoyer.
5. 🐛 Incohérence version : `gradle.properties` = `0.1.1`, README parle de `0.1.0`.
6. 🐛 Artefacts de run commitables : `forge/run/` (mondes, cache, favorites/history) — à `.gitignore`.
7. 🐛 Tout le travail Phases 1–3 non commité (2 commits seulement).

## Tests

- 11 fichiers de test (`api`, `storage`, `nbt`, `util`) : RateLimiter, multipart, download, SearchResult parse, cache, favorites/history, filenames, NBT.
- ⚠️ **Non exécutés ici** (build hors-ligne impossible : deps moddev/loom non cachées). Statut pass/fail à confirmer avec réseau : `./gradlew :common:test`.
- ⛔ Pas de test GUI (attendu — tests manuels).

## Prochaines étapes suggérées (ordre)

1. Commiter le travail Phases 1–3 (découpé par module, préfixes `[api]`/`[gui]`/…).
2. Vérifier `./gradlew build test` en ligne, corriger si rouge.
3. Brancher cache détail + `evictLru` (gains robustesse rapides).
4. Décider du sort de `CreateBridge` (câbler ou supprimer).
5. `SettingsScreen` + finaliser filtres, ou attaquer NeoForge selon priorité.
