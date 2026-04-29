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
- Branches : `main` (stable), `dev` (développement), `feature/<nom>`, `fix/<nom>`
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
mod_version=0.1.0
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

Base URL : `https://createmod.com/api`
Auth : header `X-API-Key: <clé>`
Format réponse : JSON

### Endpoints

#### Recherche
```
GET /api/schematics?query=<terme>&page=<n>&pageSize=24
```
Réponse :
```json
{
  "items": [
    {
      "name": "windmill-farm",           // identifiant URL-safe
      "title": "Windmill Farm",
      "author": "builder123",
      "views": 1500,
      "downloads": 320,
      "rating": 4.5,
      "images": ["https://..."],
      "categories": ["automation"],
      "tags": ["windmill", "farm"]
    }
  ],
  "page": 1,
  "pageSize": 24,
  "hasPrev": false,
  "hasNext": true,
  "total": 48,
  "term": "windmill"
}
```

#### Détail
```
GET /api/schematics/<name>
```
Réponse :
```json
{
  "name": "windmill-farm",
  "title": "Windmill Farm",
  "author": "builder123",
  "description": "A beautiful windmill farm...",
  "views": 1500,
  "downloads": 320,
  "rating": 4.5,
  "images": ["https://..."],
  "categories": ["automation"],
  "tags": ["windmill", "farm"],
  "materials": [
    {"block_id": "create:shaft", "name": "Shaft", "count": 12}
  ],
  "mods": ["create"],
  "dimensions": {"x": 15, "y": 20, "z": 15},
  "block_count": 450
}
```

#### Upload
```
POST /api/schematics/upload
Content-Type: multipart/form-data
Header: X-API-Key: <clé>
Body: file=<fichier .nbt>
```
Réponse :
```json
{
  "token": "abc123",
  "url": "https://createmod.com/schematics/my-schematic"
}
```

#### Téléchargement du fichier .nbt
Le fichier .nbt est téléchargeable via le lien direct depuis les métadonnées du schematic. L'URL exacte est à extraire de la page de détail ou de l'API (champ à confirmer — probablement `https://createmod.com/schematics/<name>/download` ou un lien S3).

### Contraintes de l'API
- Rate limit : non documenté précisément, mais la politique interdit le bulk downloading
- Le mod doit s'auto-limiter : max 2 requêtes/seconde
- User-Agent obligatoire : `CreateBrowser/<version> Minecraft/<mc_version>`
- Ne pas construire de "plateforme concurrente" — le mod est un client, pas un mirror

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
com.simibubi.create.content.schematics.SchematicTableScreen
com.simibubi.create.content.schematics.SchematicTableMenu
com.simibubi.create.content.schematics.SchematicAndQuillHandler
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

```java
// Dans common/
public interface CreateBridge {
    /** Le mod Create est-il chargé ? */
    boolean isCreateLoaded();

    /** Version de Create installée (ex: "6.0.10"), ou null */
    @Nullable String getCreateVersion();

    /** Liste les mods Create-addons installés */
    List<String> getInstalledCreateAddons();

    /** Ouvre le BrowserScreen depuis un bouton ajouté à la SchematicTableScreen */
    void injectBrowseButton();

    /** Instance no-op pour quand Create n'est pas installé */
    CreateBridge NOOP = new CreateBridge() {
        public boolean isCreateLoaded() { return false; }
        public String getCreateVersion() { return null; }
        public List<String> getInstalledCreateAddons() { return List.of(); }
        public void injectBrowseButton() { /* no-op */ }
    };
}
```

```java
// Dans neoforge/
public class CreateBridgeNeoForge implements CreateBridge {
    @Override
    public boolean isCreateLoaded() {
        return ModList.get().isLoaded("create");
    }

    @Override
    public String getCreateVersion() {
        return ModList.get().getModContainerById("create")
            .map(c -> c.getModInfo().getVersion().toString())
            .orElse(null);
    }

    @Override
    public List<String> getInstalledCreateAddons() {
        // Détecter les mods qui dépendent de Create
        return ModList.get().getMods().stream()
            .filter(info -> info.getDependencies().stream()
                .anyMatch(dep -> dep.getModId().equals("create")))
            .map(info -> info.getModId())
            .toList();
    }

    @Override
    public void injectBrowseButton() {
        // Fait via Mixin sur SchematicTableScreen — voir mixin/
    }
}
```

---

## Mixin SchematicTableScreen

Le seul Mixin du projet. Ajoute un bouton "Browse Online" dans le GUI de la Schematic Table de Create.

```java
// Dans neoforge/mixin/SchematicTableScreenMixin.java
@Mixin(targets = "com.simibubi.create.content.schematics.SchematicTableScreen")
public abstract class SchematicTableScreenMixin extends Screen {
    protected SchematicTableScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void createbrowser$addBrowseButton(CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(
            Component.translatable("createbrowser.button.browse"),
            btn -> Minecraft.getInstance().setScreen(new BrowserScreen())
        ).bounds(this.width / 2 + 60, this.height / 2 - 30, 80, 20).build());
    }
}
```

