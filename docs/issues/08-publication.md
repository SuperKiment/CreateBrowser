# Publication Modrinth / CurseForge

## Prérequis

- Issue 01 validée.
- Accord createmod.com (issue 02) : le mod dépend de leur API ; publier sans leur accord expose à une révocation.

## Travail

- [ ] Comptes Modrinth + CurseForge, projets créés (client-side, dépendance optionnelle Create).
- [ ] Icône `logo.png` + `logoFile` dans `mods.toml`, captures d'écran, description FR/EN.
- [ ] Workflow `.github/workflows/release.yml` sur tag `v*` : build + `Kir-Antipov/mc-publish` (utilisé par Schematic Helper) avec `MODRINTH_TOKEN` / `CURSEFORGE_TOKEN` en secrets.
- [ ] Changelog.
- [ ] Tag `v0.3.0`.
- [ ] Différenciation vs Create: Schematic Helper dans la description : recherche/navigation/filtres/favoris in-game (Schematic Helper ne fait qu'upload auto + download par URL/short code).
