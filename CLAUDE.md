# CLAUDE.md — CreateBrowser

## Identité du projet

**CreateBrowser** est un mod Minecraft client-side qui ajoute un navigateur de schematics in-game pour le mod Create. Il permet de chercher, prévisualiser, télécharger et publier des fichiers `.nbt` depuis des dépôts communautaires (createmod.com, schematicannon.com) directement dans Minecraft, sans quitter le jeu.

- Mod ID : `createbrowser`
- Package racine : `net.createbrowser`
- Licence : MIT
- Langues du code : Java (mod), JSON (config, lang, API responses)
- Langues de l'interface utilisateur : fr_fr, en_us
- Dépôt : GitHub (à créer)

---

## Architecture

### Structure MultiLoader

Le projet utilise le [MultiLoader Template](https://github.com/jaredlll08/MultiLoader-Template) de jaredlll08. Toute la logique métier est dans `common/`. Les sous-projets loader (`neoforge/`, `forge/`, `fabric/`) ne contiennent que le code de bootstrap spécifique au loader.

```
createbrowser/
├── common/src/main/java/net/createbrowser/
│   ├── CreateBrowserMod.java          ← Point d'entrée abstrait
│   ├── api/                           ← Client HTTP, modèles de données API
│   │   ├── ApiClient.java             ← Wrapper java.net.http.HttpClient async
│   │   ├── ApiConfig.java             ← URLs, clés, timeouts
│   │   ├── RateLimiter.java           ← Token bucket, max 2 req/s par source
│   │   ├── model/
│   │   │   ├── SchematicEntry.java    ← Résultat de recherche (titre, auteur, dims, rating...)
│   │   │   ├── SchematicDetail.java   ← Détail complet (matériaux, mods requis, images)
│   │   │   └── SearchResult.java      ← Page de résultats paginée
│   │   └── source/
│   │       ├── SchematicSource.java   ← Interface commune pour toutes les sources
│   │       ├── CreateModComSource.java
│   │       ├── SchematicCannonSource.java
│   │       └── LocalSource.java       ← Lecture du dossier schematics/ local
│   ├── gui/                           ← Tous les Screen (vanilla MC, pas de dépendance loader)
│   │   ├── BrowserScreen.java         ← Écran principal : recherche + résultats
│   │   ├── DetailScreen.java          ← Détail d'un schematic
│   │   ├── SettingsScreen.java        ← Configuration in-game
│   │   ├── LocalManagerScreen.java    ← Gestionnaire de fichiers locaux
│   │   └── widget/
│   │       ├── SchematicListWidget.java   ← Liste scrollable de résultats
│   │       ├── SearchBarWidget.java       ← Champ de recherche avec debounce
│   │       ├── FilterDropdown.java
│   │       ├── PaginationWidget.java
│   │       └── ThumbnailWidget.java       ← Affichage d'image async
│   ├── storage/
│   │   ├── SchematicFileManager.java  ← CRUD sur <instance>/schematics/
│   │   ├── CacheManager.java         ← Cache thumbnails + API responses
│   │   ├── FavoritesStore.java        ← JSON local, favoris
│   │   └── HistoryStore.java          ← JSON local, historique téléchargements
│   ├── nbt/
│   │   └── NbtMetadataReader.java     ← Extraction dimensions/blocs depuis .nbt header
│   ├── compat/
│   │   └── CreateBridge.java          ← Interface abstraite pour interactions Create
│   ├── config/
│   │   └── BrowserConfig.java         ← Toutes les valeurs de config
│   └── util/
│       ├── AsyncExecutor.java         ← Thread pool + callback vers game thread
│       └── FileNameResolver.java      ← Gestion conflits de noms de fichiers
├── common/src/main/resources/
│   └── assets/createbrowser/
│       ├── lang/en_us.json
│       ├── lang/fr_fr.json
│       └── textures/gui/              ← Icônes, backgrounds
├── neoforge/src/main/java/net/createbrowser/neoforge/
│   ├── CreateBrowserNeoForge.java     ← @Mod entry point, keybind registration
│   ├── CreateBridgeNeoForge.java      ← Implémentation CreateBridge (Mixin SchematicTableScreen)
│   └── mixin/
│       └── SchematicTableScreenMixin.java
├── forge/src/main/java/net/createbrowser/forge/
│   ├── CreateBrowserForge.java
│   ├── CreateBridgeForge.java
│   └── mixin/
│       └── SchematicTableScreenMixin.java
└── fabric/                            ← Optionnel, même structure
```

> État réel : seul `forge/` (1.20.1) existe. Le port NeoForge 1.21.1 se fera sur une branche `mc1.21`
> (voir `docs/issues/03-port-neoforge-1.21.1.md`). Suivi d'avancement : `STATE.md`.

### Règle d'isolation absolue

**`common/` n'importe JAMAIS :**
- `com.simibubi.create.*`
- `net.neoforged.*`
- `net.minecraftforge.*`
- `net.fabricmc.*`

Toute interaction avec Create passe par l'interface `CreateBridge`. Si Create n'est pas installé, le fallback `CreateBridge.NOOP` est utilisé. Le mod reste fonctionnel sans Create (mode standalone : browse + download dans `schematics/`).

**Pourquoi :** à chaque mise à jour de Create, seules les classes `CreateBridge*` dans les sous-projets loader doivent être adaptées. Le reste du mod est immunisé.

---

## Conventions de code

### Java
- Java 17 pour les builds 1.20.1 (Forge), Java 21 pour 1.21.1+ (NeoForge)
- Pas de Lombok, pas de Kotlin — Java pur
- Pas de dépendances externes (pas de OkHttp, pas de Retrofit, pas de Jackson)
  - HTTP : `java.net.http.HttpClient` (natif Java 17+)
  - JSON : `com.google.gson.Gson` (bundlé dans Minecraft)
  - NBT : `net.minecraft.nbt.NbtIo`, `CompoundTag` (vanilla MC)
- Nommage :
  - Classes : PascalCase (`BrowserScreen`, `ApiClient`)
  - Méthodes/variables : camelCase
  - Constantes : UPPER_SNAKE_CASE
  - Packages : lowercase, un seul mot par niveau
- Chaque classe publique a un Javadoc de 1-2 lignes décrivant sa responsabilité
- Aucun `System.out.println` — utiliser le Logger SLF4J de Minecraft
- Aucun accès réseau ou disque sur le game thread — JAMAIS. Utiliser `AsyncExecutor`

### Formatage
- 4 espaces d'indentation, pas de tabs
- Accolades sur la même ligne (`if (...) {`)
- Ligne max : 120 caractères
- Imports explicites, pas de wildcards (`import java.util.List`, pas `import java.util.*`)

### Git
- Branches : `master` (stable), `dev` (développement), `feature/<nom>`, `fix/<nom>`
- Commits : préfixe `[module]` puis description courte en anglais
  - `[api] Add rate limiter with token bucket algorithm`
  - `[gui] Implement search debounce on BrowserScreen`
  - `[neoforge] Add SchematicTableScreen mixin for browse button`
- Un commit = un changement logique. Pas de commits fourre-tout.
- Tags pour les releases : `v0.1.0`, `v0.2.0`, etc.

---

## Dépendances Gradle

### NeoForge (1.21.1)

```groovy
// settings.gradle — utilise le MultiLoader Template standard
// build.gradle (common)
dependencies {
    compileOnly("com.google.code.gson:gson:2.10.1") // déjà dans MC, juste pour la compilation
}

// build.gradle (neoforge) — Create en dépendance OPTIONNELLE
repositories {
    maven { url = "https://maven.createmod.net" }
    maven { url = "https://maven.ithundxr.dev/snapshots" }
}
dependencies {
    // Create — compileOnly car optionnel
    compileOnly("com.simibubi.create:create-1.21.1:6.0.10-280:slim") { transitive = false }
    compileOnly("net.createmod.ponder:ponder-neoforge:1.0.82+mc1.21.1")
    compileOnly("dev.engine-room.flywheel:flywheel-neoforge-api-1.21.1:1.0.6")
    compileOnly("com.tterrag.registrate:Registrate:MC1.21-1.3.0+67")
}
```

### Forge (1.20.1)

```groovy
repositories {
    maven { url = "https://maven.createmod.net" }
    maven { url = "https://maven.ithundxr.dev/snapshots" }
}
dependencies {
    compileOnly("com.simibubi.create:create-1.20.1:0.5.1.j-280:slim") { transitive = false }
    // + dépendances Flywheel/Registrate correspondantes pour 1.20.1
}
```

### Versions actuelles (à maintenir à jour)

```properties
# gradle.properties
mod_id=createbrowser
mod_name=CreateBrowser
mod_version=0.3.0
mod_group=net.createbrowser
# NeoForge 1.21.1
minecraft_version=1.21.1
create_version=6.0.10-280
ponder_version=1.0.82
flywheel_version=1.0.6
registrate_version=MC1.21-1.3.0+67
```

### neoforge.mods.toml — Create en optionnel

```toml
[[dependencies.createbrowser]]
    modId="create"
    type="optional"
    versionRange="[6.0,)"
    ordering="NONE"
    side="CLIENT"
```

---

## API createmod.com — Référence complète

Source de vérité : le code serveur, public (MIT) sur https://github.com/uberswe/createmod.com
(`internal/router/main.go`, `internal/pages/api_public.go`, `internal/pages/api_schematic_download.go`,
`internal/models/schematic.go`). L'OpenAPI servie sur `/api/openapi.json` (`internal/pages/api_openapi.go`)
est utile mais partiellement en retard sur le code : en cas de doute, lire le handler.

Base URL : `https://createmod.com`

### Authentification

| Mode | Headers | Obtention | Rate limit |
|---|---|---|---|
| Clé API | `X-API-Key: <clé>` (ou `?api_key=`) | Self-service : https://createmod.com/settings/api-keys (compte requis) | 120 req/min par clé (défaut) |
| HMAC mod | `X-Mod-Message` + `X-Mod-Signature` | Secret émis par uberswe (secrets « admin-managed ») | 100 req/min par IP |

HMAC : message `timestamp:modversion:mcusername:identifier`, signature = hex(HMAC-SHA256(message, secret)),
timestamp à ±5 min. Ne JAMAIS réutiliser le secret d'un autre mod (ex. Create: Schematic Helper).

### Endpoints utilisés par le mod

| Endpoint | Auth | Usage |
|---|---|---|
| `GET /api/schematics` | clé ou HMAC | Recherche / liste |
| `GET /api/schematics/{name}` | clé ou HMAC | Détail |
| `GET /api/schematics/{name}/download` | clé ou HMAC | 302 vers `/api/files/schematics/{id}/{fichier}.nbt` — téléchargement principal |
| `POST /api/mod/download` | HMAC seulement | `.nbt` XOR-v1 (header `X-Mod-Encoding`) — repli si seul `modSecret` est configuré |
| `POST /api/schematics/upload-anonymous` | aucune | Upload anonyme (multipart `file`) |

Disponibles, non utilisés : `GET /api/schematics/filters` (options de filtres), `GET /api/home` (rails),
`POST /api/schematics/upload` (upload authentifié), `GET /api/schematics/{name}/comments`, `/stats`.

### Recherche — paramètres

`query` (alias `q`), `page` (commence à **1**), `per_page` ∈ {8, 16, 24, 32, 64, 100} (sinon 24),
`sort` numérique : 1 pertinence, 2 récents, 3 anciens, 4 mieux notés, 5 moins bien notés, 6 plus vus,
7 moins vus, 8 trending. Sans `query` ni `sort` : trending. Filtres : `category` (clé), `tag` (clés, virgules),
`mcv`, `cv` (ou `~6.0`), `rating` (min 0-5), `mod` répétable / `mods`.
Pas de tri serveur par téléchargements, pas de filtre serveur par taille.

Réponse : `{items, page, pageSize, hasPrev, hasNext, total, totalPages, term}`.

### Objet schematic (`models.Schematic`, liste et détail)

```json
{
  "id": "abc123", "name": "windmill-farm", "title": "Windmill Farm",
  "author": {"id": "...", "username": "builder123", "avatar": "", "hasAvatar": false},
  "content": "texte", "excerpt": "court", "aiDescription": "",
  "featuredImage": "cover.png", "gallery": ["cover.png", "side.png"],
  "categories": [{"id": "...", "key": "farms", "name": "Farms"}],
  "tags": [{"id": "...", "key": "windmill", "name": "Windmill"}],
  "views": 1500, "downloads": 320, "rating": "4.5", "ratingCount": 12,
  "blockCount": 450, "dimX": 15, "dimY": 20, "dimZ": 15,
  "materials": "[{\"block_id\":\"create:shaft\",\"count\":12}]",
  "mods": ["create"], "createmodVersion": "6.0.8", "minecraftVersion": "1.20.1",
  "shortCode": "aB3dE"
}
```

Pièges :
- `rating` est une **string** formatée `%.1f`.
- `materials` est une **string JSON** ; les entrées n'ont souvent que `block_id` + `count`.
- Images = noms de fichiers : URL = `https://createmod.com/api/files/schematics/{id}/{fichier encodé}`.
  `?thumb=WxH` renvoie du WebP, non décodable par `NativeImage`.
- Les modèles Java acceptent aussi les anciens noms (`images`, `block_count`, `dimensions{x,y,z}`, `description`).

### Contraintes

- Politique createmod.com : pas de bulk download, pas de plateforme concurrente — le mod est un client, pas un mirror.
- Le mod s'auto-limite à `network.maxRequestsPerSecond` (2 par défaut).
- User-Agent : `CreateBrowser/<version> Minecraft/<mc_version>`.
- createmod.com est derrière Cloudflare : requêtes depuis des IP cloud/CI souvent refusées (403). Tests réseau = en jeu.

---

## Système de schematics Create — Référence technique

### Format de fichier
- Extension : `.nbt`
- Format : Structure Block NBT (format vanilla Minecraft), compressé GZip
- Header GZip : octets `1F 8B`
- Contenu : palette de blocs, positions, block states, entités
- Le Schematic and Quill de Create produit ce format
- Compatible avec le Schematicannon et les Deployers

### Dossier schematics
- Chemin : `<instance Minecraft>/schematics/`
- Détection : `Minecraft.getInstance().gameDirectory.toPath().resolve("schematics")`
- Create le crée automatiquement au premier usage du Schematic and Quill
- **Hot-reload** : Create re-scanne le dossier à chaque ouverture du GUI Schematic Table. Aucun redémarrage requis après ajout d'un fichier.

### Classes Create pertinentes (pour les Mixins)
Les noms de packages peuvent varier entre versions de Create. Vérifier dans le source de la version ciblée.

```
com.simibubi.create.content.schematics.table.SchematicTableScreen   ← cible du Mixin (0.5.1 et 6.x)
com.simibubi.create.content.schematics.table.SchematicTableMenu
com.simibubi.create.content.schematics.client.SchematicAndQuillHandler
com.simibubi.create.content.schematics.SchematicItem
```

### Limites serveur (configurables dans create-server.toml)
- `maxTotalSchematicSize` : taille max d'un .nbt uploadé au serveur (défaut 256 KB)
- `maxSchematicPacketSize` : taille max d'un paquet réseau schematic
- Ces limites n'affectent PAS le téléchargement local (écriture dans `schematics/`), mais affectent l'upload via la Schematic Table vers le serveur MC

---

## GUI — Règles de développement

### Framework
Utiliser exclusivement les widgets vanilla Minecraft :
- `Screen` — écran de base
- `EditBox` — champ de texte (recherche)
- `Button` — boutons
- `ObjectSelectionList` — liste scrollable
- `GuiGraphics` — rendu (texte, textures, rectangles)
- `AbstractWidget` — pour les widgets custom

**NE PAS utiliser** de framework GUI tiers (Cloth Config, YACL, etc.) pour les écrans du navigateur. Les écrans de config peuvent utiliser le système de config NeoForge natif.

### Coordonnées et responsive
- X augmente vers la droite, Y vers le bas
- Toujours calculer les positions relativement à `this.width` et `this.height`
- Tester avec Gui Scale 1, 2, 3 et Auto
- Les tailles de police sont en pixels à l'échelle actuelle

### Threading GUI
- `Screen.render()` est appelé sur le game thread — ne JAMAIS bloquer
- Les données à afficher sont pré-chargées via `AsyncExecutor` et stockées dans des champs du Screen
- Quand les données arrivent : `Minecraft.getInstance().execute(() -> { this.results = data; })`
- Le rendu vérifie `if (this.results == null)` et affiche un placeholder

### Keybind
```java
// Dans le sous-projet loader (ex: CreateBrowserNeoForge.java)
public static final KeyMapping OPEN_BROWSER = new KeyMapping(
    "key.createbrowser.open",  // clé de traduction
    InputConstants.KEY_N,       // touche par défaut
    "key.categories.createbrowser"
);

// Enregistrement NeoForge
@SubscribeEvent
public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
    event.register(OPEN_BROWSER);
}

// Écoute NeoForge
@SubscribeEvent
public static void onKeyInput(InputEvent.Key event) {
    if (OPEN_BROWSER.consumeClick()) {
        Minecraft.getInstance().setScreen(new BrowserScreen());
    }
}
```

---

## Client HTTP — Implémentation

### AsyncExecutor

```java
public class AsyncExecutor {
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3,
        r -> { Thread t = new Thread(r, "CreateBrowser-IO"); t.setDaemon(true); return t; });

    public static <T> void run(Supplier<T> task, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        CompletableFuture.supplyAsync(task, POOL)
            .thenAcceptAsync(result ->
                Minecraft.getInstance().execute(() -> onSuccess.accept(result)),
                POOL)
            .exceptionally(ex -> {
                Minecraft.getInstance().execute(() -> onError.accept(ex));
                return null;
            });
    }
}
```

### Requêtes HTTP

```java
// Pattern standard pour toutes les requêtes API
HttpClient client = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build();

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://createmod.com/api/schematics?query=" +
        URLEncoder.encode(query, StandardCharsets.UTF_8)))
    .header("X-API-Key", config.apiKey())
    .header("User-Agent", "CreateBrowser/" + MOD_VERSION + " Minecraft/" + MC_VERSION)
    .timeout(Duration.ofSeconds(10))
    .GET()
    .build();

// TOUJOURS exécuter via AsyncExecutor, JAMAIS sur le game thread
AsyncExecutor.run(
    () -> {
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        return GSON.fromJson(resp.body(), SearchResult.class);
    },
    result -> this.displayResults(result),
    error -> this.showError("Erreur réseau")
);
```

### Rate Limiter

```java
public class RateLimiter {
    private final long minIntervalMs;
    private long lastRequestTime = 0;

    public RateLimiter(int maxPerSecond) {
        this.minIntervalMs = 1000L / maxPerSecond;
    }

    public synchronized void acquire() throws InterruptedException {
        long now = System.currentTimeMillis();
        long wait = minIntervalMs - (now - lastRequestTime);
        if (wait > 0) Thread.sleep(wait);
        lastRequestTime = System.currentTimeMillis();
    }
}
```

---

## Écriture de fichiers

### Téléchargement d'un .nbt

```java
public void downloadSchematic(String name, byte[] data) {
    Path schematicsDir = Minecraft.getInstance().gameDirectory.toPath().resolve("schematics");
    if (!Files.exists(schematicsDir)) Files.createDirectories(schematicsDir);

    String fileName = sanitizeFileName(name) + ".nbt";
    Path target = schematicsDir.resolve(fileName);

    // Gestion des conflits de nom
    int counter = 1;
    while (Files.exists(target)) {
        target = schematicsDir.resolve(sanitizeFileName(name) + "_" + counter + ".nbt");
        counter++;
    }

    // Vérification header GZip
    if (data.length < 2 || data[0] != (byte) 0x1F || data[1] != (byte) 0x8B) {
        throw new IllegalArgumentException("Fichier invalide : pas un NBT GZip");
    }

    Files.write(target, data);
    // Le fichier apparaît immédiatement dans la Schematic Table
}

private String sanitizeFileName(String name) {
    return name.replaceAll("[^a-zA-Z0-9._-]", "_");
}
```

### Stockage local (favoris, historique, cache)

```
<instance>/createbrowser/
├── favorites.json        ← [{name, title, source, addedAt}]
├── history.json          ← [{name, title, source, downloadedAt, filePath}]
└── cache/
    ├── thumbs/           ← images PNG par hash URL
    └── api/              ← réponses JSON par hash requête, avec TTL
```

Les fichiers JSON sont lus/écrits via Gson. Le cache utilise un TTL configurable (défaut 15 min pour les recherches, 1h pour les détails). LRU eviction quand la taille dépasse la limite configurée.

---

## CreateBridge — Interface d'abstraction

Chargée par SPI (`META-INF/services/net.createbrowser.compat.CreateBridge`) via `Services.CREATE`,
avec repli `CreateBridge.NOOP`. Le bouton de la Schematic Table est injecté par Mixin, pas par le bridge.

```java
// common/ — net.createbrowser.compat.CreateBridge
public interface CreateBridge {
    boolean isCreateLoaded();
    String getCreateVersion();               // null si absent
    List<String> getInstalledCreateAddons(); // mods déclarant une dépendance à "create"
    CreateBridge NOOP = ...;
}
```

Implémentation : `forge/.../CreateBridgeForge.java` (`ModList`). Autres services SPI du même modèle :
`BrowserConfig` (config), `ChatNotifier` (messages chat), `IPlatformHelper` (mods chargés, environnement).

---

## Mixin SchematicTableScreen

Le seul Mixin du projet. Ajoute un bouton « Browse Online » dans le GUI de la Schematic Table de Create.

```java
// forge/src/main/java/net/createbrowser/forge/mixin/SchematicTableScreenMixin.java
@Mixin(targets = "com.simibubi.create.content.schematics.table.SchematicTableScreen", remap = false)
public abstract class SchematicTableScreenMixin extends Screen {
    @Inject(method = {"init", "m_7856_"}, at = @At("TAIL"), remap = false)
    private void createbrowser$addBrowseButton(CallbackInfo ci) { ... }
}
```

**Pourquoi deux noms** : Forge 1.20.1 tourne en noms SRG en production (`Screen.init()` = `m_7856_`) et en
noms Mojang en dev. Aucun refmap n'est généré (Create absent du classpath de compilation), donc la cible
doit lister les deux. Sur NeoForge 1.21.1 (noms Mojang partout), `"init"` suffit.

**Fichier mixin config** (`forge/src/main/resources/createbrowser.mixins.json`) :
```json
{
  "required": false,
  "package": "net.createbrowser.forge.mixin",
  "compatibilityLevel": "JAVA_17",
  "client": ["SchematicTableScreenMixin"],
  "injectors": { "defaultRequire": 0 }
}
```

`"required": false` et `"defaultRequire": 0` : si Create est absent ou si la classe a changé, le mixin échoue
silencieusement au lieu de crasher. Revers : un mauvais nom de méthode ne produit AUCUNE erreur — tester le
jar de production, pas seulement `runClient`.

---

## Configuration

Fichier : `<instance>/config/createbrowser-client.toml` (`forge/.../config/ForgeBrowserConfig.java`,
exposé à `common/` via l'interface `BrowserConfig`). `apiKey`, `modSecret` et `showThumbnails` sont
éditables en jeu (`SettingsScreen`) et pris en compte sans redémarrage.

```toml
[general]
    # Clé API createmod.com (recherche, détail, téléchargement) — https://createmod.com/settings/api-keys
    apiKey = ""
    # Secret HMAC optionnel émis par createmod.com — utilisé seulement si apiKey est vide
    modSecret = ""

[network]
    timeoutSeconds = 10        # 1-60
    maxRequestsPerSecond = 2   # 1-10
    maxRetries = 3             # 0-10, retries sur 5xx

[ui]
    pageSize = 24              # 8, 16, 24, 32, 64 ou 100 (autre valeur → 24 côté serveur)
    showThumbnails = true

[cache]
    maxSizeMB = 50
    searchTtlMinutes = 15
    detailTtlMinutes = 60
```

Non implémentés (voir `docs/issues/`) : `schematicsPath`, `sources.active`, `ui.enable3dPreview`.

---

## Internationalisation

Fichiers dans `assets/createbrowser/lang/`.

### Clés de traduction

Source de vérité : `common/src/main/resources/assets/createbrowser/lang/en_us.json`. Toute clé ajoutée
doit l'être dans `en_us.json` ET `fr_fr.json`. Les erreurs affichées passent par `gui/ErrorText`
(`createbrowser.error.<code>`).

---

## Tests

### Structure
```
common/src/test/java/net/createbrowser/
├── api/
│   ├── ApiClientTest.java          ← Mock HTTP, parse réponses JSON
│   ├── RateLimiterTest.java
│   └── model/
│       └── SearchResultTest.java   ← Désérialisation JSON
├── storage/
│   ├── SchematicFileManagerTest.java ← Conflits de noms, sanitize
│   ├── CacheManagerTest.java       ← TTL, LRU eviction, taille
│   └── FavoritesStoreTest.java
├── nbt/
│   └── NbtMetadataReaderTest.java  ← Parse de vrais .nbt
└── util/
    └── FileNameResolverTest.java
```

### Framework
- JUnit 5 + Mockito
- Pas de tests Minecraft in-game automatisés (trop fragile) — tests manuels du GUI
- CI : `./gradlew test` dans GitHub Actions

### Tests critiques
- Parse d'une réponse JSON createmod.com avec tous les champs, y compris null/vides
- Rate limiter respecte le délai minimum entre requêtes
- Gestion de `fileName_1.nbt`, `fileName_2.nbt` quand le nom existe déjà
- Cache expire après TTL
- Le sanitizer de noms de fichiers supprime les caractères dangereux
- Parse du header GZip pour validation .nbt

---

## CI/CD

### GitHub Actions

`.github/workflows/build.yml` : push sur `master`/`dev` et PR vers `master`, JDK 17 Temurin,
`./gradlew build` (inclut les tests), artefact `forge/build/libs/*.jar`.

`ConstantsTest` vérifie que `Constants.MOD_VERSION` / `MC_VERSION` correspondent à `gradle.properties` :
bumper les deux ensemble.

---

## Processus de développement — Phases

Chaque phase a un critère de validation binaire (ça marche ou pas). Ne pas passer à la phase suivante tant que la phase courante n'est pas validée.

### Phase 0 — Setup (1 semaine)
Livrable : mod compilable, keybind `N` ouvre un Screen vide, chargement sans crash sur MC 1.20.1 Forge et 1.21.1 NeoForge.

### Phase 1 — Navigateur de base (3 semaines)
Livrable : recherche createmod.com, affichage résultats, page de détail, téléchargement .nbt dans `schematics/`, message chat de confirmation.

### Phase 2 — Enrichissement (3 semaines)
Livrable : filtres, thumbnails, favoris, historique, gestionnaire de fichiers locaux.

### Phase 3 — Upload et intégration Create (3 semaines)
Livrable : upload vers createmod.com, bouton dans Schematic Table via Mixin, badges de compatibilité mods.

### Phase 4 — Multi-sources et robustesse (3 semaines)
Livrable : interface `SchematicSource`, 2+ sources, cache avancé, mode hors-ligne, configuration complète.

### Phase 5 — Preview 3D, Quick-Share, i18n (5 semaines)
Livrable : preview 3D in-game, partage par lien, localisation fr/en.

### Phase 6 — Polish et publication (2 semaines)
Livrable : tests, documentation, publication Modrinth/CurseForge.

---

## Erreurs fréquentes à éviter

1. **Bloquer le game thread** avec une requête HTTP synchrone → freeze du jeu. Toujours async.
2. **Importer Create dans common/** → casse le build quand Create n'est pas dans le classpath.
3. **Utiliser `WidthType.PERCENTAGE`** dans un GUI → incohérent entre échelles. Utiliser des pixels relatifs à `this.width`.
4. **Hardcoder la clé API** dans le code → utiliser le fichier config.
5. **Ignorer les conflits de noms de fichiers** → écrasement silencieux des schematics existants.
6. **Oublier `"required": false`** dans la config mixin → crash si Create n'est pas installé.
7. **Ne pas vérifier le header GZip** des .nbt téléchargés → fichiers corrompus silencieux.
8. **Utiliser `net.minecraftforge` dans le sous-projet NeoForge** → les packages ont changé en `net.neoforged`.
9. **Dépendre d'un jar non-release de Create** → le code peut ne pas exister dans la version release que les utilisateurs installent.
10. **Passer le curseur de recherche comme `page=0`** → l'API createmod.com utilise `page=1` comme première page.
11. **Cibler une méthode vanilla dans un Mixin avec `remap = false`** sur Forge 1.20.1 → introuvable en production (noms SRG). Lister aussi le nom SRG.
12. **Se fier à l'OpenAPI ou à un exemple JSON inventé** → vérifier le handler dans `uberswe/createmod.com`. Les paramètres `pageSize` et `sort=recent` étaient silencieusement ignorés.
13. **Réutiliser le secret HMAC d'un autre mod** → usurpation ; demander un secret dédié (voir `docs/issues/02-partenariat-createmod.md`).

---

## Ressources de référence

| Ressource | URL |
|-----------|-----|
| MultiLoader Template | https://github.com/jaredlll08/MultiLoader-Template |
| NeoForge Docs (Screens) | https://docs.neoforged.net/docs/1.21.1/gui/screens/ |
| NeoForge Docs (Config) | https://docs.neoforged.net/docs/misc/config/ |
| NeoForge Docs (Sides) | https://docs.neoforged.net/docs/concepts/sides/ |
| Create Wiki — Developers | https://wiki.createmod.net/developers/ |
| Create — Depend NeoForge 1.21.1 | https://wiki.createmod.net/developers/depend-on-create/neoforge-1.21.1 |
| Create Maven | https://maven.createmod.net |
| createmod.com API (OpenAPI) | https://createmod.com/api/openapi.json |
| createmod.com — clés API | https://createmod.com/settings/api-keys |
| createmod.com — contact / Discord uberswe | https://createmod.com/contact · https://discord.gg/NQJuhb6stv |
| createmod.com GitHub | https://github.com/uberswe/createmod.com |
| CreateSchematicUpload (référence) | https://github.com/uberswe/CreateSchematicUpload |
| LitematicDownloader (modèle UX) | https://modrinth.com/mod/litematicdownloader |
| Create source (GitHub) | https://github.com/Creators-of-Create/Create |
