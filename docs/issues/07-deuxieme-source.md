# Deuxième source réseau de schematics

## Contexte

`SchematicSource` + `SourceRegistry` sont prêts pour plusieurs sources ; seule createmod.com est branchée. CLAUDE.md cite schematicannon.com, dont l'API n'est pas documentée. Aucun spec trouvé.

## Travail

- [ ] Recenser les sources candidates avec API publique et licence compatible (vérifier CGU : pas de scraping, pas de mirror).
- [ ] Pour chaque candidate : auth, format de fichier (`.nbt` Structure NBT uniquement ; `.litematic`/`.schem` exigeraient une conversion), rate limits.
- [ ] Implémenter `XxxSource implements SchematicSource`, l'enregistrer dans `SourceRegistry`.
- [ ] UI : sélecteur de source dans `BrowserScreen` (aujourd'hui `SourceRegistry.primary()`).
- [ ] Config `sources.active` (décrite dans CLAUDE.md, absente du code).

Priorité basse tant que l'issue 01 n'est pas validée.
