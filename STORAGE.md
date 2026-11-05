# CPN storage backends (Minecraft 1.21.1 / Cobblemon 1.8.4)

The server stores shared Pokédex entries using the selected backend. The code is Java 21 and works through the common Architectury module, with the same configuration for Fabric and NeoForge.

## Configuration

On the first server startup CPN creates `config/cobblemon-pokedex-network-storage.json`:

```json
{
  "storageFormat": "json",
  "mongoDBConnectionString": "mongodb://localhost:27017",
  "mongoDBDatabaseName": "cobblemon",
  "mongoDBCollectionName": "PokedexNetwork",
  "migrationSourceFormat": ""
}
```

* `storageFormat`: `json` (default, backwards compatible), `nbt`, or `mongodb`.
* `mongoDBConnectionString`: MongoDB server URI; credentials may be included. Keep the config private.
* `mongoDBDatabaseName`: MongoDB database. The default matches Cobblemon, but CPN uses a separate collection.
* `mongoDBCollectionName`: MongoDB collection for CPN only. Choose a different name if independent worlds should not share data.
* `migrationSourceFormat`: set to the *previous* backend (`json`, `nbt` or `mongodb`) during a one-time migration. Empty string disables migration.

Restart the Minecraft server after changing this file. Configuration and storage are server-side; clients do not need any database credentials. The MongoDB Java sync driver is packaged inside the mod and shaded to avoid version clashes with other mods.

## Files

* JSON: `<world>/pokenet/*.json`. This is the **existing CPN format** and is the default; no migration is required for existing servers.
* NBT: `<world>/pokenet_nbt/*.nbt`. Compressed NBT with an explicit species identifier, forms, capture date, genders, shiny states and biome counts.
* MongoDB: one document per species (`_id` = resource identifier, `forms` = serialized fields) in the configured collection.

JSON/NBT writes use a temporary file followed by atomic replacement when supported. The previous version is retained as `*.json.old` or `*.nbt.old`. Existing `.old` backups are not automatically loaded. All updates run on the existing single-thread save queue, which drains on a normal server stop. MongoDB is synchronously pinged during server startup, so an unreachable database is reported as a startup failure instead of silently using an empty Pokédex.

## Switching storage formats safely

1. Stop the server and back up `<world>/pokenet`, `<world>/pokenet_nbt` (if present), the configuration and, if applicable, MongoDB.
2. Set `storageFormat` to the destination format and `migrationSourceFormat` to the old format. Keep MongoDB connection settings pointing to the correct MongoDB instance if the source or destination is MongoDB.
3. Start the server. CPN imports species into the destination and writes a marker in `<world>/pokenet_migrations/` **only after** all records have been copied. Old data is left untouched. If the destination already has conflicting data, startup is refused rather than overwriting it.
4. Confirm the log entry `Completed CPN migration`, restart, and set `migrationSourceFormat` back to `""` (optional: keeping it set is safe because of the completion marker).

Examples: `json` -> `nbt` uses `storageFormat: "nbt", migrationSourceFormat: "json"`; `json` -> `mongodb` uses `"mongodb"` and `"json"`. Changing MongoDB URI/database/collection within the same format is **not** an automatic migration: back up and move the collection manually.

CPN records the active format in `<world>/pokenet_storage_state.txt`. If this changes, server startup requires `migrationSourceFormat` to match the previous backend, preventing accidental rollback to stale data. The migration marker is per world and per format pair. Repeated migrations between the same two formats require reviewing and removing the previous marker after making a backup. Do not remove it without first verifying the source and destination contents.

## Limitations / testing

This is the first storage abstraction for CPN. It retains the existing save-on-every-capture behavior (asynchronous, no tick loop or timer). It does not yet implement Cobblemon's configurable save interval or an admin storage command. Sharing the same MongoDB collection simultaneously between multiple Minecraft servers is not supported: each process keeps its own in-memory snapshot and can overwrite another server's edits. Use separate collections until multi-server concurrency/versioning is implemented.

Recommended tests before deploying: create and restart a JSON server; migrate JSON -> NBT and restart; migrate JSON -> MongoDB and restart; capture during active player sessions; stop immediately after capture; simulate unreachable MongoDB and damaged JSON/NBT records. Both Fabric and NeoForge production jars must pass Gradle `build`.
