package me.plushymold2011.MineAlert;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Main extends JavaPlugin implements Listener, TabCompleter {

    private final Set<Block> alertedBlocks = new HashSet<>();
    private long lastAlertTime = 0; // Timestamp of the last alert

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("minealert").setTabCompleter(this);
    }

    @Override
    public void onDisable() {
        alertedBlocks.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getFormattedMessage(null, "messages.command-only-by-players"));
            return true;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("minealert")) {
            if (args.length < 1) {
                player.sendMessage(getFormattedMessage(player, "messages.usage"));
                return true;
            }

            String subcommand = args[0].toLowerCase();

            switch (subcommand) {
                case "notifications":
                    toggleNotifications(player);
                    break;
                case "interval":
                    showInterval(player);
                    break;
                case "reload":
                    reloadConfig(player);
                    break;
                case "addblock":
                    if (args.length == 2) {
                        addBlock(player, args[1]);
                    } else {
                        player.sendMessage(getFormattedMessage(player, "messages.usage").replace("<subcommand>", "addblock <block>"));
                    }
                    break;
                case "removeblock":
                    if (args.length == 2) {
                        removeBlock(player, args[1]);
                    } else {
                        player.sendMessage(getFormattedMessage(player, "messages.usage").replace("<subcommand>", "removeblock <block>"));
                    }
                    break;
                default:
                    player.sendMessage(getFormattedMessage(player, "messages.unknown-subcommand"));
                    break;
            }
        }

        return true;
    }

    private void addBlock(Player player, String blockName) {
        FileConfiguration config = this.getConfig();
        if (Material.getMaterial(blockName) != null) {
            if (!config.getConfigurationSection("alerts.blocks").contains(blockName)) {
                config.set("alerts.blocks." + blockName, true);
                saveConfig();
                String message = getFormattedMessage(player, "messages.block-added").replace("%block%", blockName);
                player.sendMessage(message);
            } else {
                String message = getFormattedMessage(player, "messages.block-already-added").replace("%block%", blockName);
                player.sendMessage(message);
            }
        } else {
            String message = getFormattedMessage(player, "messages.block-invalid");
            player.sendMessage(message);
        }
    }

    private void removeBlock(Player player, String blockName) {
        FileConfiguration config = this.getConfig();
        if (config.getConfigurationSection("alerts.blocks").contains(blockName)) {
            config.set("alerts.blocks." + blockName, null);
            saveConfig();
            String message = getFormattedMessage(player, "messages.block-removed").replace("%block%", blockName);
            player.sendMessage(message);
        } else {
            String message = getFormattedMessage(player, "messages.block-not-in-list").replace("%block%", blockName);
            player.sendMessage(message);
        }
    }

    private void toggleNotifications(Player player) {
        FileConfiguration config = this.getConfig();
        boolean enabled = config.getBoolean("alerts.enabled");
        config.set("alerts.enabled", !enabled);
        saveConfig();
        String status = !enabled ? "enabled" : "disabled";
        String message = getFormattedMessage(player, "messages.notifications-toggle").replace("%status%", status);
        player.sendMessage(message);
    }

    private void showInterval(Player player) {
        long currentTime = System.currentTimeMillis();
        long interval = 86400000L; // 24 hours in milliseconds
        long timeLeft = (lastAlertTime + interval) - currentTime;

        if (timeLeft > 0) {
            String timeLeftStr = formatElapsedTime(timeLeft);
            String message = getFormattedMessage(player, "messages.time-left").replace("%time_left%", timeLeftStr);
            player.sendMessage(message);
        } else {
            String message = getFormattedMessage(player, "messages.data-reset");
            player.sendMessage(message);
        }
    }

    private void reloadConfig(Player player) {
        this.reloadConfig();
        String message = getFormattedMessage(player, "messages.config-reloaded");
        player.sendMessage(message);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();

        FileConfiguration config = this.getConfig();
        if (config.getBoolean("alerts.enabled") && config.getConfigurationSection("alerts.blocks").getBoolean(blockType.name(), false)) {
            if (!alertedBlocks.contains(block)) {
                Set<Block> countedBlocks = new HashSet<>();
                int count = countVeinBlocks(block, blockType, countedBlocks);

                long currentTime = System.currentTimeMillis();
                long elapsedMillis = currentTime - lastAlertTime;
                lastAlertTime = currentTime; // Update last alert time

                String elapsedTime = formatElapsedTime(elapsedMillis);

                String message = getFormattedMessage(player, "alerts.alert-message")
                        .replace("%player%", player.getName())
                        .replace("%vein_amount%", String.valueOf(count))
                        .replace("%block_type%", blockType.name().toLowerCase().replace("_", " "))
                        .replace("%last_notification%", elapsedTime);

                alertPlayers(message);
                alertedBlocks.addAll(countedBlocks);  // Add the counted blocks to the alertedBlocks set
            }
        }
    }

    private int countVeinBlocks(Block block, Material blockType, Set<Block> countedBlocks) {
        if (block.getType() != blockType || countedBlocks.contains(block)) {
            return 0;
        }

        countedBlocks.add(block);
        int count = 1;

        count += countVeinBlocks(block.getRelative(1, 0, 0), blockType, countedBlocks);
        count += countVeinBlocks(block.getRelative(-1, 0, 0), blockType, countedBlocks);
        count += countVeinBlocks(block.getRelative(0, 1, 0), blockType, countedBlocks);
        count += countVeinBlocks(block.getRelative(0, -1, 0), blockType, countedBlocks);
        count += countVeinBlocks(block.getRelative(0, 0, 1), blockType, countedBlocks);
        count += countVeinBlocks(block.getRelative(0, 0, -1), blockType, countedBlocks);

        return count;
    }

    private void alertPlayers(String message) {
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.hasPermission("minealert.notifications.receive")) {
                player.sendMessage(message);
            }
        }
    }

    private String formatElapsedTime(long elapsedMillis) {
        long seconds = elapsedMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return hours + " hour(s) ago";
        } else if (minutes > 0) {
            return minutes + " minute(s) ago";
        } else {
            return seconds + " second(s) ago";
        }
    }

    private String getFormattedMessage(Player player, String path) {
        FileConfiguration config = this.getConfig();
        String message = config.getString(path);

        if (message == null) {
            return ChatColor.translateAlternateColorCodes('&', "Message path '" + path + "' not found in config.");
        }

        // Check if PlaceholderAPI is present and replace placeholders
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            if (player != null) {
                message = PlaceholderAPI.setPlaceholders(player, message); // Replace player-specific placeholders
            } else {
                message = PlaceholderAPI.setPlaceholders(null, message); // Replace global placeholders
            }
        }

        // Convert color codes
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("notifications");
            completions.add("interval");
            completions.add("inspect");
            completions.add("reload");
            completions.add("addblock");
            completions.add("removeblock");
        } else if (args.length == 2) {
            if ("inspect".equalsIgnoreCase(args[0])) {
                for (Player p : getServer().getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            } else if ("addblock".equalsIgnoreCase(args[0]) || "removeblock".equalsIgnoreCase(args[0])) {
                for (Material material : Material.values()) {
                    if (material.isBlock() && material != Material.AIR) {
                        completions.add(material.name());
                    }
                }
            }
        }
        return completions;
    }
}
