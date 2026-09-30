# Partenariat createmod.com : secret HMAC dédié et besoins API

## Contexte

Authentification acceptée par l'API (`requireAPIKeyOrHMAC`, `internal/pages/api_public.go`) :

1. **Clé API** (`X-API-Key`) : self-service sur https://createmod.com/settings/api-keys. Suffit pour recherche, détail, **téléchargement** (`GET /api/schematics/{name}/download`). Implémenté.
2. **HMAC** (`X-Mod-Message` + `X-Mod-Signature`, message `timestamp:modversion:mcusername:identifier`, fenêtre 5 min) : utilisé par Create: Schematic Helper. Les secrets sont « env + admin-managed » (`resolveModSecrets`) : uberswe peut émettre un secret par mod depuis l'admin.

Aujourd'hui chaque joueur doit créer un compte et une clé. Un secret HMAC dédié à CreateBrowser supprime cette friction (zéro config).

**Ne pas réutiliser** le secret embarqué dans le source de Schematic Helper (`SchematicDownloadHandler.SHARED_SECRET`) : c'est le credential d'un autre mod (licence All Rights Reserved), s'en servir reviendrait à usurper ce mod.

## Demandes à uberswe

- [ ] Secret HMAC admin-managed dédié à CreateBrowser (révocable indépendamment).
- [ ] Accord explicite pour un client de navigation in-game (la politique interdit le bulk download ; le mod limite à 2 req/s et ne fait aucun mirror).
- [ ] Vignettes dans un format lisible par Minecraft (PNG) : `?thumb=WxH` renvoie du WebP (voir issue 05).
- [ ] Endpoint de résolution des short codes (parité avec Schematic Helper).
- [ ] Mise à jour de l'OpenAPI (`dimensions` vs `dimX/Y/Z`, `type` de `/api/mod/download`).

## Implémentation une fois le secret obtenu

- Embarquer le secret au build (propriété Gradle / secret CI, jamais commité), exposé via `Constants` ou un service.
- `ApiClient` : si pas de clé API, signer les GET (`/api/schematics`, `/api/schematics/{name}`, `/download`) avec HMAC au lieu de lever `no_api_key`. Rate limit HMAC : 100 req/min par IP.
- Le champ `modSecret` de la config reste comme override.

## Contacts

- Discord uberswe (réponse la plus rapide selon le site) : https://discord.gg/NQJuhb6stv
- Formulaire : https://createmod.com/contact
- E-mail (contact de l'OpenAPI) : hello@createmod.com
- GitHub : https://github.com/uberswe (issues réservées aux bugs/features, cf. `CONTRIBUTING.md`)
