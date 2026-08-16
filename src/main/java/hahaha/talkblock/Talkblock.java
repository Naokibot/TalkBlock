package hahaha.talkblock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class Talkblock extends JavaPlugin implements Listener {
    private static final String PREFIX = ChatColor.LIGHT_PURPLE + "[Sakura SYSTEM] " + ChatColor.RESET;

    private final Map<UUID, TalkblockEntry> talkblocked = new ConcurrentHashMap<>();
    private final PlayerResolver playerResolver = new PlayerResolver();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadTalkblocks();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Talkblock plugin enabled. Loaded " + talkblocked.size() + " restriction(s).");
    }

    @Override
    public void onDisable() {
        saveTalkblocks();
        getLogger().info("Talkblock plugin disabled.");
    }

    private void loadTalkblocks() {
        talkblocked.clear();
        FileConfiguration config = getConfig();
        ConfigurationSection root = config.getConfigurationSection("talkblocks");
        if (root == null) {
            return;
        }

        for (String key : root.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                getLogger().warning("Ignoring invalid TalkBlock UUID in config: " + key);
                continue;
            }

            Object raw = root.get(key);
            if (raw instanceof String legacyReason) {
                String name = playerResolver.nameFor(uuid, "");
                talkblocked.put(uuid, new TalkblockEntry(legacyReason, name));
                continue;
            }

            ConfigurationSection entrySection = root.getConfigurationSection(key);
            if (entrySection == null) {
                getLogger().warning("Ignoring malformed TalkBlock entry for " + key);
                continue;
            }

            String reason = entrySection.getString("reason", "");
            String name = entrySection.getString("name", "");
            talkblocked.put(uuid, new TalkblockEntry(reason, name));
        }
    }

    private void saveTalkblocks() {
        FileConfiguration config = getConfig();
        config.set("talkblocks", null);
        talkblocked.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String base = "talkblocks." + entry.getKey();
                    TalkblockEntry value = entry.getValue();
                    config.set(base + ".reason", value.reason());
                    config.set(base + ".name", playerResolver.nameFor(entry.getKey(), value.lastKnownName()));
                });
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("talkblock.admin")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "このコマンドを使用する権限がありません。");
            return true;
        }

        String commandName = command.getName();
        if (commandName.equalsIgnoreCase("tablist")) {
            sendTalkblockList(sender);
            return true;
        }

        if (!commandName.equalsIgnoreCase("tab") && !commandName.equalsIgnoreCase("utab")) {
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(PREFIX + ChatColor.RED + "使用法: /" + label + " <プレイヤー名|UUID> <理由>");
            return true;
        }

        PlayerTarget target = playerResolver.resolveKnownPlayer(args[0]).orElse(null);
        if (target == null) {
            sender.sendMessage(PREFIX + ChatColor.RED
                    + "プレイヤーが見つかりません。オンライン中、またはこのサーバーへの参加履歴がある名前/UUIDを指定してください。");
            return true;
        }

        String reason = joinReason(args);
        String executorName = sender.getName();

        if (commandName.equalsIgnoreCase("tab")) {
            talkblocked.put(target.uuid(), new TalkblockEntry(reason, target.displayName()));
            saveTalkblocks();
            Bukkit.broadcastMessage(PREFIX + ChatColor.YELLOW + target.displayName() + ChatColor.RESET
                    + ChatColor.YELLOW + " を " + executorName + ChatColor.RESET + ChatColor.YELLOW
                    + " がTalkblockしました。" + ChatColor.RESET + "\n理由: " + ChatColor.WHITE + reason);
            return true;
        }

        TalkblockEntry removed = talkblocked.remove(target.uuid());
        if (removed == null) {
            sender.sendMessage(PREFIX + ChatColor.RED + target.displayName() + " はTalkblockされていません。");
            return true;
        }

        saveTalkblocks();
        Bukkit.broadcastMessage(PREFIX + ChatColor.YELLOW + target.displayName() + ChatColor.RESET
                + ChatColor.YELLOW + " のTalkblockを " + executorName + ChatColor.RESET + ChatColor.YELLOW
                + " が解除しました。" + ChatColor.RESET + "\n理由: " + ChatColor.WHITE + reason);
        return true;
    }

    private void sendTalkblockList(CommandSender sender) {
        if (talkblocked.isEmpty()) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "Talkblock中のプレイヤーはいません。");
            return;
        }

        sender.sendMessage(PREFIX + ChatColor.AQUA + "Talkblock中のプレイヤー:");
        List<Map.Entry<UUID, TalkblockEntry>> entries = new ArrayList<>(talkblocked.entrySet());
        entries.sort(Comparator.comparing(entry ->
                playerResolver.nameFor(entry.getKey(), entry.getValue().lastKnownName()), String.CASE_INSENSITIVE_ORDER));
        for (Map.Entry<UUID, TalkblockEntry> entry : entries) {
            String name = playerResolver.nameFor(entry.getKey(), entry.getValue().lastKnownName());
            sender.sendMessage(ChatColor.YELLOW + "- " + name + ChatColor.GRAY + " : "
                    + ChatColor.WHITE + entry.getValue().reason());
        }
    }

    private String joinReason(String[] args) {
        StringBuilder reason = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) {
                reason.append(' ');
            }
            reason.append(args[i]);
        }
        return reason.toString().trim();
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        TalkblockEntry entry = talkblocked.get(player.getUniqueId());
        if (entry == null) {
            return;
        }

        player.sendMessage(PREFIX + ChatColor.RED + "あなたはTalkblockされています。"
                + ChatColor.RESET + " 理由: " + ChatColor.WHITE + entry.reason());
        event.setCancelled(true);
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        TalkblockEntry entry = talkblocked.get(player.getUniqueId());
        if (entry == null) {
            return;
        }

        String message = event.getMessage().toLowerCase(java.util.Locale.ROOT);
        if (message.equals("/me") || message.startsWith("/me ")) {
            player.sendMessage(PREFIX + ChatColor.RED + "あなたはTalkblockされています。"
                    + ChatColor.RESET + " 理由: " + ChatColor.WHITE + entry.reason());
            event.setCancelled(true);
        }
    }
}
