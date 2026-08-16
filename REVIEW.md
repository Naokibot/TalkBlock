# TalkBlock 1.1.0 review

## Reviewed input

Original JAR: `talkblock-1.0-SNAPSHOT.jar`

The original JAR contained one class (`hahaha.talkblock.Talkblock`) and no Java source. The implementation was reconstructed from Java 21 bytecode and `plugin.yml`.

## Findings fixed

### HIGH — administration only worked for online players

Original `/tab` and `/utab` used `Bukkit.getPlayerExact(name)` and rejected null, so offline users could never be restricted or un-restricted by name.

Fixed by resolving online players first, then known `OfflinePlayer` entries, with UUID input as a fallback. Unknown names with no server history are rejected rather than fabricating an identity.

### HIGH — unsafe map access from AsyncPlayerChatEvent

The original used a plain `HashMap`. Commands modified it on the primary server thread while `AsyncPlayerChatEvent` read it asynchronously. That is a Java data race.

Fixed with `ConcurrentHashMap`.

### HIGH — clean first install could fail because config.yml was absent

The original called `saveDefaultConfig()` but the supplied JAR did not contain `config.yml`. A clean data folder can therefore fail when the default config resource is requested.

Fixed by embedding `config.yml`.

### MEDIUM — offline list names were not durable

The original stored only UUID -> reason. `/tablist` tried to recover the name from Bukkit every time, which may be null for some cached/offline records.

Fixed by storing the last known name alongside the reason while remaining backward-compatible with 1.0 scalar entries.

### MEDIUM — hard-coded OP check instead of Bukkit permissions

The original allowed only OP or console and declared no permissions. Fixed with `talkblock.admin`, default OP. This keeps the default behavior while allowing LuckPerms or another permission manager to delegate administration safely.

## Deliberately unchanged behavior

- Restriction blocks normal chat and `/me`.
- It does not block `/msg`, `/tell`, `/w`, Essentials messaging commands, Discord bridges, signs, books, or other plugin-specific communication channels.
- Applying/removing a restriction broadcasts the action and reason server-wide, matching the original behavior.

## Remaining runtime verification

Build against the real Spigot 1.21.1 API is required in CI. A real server integration test should additionally verify:

1. Restrict an online player.
2. Restrict a known offline player.
3. Restart the server and verify both remain listed.
4. Let the offline target join and verify chat and `/me` are cancelled.
5. Unrestrict the player while offline and verify chat works on their next login.
