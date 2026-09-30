# CreateBrowser

Mod Minecraft client-side qui ajoute un navigateur de schematics in-game pour le mod [Create](https://www.curseforge.com/minecraft/mc-mods/create). Recherche, prévisualisation et téléchargement de fichiers `.nbt` depuis [createmod.com](https://createmod.com) directement dans Minecraft, sans quitter le jeu.

- **Mod ID** : `createbrowser`
- **Minecraft** : 1.20.1
- **Loader** : Forge `47.x` (NeoForge 1.21.1 prévu, voir `docs/issues/03-port-neoforge-1.21.1.md`)
- **Java** : 17
- **Licence** : MIT

## Fonctionnalités

- Touche `N` ouvre le navigateur in-game
- Recherche createmod.com : pagination, tri serveur (pertinence, récents, notes, vues), filtres catégorie et taille
- Page de détail : dimensions, nombre de blocs, matériaux, mods requis avec badges installé/manquant
- Téléchargement `.nbt` vers `<instance>/schematics/`, immédiatement utilisable dans la Schematic Table de Create
- Favoris, historique, gestion des fichiers locaux (renommer, supprimer, ouvrir le dossier)
- Publication anonyme vers createmod.com, partage de lien
- Bouton « Browse Online » dans la Schematic Table de Create (si Create est installé)
- Cache disque avec mode hors-ligne, paramètres in-game, FR + EN

Recherche, détail et téléchargement nécessitent une **clé API createmod.com gratuite**, générée sur
https://createmod.com/settings/api-keys (compte requis).

## Architecture

Projet basé sur le [MultiLoader Template](https://github.com/jaredlll08/MultiLoader-Template) — une seule source de vérité dans `common/`, du bootstrap minimal par loader.

```
common/   ← Toute la logique métier (Minecraft vanilla uniquement)
forge/    ← Bootstrap Forge : @Mod, keybind, ModConfig TOML, services SPI
```

### Règle d'isolation

`common/` n'importe **jamais** `net.minecraftforge.*`, `net.neoforged.*`, ni `com.simibubi.create.*`. Toute interaction loader-spécifique (config, chat, intégration Create) passe par une interface dans `common/platform/services/` injectée via `java.util.ServiceLoader` (SPI).

### Modules clés (`common/`)

| Package | Rôle |
|---|---|
| `api/` | Client HTTP `java.net.http.HttpClient`, rate limiter token-bucket, retry exponentiel sur 5xx |
| `api/source/` | Interface `SchematicSource`, `CreateModComSource`, `LocalSource`, `SourceRegistry` |
| `api/model/` | DTO Gson immuables |
| `gui/` | `BrowserScreen`, `DetailScreen` et widgets vanilla MC (pas de framework GUI tiers) |
| `storage/` | Écriture des `.nbt` dans `<instance>/schematics/` avec validation header GZip et résolution de conflits de nom |
| `config/` | Interface `BrowserConfig` exposée via SPI |
| `util/` | `AsyncExecutor` (pool 3 threads daemon, dispatch retour vers game thread) |

### Threading

Aucune I/O réseau ou disque sur le game thread. Tout passe par `AsyncExecutor.run(supplier, onSuccess, onError)` qui exécute la tâche sur un pool dédié et redispatche le résultat vers le game thread via `Minecraft.getInstance().execute(...)`.

## Dépendances

**Aucune dépendance externe à l'exécution.**

| Besoin | Solution |
|---|---|
| HTTP client | `java.net.http.HttpClient` (JDK 17 natif) |
| JSON | `com.google.gson.Gson` (déjà bundlé dans Minecraft) |
| NBT | `net.minecraft.nbt.*` (vanilla) |
| Crypto (HMAC-SHA256, SHA-256) | `javax.crypto`, `java.security.MessageDigest` (JDK natif) |

**Tests** (compileTest only) :
- JUnit Jupiter `5.10.0`
- Mockito Core `5.7.0`

**Compile-only** : Forge `47.4.2` (1.20.1).

## Compilation

Prérequis : **JDK 17** (Temurin recommandé).

```bash
# Build complet (common + forge JAR)
./gradlew build

# Tests unitaires uniquement
./gradlew :common:test

# Lancer le client de dev Forge avec le mod chargé
./gradlew :forge:runClient
```

Artefact produit : `forge/build/libs/createbrowser-forge-1.20.1-0.3.0.jar`.

CI GitHub Actions configurée dans `.github/workflows/build.yml` (push sur `master`/`dev`, pull requests vers `master`).

## Installation

1. Télécharger `createbrowser-forge-1.20.1-0.3.0.jar` depuis les releases (ou builder localement, voir ci-dessus).
2. Le placer dans `<instance Minecraft>/mods/`.
3. Avoir Forge `47.x` pour MC 1.20.1.
4. Le mod [Create](https://www.curseforge.com/minecraft/mc-mods/create) est **optionnel** — sans Create, le navigateur reste utilisable et écrit dans `schematics/` (utile pour Litematica ou d'autres mods compatibles avec ce format).
5. En jeu, taper `N`, cliquer ⚙ et coller la clé API createmod.com (https://createmod.com/settings/api-keys).
   Elle est enregistrée dans `<instance>/config/createbrowser-client.toml` et prise en compte immédiatement.

```toml
[general]
apiKey = "votre-clé-api"
modSecret = ""    # optionnel — secret HMAC émis par createmod.com, utilisé seulement sans apiKey

[network]
timeoutSeconds = 10
maxRequestsPerSecond = 2
maxRetries = 3

[ui]
pageSize = 24     # 8, 16, 24, 32, 64 ou 100
```

6. La touche est rebindable dans Options → Controls → CreateBrowser.

## État et suite du projet

- Avancement détaillé : `STATE.md`
- Chantiers ouverts (prêts à devenir des issues GitHub) : `docs/issues/`
- Conventions et référence API : `CLAUDE.md`

| Phase | Statut | Livrable |
|---|---|---|
| 0 — Setup | ✅ | Mod compilable, keybind, écran vide |
| 1 — Navigateur de base | ✅ | Recherche → détail → download `.nbt` |
| 2 — Enrichissement | ✅ | Filtres, thumbnails, favoris, historique, fichiers locaux |
| 3 — Upload + intégration Create | ✅ | Upload anonyme, bouton Schematic Table (Mixin), badges mods |
| 4 — Multi-sources | 🟡 | Cache + hors-ligne faits ; 2e source réseau et NeoForge restants |
| 5 — Preview 3D, Quick-Share, i18n | 🟡 | Quick-share + FR/EN faits ; preview 3D restante |
| 6 — Polish | ⏳ | Validation in-game, publication Modrinth/CurseForge |

## Projets similaires

- [Create: Schematic Helper](https://modrinth.com/project/vDsPXWBh) (uberswe, ARR) : upload automatique vers createmod.com et téléchargement par URL / short code dans la Schematic Table. Pas de recherche ni de navigation.
- [Create: Schematics+](https://modrinth.com/mod/schematic) (Fabric 1.20.1, abandonné) : dossiers locaux dans la Schematic Table.

## Liens

- Source createmod.com (référence API) : https://github.com/uberswe/createmod.com
- Clés API createmod.com : https://createmod.com/settings/api-keys
- Wiki Create — Developers : https://wiki.createmod.net/developers/
- MultiLoader Template : https://github.com/jaredlll08/MultiLoader-Template