**Fichier mixin config** (`createbrowser.mixins.json` dans resources/) :
```json
{
  "required": false,
  "package": "net.createbrowser.neoforge.mixin",
  "compatibilityLevel": "JAVA_21",
  "client": ["SchematicTableScreenMixin"],
  "injectors": { "defaultRequire": 0 }
}
```

Le `"required": false` et `"defaultRequire": 0` sont essentiels : si Create n'est pas installé ou si la classe cible a changé, le mixin échoue silencieusement au lieu de crasher.

---

## Configuration

Fichier : `<instance>/config/createbrowser-client.toml`

```toml
[general]
    # Clé API pour createmod.com (obtenue sur le site)
    apiKey = ""
    # Chemin du dossier schematics (auto-détecté si vide)
    schematicsPath = ""

[cache]
    # Taille max du cache en MB
    maxSizeMB = 50
    # TTL des résultats de recherche en minutes
    searchTtlMinutes = 15
    # TTL des pages de détail en minutes
    detailTtlMinutes = 60

[network]
    # Timeout des requêtes HTTP en secondes
    timeoutSeconds = 10
    # Max requêtes par seconde par source
    maxRequestsPerSecond = 2
    # Nombre max de retries
    maxRetries = 3

[sources]
    # Sources actives (ordre = priorité)
    active = ["createmod.com", "local"]

[ui]
    # Nombre de résultats par page
    pageSize = 24
    # Afficher les thumbnails
    showThumbnails = true
    # Activer la preview 3D (Phase 5)
    enable3dPreview = false
```

Enregistrement NeoForge :
```java
// Dans CreateBrowserNeoForge.java
container.registerConfig(ModConfig.Type.CLIENT, BrowserConfig.SPEC);
```

---

## Internationalisation

Fichiers dans `assets/createbrowser/lang/`.

### Clés de traduction

```json
{
  "key.createbrowser.open": "Open Schematic Browser",
  "key.categories.createbrowser": "CreateBrowser",
  "createbrowser.button.browse": "Browse Online",
  "createbrowser.screen.title": "Schematic Browser",
  "createbrowser.screen.search.placeholder": "Search schematics...",
  "createbrowser.screen.search.loading": "Searching...",
  "createbrowser.screen.search.no_results": "No schematics found",
  "createbrowser.screen.search.error": "Search failed: %s",
  "createbrowser.screen.detail.title": "Schematic Details",
  "createbrowser.screen.detail.dimensions": "Size: %dx%dx%d",
  "createbrowser.screen.detail.blocks": "%d blocks",
  "createbrowser.screen.detail.materials": "Materials",
  "createbrowser.screen.detail.mods_required": "Required Mods",
  "createbrowser.screen.detail.mod_installed": "Installed",
  "createbrowser.screen.detail.mod_missing": "Missing",
  "createbrowser.screen.detail.download": "Download",
  "createbrowser.screen.detail.downloading": "Downloading...",
  "createbrowser.screen.detail.downloaded": "Downloaded!",
  "createbrowser.screen.detail.download_error": "Download failed",
  "createbrowser.screen.detail.publish": "Publish",
  "createbrowser.screen.tabs.search": "Search",
  "createbrowser.screen.tabs.favorites": "Favorites",
  "createbrowser.screen.tabs.history": "History",
  "createbrowser.screen.tabs.local": "My Schematics",
  "createbrowser.screen.local.rename": "Rename",
  "createbrowser.screen.local.delete": "Delete",
  "createbrowser.screen.local.delete_confirm": "Delete '%s'? This cannot be undone.",
  "createbrowser.screen.settings": "Settings",
  "createbrowser.screen.offline": "Offline — showing cached results",
  "createbrowser.chat.downloaded": "[CreateBrowser] '%s' downloaded — available in the Schematic Table",
  "createbrowser.chat.upload_success": "[CreateBrowser] '%s' uploaded — %s",
  "createbrowser.chat.upload_error": "[CreateBrowser] Upload failed: %s",
  "createbrowser.compat.all_mods_present": "All required mods installed",
  "createbrowser.compat.missing_mods": "Missing mods: %s",
  "createbrowser.filter.category": "Category",
  "createbrowser.filter.size": "Size",
  "createbrowser.filter.sort": "Sort by",
  "createbrowser.filter.sort.recent": "Most recent",
  "createbrowser.filter.sort.downloads": "Most downloaded",
  "createbrowser.filter.sort.rating": "Highest rated",
  "createbrowser.filter.sort.views": "Most viewed"
}
```

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

```yaml
# .github/workflows/build.yml
name: Build
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'microsoft'
      - name: Build
        run: ./gradlew build
      - name: Test
        run: ./gradlew test
      - name: Upload artifacts
        uses: actions/upload-artifact@v4
        with:
          name: jars
          path: |
            neoforge/build/libs/*.jar
            forge/build/libs/*.jar
```

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
| createmod.com API | https://createmod.com/api |
| createmod.com GitHub | https://github.com/uberswe/createmod.com |
| CreateSchematicUpload (référence) | https://github.com/uberswe/CreateSchematicUpload |
| LitematicDownloader (modèle UX) | https://modrinth.com/mod/litematicdownloader |
| Create source (GitHub) | https://github.com/Creators-of-Create/Create |
