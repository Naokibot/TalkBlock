# TalkBlock

Spigot 1.21.1 / Java 21 chat restriction plugin.

## 1.1.0 changes

- `/tab <player> <reason>` and `/utab <player> <reason>` now accept players who are offline if they have previously joined the server.
- UUID input is supported as a fallback for known offline players.
- Restrictions are stored by UUID, so a player restricted while offline is automatically blocked when they next join.
- Legacy 1.0 config entries (`talkblocks.<uuid>: reason`) are loaded and migrated on the next save.
- The last known player name is stored for reliable `/tablist` output while the player is offline.
- Added an embedded `config.yml`, so a clean first install no longer depends on a pre-existing data folder.
- Replaced the async-read/sync-write `HashMap` with `ConcurrentHashMap` for safe chat-event access.
- Added `talkblock.admin` permission (default: OP); console remains allowed.

## Commands

- `/tab <player name|UUID> <reason>`
- `/utab <player name|UUID> <reason>`
- `/tablist`

TalkBlock continues to cancel normal chat and `/me` for restricted players. It does not attempt to intercept third-party private-message commands.
