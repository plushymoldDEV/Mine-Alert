package me.plushymold2011.MineAlert;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
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
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.no-console", "This command cannot be run from the console.")));
            return true;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("minealert")) {
            if (args.length < 1) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.usage", "Usage: /minealert <command> [args]")));
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
                case "inspect":
                    if (args.length == 2) {
                        inspectPlayer(player, args[1]);
                    } else {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.inspect-usage", "Usage: /minealert inspect <player>")));
                    }
                    break;
                case "reload":
                    reloadConfig(player);
                    break;
                case "addblock":
                    if (args.length == 2) {
                        addBlock(player, args[1]);
                    } else {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.addblock-usage", "Usage: /minealert addblock <block>")));
                    }
                    break;
                case "removeblock":
                    if (args.length == 2) {
                        removeBlock(player, args[1]);
                    } else {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.removeblock-usage", "Usage: /minealert removeblock <block>")));
                    }
                    break;
                default:
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.unknown-subcommand", "Unknown subcommand. Use /minealert for help.")));
                    break;
            }
        }

        return true;
    }

    private void addBlock(Player player, String blockName) {
        FileConfiguration config = getConfig();
        if (Material.getMaterial(blockName) != null) {
            if (!config.getConfigurationSection("alerts.blocks").contains(blockName)) {
                config.set("alerts.blocks." + blockName, true);
                saveConfig();
                String message = PlaceholderAPI.setPlaceholders(player, getConfig().getString("messages.block-added", "Block %block% has been added."))
                        .replace("%block%", blockName);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            } else {
                String message = PlaceholderAPI.setPlaceholders(player, getConfig().getString("messages.block-already-exists", "Block %block% already exists."))
                        .replace("%block%", blockName);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            }
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.invalid-block", "Invalid block name.")));
        }
    }

    private void removeBlock(Player player, String blockName) {
        FileConfiguration config = getConfig();
        if (config.getConfigurationSection("alerts.blocks").contains(blockName)) {
            config.set("alerts.blocks." + blockName, null);
            saveConfig();
            String message = PlaceholderAPI.setPlaceholders(player, getConfig().getString("messages.block-removed", "Block %block% has been removed."))
                    .replace("%block%", blockName);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        } else {
            String message = PlaceholderAPI.setPlaceholders(player, getConfig().getString("messages.block-not-exists", "Block %block% does not exist."))
                    .replace("%block%", blockName);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    private void toggleNotifications(Player player) {
        FileConfiguration config = getConfig();
        boolean enabled = config.getBoolean("alerts.enabled");
        config.set("alerts.enabled", !enabled);
        saveConfig();
        String status = !enabled ? "enabled" : "disabled";
        String message = PlaceholderAPI.setPlaceholders(player, getConfig().getString("messages.notifications-toggled", "Notifications have been %status%."))
                .replace("%status%", status);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    private void showInterval(Player player) {
        long currentTime = System.currentTimeMillis();
        long interval = 86400000L; // 24 hours in milliseconds
        long timeLeft = (lastAlertTime + interval) - currentTime;

        String message;
        if (timeLeft > 0) {
            String timeLeftStr = formatElapsedTime(timeLeft);
            message = PlaceholderAPI.setPlaceholders(player, getConfig().getString("messages.time-left", "Time left: %time_left%"))
                    .replace("%time_left%", timeLeftStr);
        } else {
            message = getConfig().getString("messages.data-reset", "Data has been reset.");
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    private void inspectPlayer(Player player, String targetName) {
        Player target = getServer().getPlayer(targetName);

        if (target != null) {
            String message = PlaceholderAPI.setPlaceholders(player, getConfig().getString("messages.inspect-player", "Inspecting player %player%."))
                    .replace("%player%", targetName);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            // Add more detailed player mine data here
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.player-not-found", "Player %player% not found.")).replace("%player%", targetName));
        }
    }

    private void reloadConfig(Player player) {
        this.reloadConfig();
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.config-reloaded", "Configuration has been reloaded.")));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();

        FileConfiguration config = getConfig();
        if (config.getBoolean("alerts.enabled") && config.getConfigurationSection("alerts.blocks").getBoolean(blockType.name(), false)) {
            if (!alertedBlocks.contains(block)) {
                Set<Block> countedBlocks = new HashSet<>();
                int count = countVeinBlocks(block, blockType, countedBlocks);
                
                // Get the current time
                long currentTime = System.currentTimeMillis();
                
                // Calculate elapsed time
                long elapsedMillis = currentTime - lastAlertTime;
                lastAlertTime = currentTime; // Update last alert time
                
                // Format elapsed time
                String elapsedTime = formatElapsedTime(elapsedMillis);
                
                String message = PlaceholderAPI.setPlaceholders(player, getConfig().getString("alerts.alert-message", "Player %player% mined %vein_amount% %block_type% blocks. Last notification was %last_notification%."))
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
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            Collections.addAll(completions, "notifications", "interval", "inspect", "reload", "addblock", "removeblock");
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
