# STATE.md — État d'avancement de CreateBrowser

> Instantané au 2026-09-30. Branche `master`, version `0.3.0`.
> Légende : ✅ fait · 🟡 partiel · ⛔ manquant · 🐛 bug/dette

---

## Vue d'ensemble

| Phase | Statut réel | Commentaire |
|---|---|---|
| 0 — Setup | ✅ | Bootstrap Forge 1.20.1, keybind `N`, config TOML, SPI. |
| 1 — Navigateur de base | 🟡 | Code complet et aligné sur l'API réelle. ⛔ Jamais validé en jeu contre createmod.com. |
| 2 — Enrichissement | ✅ | Favoris/historique/local/thumbnails, filtres tri+catégorie+taille, SettingsScreen. |
| 3 — Upload + intégration Create | 🟡 | Upload anonyme, badges compat, CreateBridge. Mixin corrigé (SRG) mais non testé en production. |
| 4 — Multi-sources & robustesse | 🟡 | `SourceRegistry`, cache, hors-ligne. ⛔ 2e source réseau, NeoForge. |
| 5 — Preview 3D / Quick-Share / i18n | 🟡 | i18n FR+EN, quick-share. ⛔ Preview 3D. |
| 6 — Polish / publication | ⛔ | `./gradlew build` vert (71 tests). Validation in-game et release à faire. |

---

## Fait le 2026-09-30 (v0.3.0)

Audit du client contre le code source de createmod.com (`uberswe/createmod.com`) :

- 🐛→✅ **Téléchargement** : `GET /api/schematics/{name}/download` avec la clé API. Le `modSecret` n'est plus
  requis ; l'HMAC `/api/mod/download` reste en repli si seul un secret est configuré.
- 🐛→✅ **Pagination** : le param `pageSize` était ignoré par l'API → `per_page` (8/16/24/32/64/100).
- 🐛→✅ **Tri** : `sort=recent` était ignoré → codes numériques (1 pertinence, 2 récents, 4 notes, 6 vues).
  Nouveau tri « Pertinence » par défaut. « Téléchargements » reste client (pas de tri serveur).
- 🐛→✅ **Modèles JSON** : images (`featuredImage`/`gallery` → URLs `/api/files`), `blockCount`, `dimX/Y/Z`,
  `content`. Les thumbnails et les dimensions ne pouvaient jamais s'afficher.
- ✅ **Filtre taille** (client) : `blockCount` est présent dans les résultats de recherche.
- 🐛→✅ **Clé API modifiée en jeu** ignorée jusqu'au redémarrage (snapshot figé) → `ApiConfig` record +
  reconstruction des clients.
- 🐛→✅ **Erreurs** affichées en codes bruts (`Search failed: no_api_key`) ; erreurs de download jamais
  affichées → `gui/ErrorText` + clés de langue. Clé `createbrowser.screen.settings` manquante ajoutée.
- 🐛→✅ **Mixin** : `remap = false` + `method = "init"` ne matche pas en production Forge 1.20.1 (SRG) →
  cible `init` + `m_7856_`. Le bouton n'apparaissait jamais hors dev.
- ✅ Build : encodage UTF-8 (javadoc cassée en locale ASCII), plugin fabric-loom inutile retiré,
  `minecraft_version_range=[1.20.1]`, Forge `[47,)`, CI sur `master`, `ConstantsTest` anti-dérive de version.
- ✅ Docs : `CLAUDE.md` (référence API réelle, Mixin, config, CI), `README.md`, chantiers dans `docs/issues/`.

## Restant (par priorité)

| # | Chantier | Fichier | Bloquant |
|---|---|---|---|
| 1 | Validation in-game (checklist) | `docs/issues/01-validation-in-game.md` | Oui — tout le reste |
| 2 | Partenariat createmod.com (secret HMAC dédié, accord, PNG thumbs) | `docs/issues/02-partenariat-createmod.md` | Pour publication |
| 3 | Port NeoForge 1.21.1 (branche `mc1.21`) | `docs/issues/03-port-neoforge-1.21.1.md` | Non |
| 4 | Filtres serveur + trending à l'ouverture | `docs/issues/04-filtres-serveur-et-browse.md` | Non |
| 5 | Vignettes WebP/JPEG | `docs/issues/05-vignettes-webp.md` | Non |
| 6 | Preview 3D | `docs/issues/06-preview-3d.md` | Non |
| 7 | 2e source réseau | `docs/issues/07-deuxieme-source.md` | Non |
| 8 | Publication Modrinth/CurseForge | `docs/issues/08-publication.md` | Après 1 et 2 |

Les fichiers `docs/issues/*.md` sont rédigés pour être copiés tels quels en issues GitHub
(titre = première ligne).

## Contraintes connues

- Recherche/détail/download exigent une clé API (self-service, 120 req/min) ou un secret HMAC.
- createmod.com est derrière Cloudflare : pas de test réseau possible depuis la CI/cloud.
- Filtres catégorie et taille : client-side, page courante uniquement (voir chantier 4).
- Tests GUI : manuels uniquement.
