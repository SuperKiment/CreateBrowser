# Preview 3D in-game (Phase 5)

## Objectif

Afficher le schematic en 3D dans `DetailScreen` avant téléchargement. Flag config prévu : `ui.enable3dPreview` (non implémenté dans `ForgeBrowserConfig`).

## Contraintes

- Il faut le `.nbt` : donc un téléchargement (compte comme download côté createmod.com). Télécharger en mémoire, mettre en cache, réutiliser si l'utilisateur clique « Télécharger ».
- Format Structure NBT : palette + blocks + états. `NbtMetadataReader` lit déjà l'en-tête.
- Rendu : `BlockRenderDispatcher.renderSingleBlock` dans un `PoseStack` isolé, rotation souris, zoom. Pas de monde : pas de lumière/biome, block entities à ignorer ou rendus via `BlockEntityRenderer` avec prudence.
- Performance : gros schematics (>50k blocs) → limiter, ou construire un `VertexBuffer` une fois (1.20.1 : `BufferBuilder` + `VertexBuffer.upload`).
- Alternative légère : l'API expose un « layer viewer » côté site ; vérifier si des images de rotation (`rotationImages`) suffisent comme preview 2.5D sans téléchargement.

## Travail

- [ ] Évaluer `rotationImages` (champ API) comme alternative.
- [ ] Parser `.nbt` → liste (pos, BlockState) via `NbtUtils.readBlockState` + `HolderGetter<Block>`.
- [ ] Widget de rendu avec cache GPU.
- [ ] Ajouter `enable3dPreview` à `BrowserConfig`/`ForgeBrowserConfig`.
- [ ] Tests : parser sur un vrai `.nbt` (fixture).
