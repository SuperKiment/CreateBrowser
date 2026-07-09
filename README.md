# CreateBrowser

Mod Minecraft client-side qui ajoute un navigateur de schematics in-game pour le mod [Create](https://www.curseforge.com/minecraft/mc-mods/create). Recherche, prévisualisation et téléchargement de fichiers `.nbt` depuis [createmod.com](https://createmod.com) directement dans Minecraft, sans quitter le jeu.

- **Mod ID** : `createbrowser`
- **Minecraft** : 1.20.1
- **Loader** : Forge `47.2.30+` (NeoForge prévu en Phase 4+)
- **Java** : 17
- **Licence** : MIT

## Fonctionnalités (Phase 1)

- Touche `N` ouvre le navigateur in-game
- Recherche createmod.com avec pagination, debounce et états (loading / empty / error)
- Page de détail (dimensions, block count, mods requis avec badges installé/manquant)
- Téléchargement `.nbt` vers `<instance>/schematics/`, immédiatement utilisable dans la Schematic Table de Create
- Notification chat de confirmation
- Configuration TOML Forge, i18n FR + EN

> Le téléchargement nécessite un `mod_download_secret` partagé fourni par le mainteneur de createmod.com (uberswe). Sans ce secret, recherche et détail restent fonctionnels avec une simple clé API.

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
| `api/source/` | Interface `SchematicSource` + implémentation `CreateModComSource` |
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

**Compile-only** : Forge `47.2.30` (1.20.1).

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

Artefact produit : `forge/build/libs/createbrowser-forge-1.20.1-0.2.0.jar`.

CI GitHub Actions configurée dans `.github/workflows/build.yml` (déclenchée sur push `main`/`dev` et pull requests).

## Installation

1. Télécharger `createbrowser-forge-1.20.1-0.2.0.jar` depuis les releases (ou builder localement, voir ci-dessus).
2. Le placer dans `<instance Minecraft>/mods/`.
3. Avoir Forge `47.2.30+` pour MC 1.20.1.
4. Le mod [Create](https://www.curseforge.com/minecraft/mc-mods/create) est **optionnel** — sans Create, le navigateur reste utilisable et écrit dans `schematics/` (utile pour Litematica ou d'autres mods compatibles avec ce format).
5. Premier lancement → ouvrir une fois le menu pour créer `<instance>/config/createbrowser-client.toml`.
6. Renseigner la clé API obtenue sur createmod.com :

```toml
[general]
apiKey = "votre-clé-api"
modSecret = ""    # optionnel — fourni par uberswe pour activer les downloads

[network]
timeoutSeconds = 10
maxRequestsPerSecond = 2
maxRetries = 3

[ui]
pageSize = 24
```

7. En jeu, taper `N` pour ouvrir le navigateur. La touche est rebindable dans Options → Controls → CreateBrowser.

## Phases de développement

Voir `CLAUDE.md` pour la roadmap détaillée.

| Phase | Statut | Livrable |
|---|---|---|
| 0 — Setup | ✅ | Mod compilable, keybind, écran vide |
| 1 — Navigateur de base | ✅ | Recherche → détail → download `.nbt` |
| 2 — Enrichissement | ⏳ | Filtres, thumbnails, favoris, historique |
| 3 — Upload + intégration Create | ⏳ | Bouton dans Schematic Table (Mixin), upload |
| 4 — Multi-sources | ⏳ | Cache disque, mode hors-ligne, NeoForge |
| 5 — Preview 3D, Quick-Share | ⏳ | |
| 6 — Polish | ⏳ | Publication Modrinth/CurseForge |

## Liens

- Source createmod.com : https://github.com/uberswe/createmod.com
- Wiki Create — Developers : https://wiki.createmod.net/developers/
- MultiLoader Template : https://github.com/jaredlll08/MultiLoader-Template
