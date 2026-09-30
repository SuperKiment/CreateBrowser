# Port NeoForge 1.21.1 (Create 6.0.10+)

## Pourquoi

1.21.1 NeoForge est la cible principale de Create 6. Create: Schematic Helper couvre 1.21.1 (NeoForge + Fabric), 1.20.1, 1.19.2, 1.18.2.

## Décision déjà prise (STATE.md)

Branche dédiée `mc1.21`, pas un sous-projet : `common/` est compilé contre 1.20.1 et les API vanilla divergent.

## Travail

- [ ] `gradle.properties` : `minecraft_version=1.21.1`, `java_version=21`, NeoForge `21.1.x`, Parchment 1.21.1.
- [ ] Remplacer `net.neoforged.moddev.legacyforge` par `net.neoforged.moddev` ; sous-projet `neoforge/` (renommer `forge/`).
- [ ] `META-INF/mods.toml` → `META-INF/neoforge.mods.toml` ; dépendance Create `type="optional"`, `versionRange="[6.0,)"`, `side="CLIENT"`.
- [ ] Imports `net.minecraftforge.*` → `net.neoforged.*` (`ModConfigSpec` au lieu de `ForgeConfigSpec`, `container.registerConfig`, `RegisterKeyMappingsEvent` sur le mod bus, `InputEvent.Key` → `ClientTickEvent` + `consumeClick`).
- [ ] Vanilla 1.21 : `new ResourceLocation(ns, path)` → `ResourceLocation.fromNamespaceAndPath` (`ThumbnailWidget`) ; `renderBackground(graphics)` → `renderBackground(graphics, mouseX, mouseY, partialTick)` ; `ObjectSelectionList` (constructeur et `getRowWidth`) ; `Button`/`EditBox` inchangés ; pack_format 34.
- [ ] Mixin : NeoForge tourne en noms Mojang, `method = "init"` suffit (retirer `m_7856_`). `compatibilityLevel` `JAVA_21`.
- [ ] Dépendances compileOnly (CLAUDE.md) : `com.simibubi.create:create-1.21.1:6.0.10-280:slim`, Ponder, Flywheel, Registrate. Vérifier le chemin `content.schematics.table.SchematicTableScreen` dans Create 6.0.10.
- [ ] `Constants.MC_VERSION` (le test `ConstantsTest` échouera tant que non aligné).
- [ ] CI : Java 21, artefact `neoforge/build/libs/*.jar`.
- [ ] Rejouer la checklist de l'issue 01.

## Référence

Schematic Helper (`uberswe/CreateSchematicUpload`) : structure MultiLoader 1.21.1 NeoForge + Fabric, Mixin `SchematicTableScreen` sur NeoForge.
