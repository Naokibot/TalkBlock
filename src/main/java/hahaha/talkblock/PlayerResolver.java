package hahaha.talkblock;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

final class PlayerResolver {
    Optional<PlayerTarget> resolveKnownPlayer(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        String query = input.trim();
        Player online = Bukkit.getPlayerExact(query);
        if (online != null) {
            return Optional.of(new PlayerTarget(online.getUniqueId(), online.getName(), true));
        }

        Optional<UUID> uuid = parseUuid(query);
        if (uuid.isPresent()) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid.get());
            String name = offline.getName();
            if (offline.hasPlayedBefore() || offline.isOnline() || name != null) {
                return Optional.of(new PlayerTarget(uuid.get(), safeName(name, uuid.get()), offline.isOnline()));
            }
            return Optional.empty();
        }

        String needle = query.toLowerCase(Locale.ROOT);
        return Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(player -> player.getName() != null)
                .filter(player -> player.getName().toLowerCase(Locale.ROOT).equals(needle))
                .findFirst()
                .map(player -> new PlayerTarget(
                        player.getUniqueId(),
                        safeName(player.getName(), player.getUniqueId()),
                        player.isOnline()));
    }

    String nameFor(UUID uuid, String fallback) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        String offlineName = Bukkit.getOfflinePlayer(uuid).getName();
        if (offlineName != null && !offlineName.isBlank()) {
            return offlineName;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return uuid.toString();
    }

    private Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static String safeName(String name, UUID uuid) {
        return name == null || name.isBlank() ? uuid.toString() : name;
    }
}
