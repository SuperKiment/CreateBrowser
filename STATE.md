# STATE.md — État d'avancement de CreateBrowser

> Instantané au 2026-07-09. Branche `master`, version `0.1.1`.
> Légende : ✅ fait · 🟡 partiel · ⛔ manquant · 🐛 bug/dette

---

## Vue d'ensemble

| Phase | Statut réel | Commentaire |
|---|---|---|
| 0 — Setup | ✅ | Bootstrap Forge 1.20.1, keybind `N`, config TOML, SPI. |
| 1 — Navigateur de base | ✅ | Recherche → détail → download `.nbt` + chat. |
| 2 — Enrichissement | ✅ | Favoris/historique/local/thumbnails, filtres tri+catégorie, SettingsScreen. |
| 3 — Upload + intégration Create | ✅ | Upload anonyme, Mixin bouton, badges compat, CreateBridge câblé (badge version). |
| 4 — Multi-sources & robustesse | 🟡 | `SourceRegistry` + 2 sources (createmod.com, local), cache complet (recherche+détail, LRU borné), mode hors-ligne. ⛔ Pas de 2e source réseau, pas de NeoForge. |
| 5 — Preview 3D / Quick-Share / i18n | 🟡 | i18n FR+EN faite. ⛔ Preview 3D et quick-share absents. |
| 6 — Polish / publication | 🟡 | Tests verts (`./gradlew build test`). ⛔ Pas de release Modrinth/CurseForge. |

**Git** : tout le travail Phases 1–3 + robustesse est commité par module sur `master`.

---

## Fait depuis l'instantané du 2026-07-08

- ✅ Versions alignées (`0.1.1` partout : gradle.properties, Constants, README).
- ✅ `./gradlew build test` vert (65+ tests).
- ✅ Cache détail branché (`detail:<source>:<name>`, TTL `detailTtlMinutes`).
- ✅ `evictLru` branché : throttlé (1×/min max) après chaque écriture cache, borne `cache.maxSizeMB`.
- ✅ Toutes les lectures cache déplacées hors du game thread (dans le supplier AsyncExecutor).
- ✅ FavoritesStore/HistoryStore : tous les accès GUI passent par AsyncExecutor (trouvé par review : 2🔴 3🟡).
- ✅ CreateBridge câblé : badge « Create x.y.z détecté » dans le footer du BrowserScreen.
- ✅ `Services.CONFIG` avec fallback `BrowserConfig.DEFAULT` (tests unitaires sans SPI).
- ✅ SettingsScreen in-game : apiKey, modSecret, toggle thumbnails (setters `BrowserConfig` + persistance ForgeConfigSpec).
- ✅ TTL recherche/détail exposés dans le TOML (`cache.searchTtlMinutes` / `cache.detailTtlMinutes`).
- ✅ `SourceRegistry` : les écrans ne construisent plus `CreateModComSource` en dur.
- ✅ Filtre catégorie (dropdown, client-side sur la page courante).
- ✅ Mode hors-ligne : fallback cache périmé + bannière `screen.offline` si le réseau échoue.
- ✅ `onClientSetup` placeholder supprimé.

## Restant (par priorité)

1. ⛔ **Quick-share** : bouton « Share » dans DetailScreen → copie l'URL createmod.com dans le presse-papier.
2. ⛔ **Filtre taille** : impossible client-side (pas de `block_count` dans les résultats de recherche). Nécessite support API.
3. ⛔ **2e source réseau** (schematicannon.com) : API non documentée — bloqué tant que pas de spec.
4. ⛔ **NeoForge 1.21.1** : le MultiLoader actuel compile `common/` contre MC 1.20.1 ; un port 1.21.1 exige une branche dédiée (mappings/GuiGraphics différents). Décision : branche `mc1.21`, pas un sous-projet.
5. ⛔ **Preview 3D** (Phase 5) : gros chantier rendu ; flag config `enable3dPreview` prévu, non commencé.
6. ⛔ **Publication** Modrinth/CurseForge : nécessite comptes + validation manuelle in-game.

## Contraintes connues

- Download gaté par `modSecret` (secret partagé uberswe) ; search/detail exigent `apiKey`.
- Tri client-side (l'API createmod.com ignore les params de tri).
- Tests GUI : manuels uniquement (pas d'automatisation in-game).
