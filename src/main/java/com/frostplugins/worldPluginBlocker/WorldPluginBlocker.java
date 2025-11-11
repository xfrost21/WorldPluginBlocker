package com.frostplugins.pluginManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PluginManager extends JavaPlugin implements Listener {

    // Map storing the plugin name (lowercase) and the list of worlds where it is blocked.
    private final Map<String, List<String>> blockedPluginWorlds = new HashMap<>();
    private final String prefix = ChatColor.RED + "[Blocker] " + ChatColor.YELLOW;

    @Override
    public void onEnable() {
        // Saves the default config.yml if it doesn't exist.
        saveDefaultConfig();

        // Loads the configuration into memory.
        loadConfiguration();

        // Register the Event Listener
        Bukkit.getPluginManager().registerEvents(this, this);

        // Diagnostic function to help the user configure
        logAvailablePlugins();
    }

    /**
     * Prints a list of all currently loaded plugins to the console.
     * This helps the user get the exact (case-sensitive) names for config.yml.
     */
    private void logAvailablePlugins() {
        getLogger().info("--- Available Plugins for Configuration ---");
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            getLogger().info("- " + plugin.getName() + " (Version: " + plugin.getDescription().getVersion() + ")");
        }
        getLogger().info("-----------------------------------------");
        getLogger().info("Use the EXACT names above in your config.yml under 'managed_plugins'.");
    }

    /**
     * Loads the blocking rules from config.yml into the blockedPluginWorlds map.
     */
    private void loadConfiguration() {
        blockedPluginWorlds.clear();

        ConfigurationSection managedSection = getConfig().getConfigurationSection("managed_plugins");

        if (managedSection == null) {
            getLogger().warning("Configuration section 'managed_plugins' not found or is empty!");
            return;
        }

        Set<String> pluginNames = managedSection.getKeys(false);
        for (String pluginName : pluginNames) {
            String path = "managed_plugins." + pluginName;

            boolean enabled = getConfig().getBoolean(path + ".enabled", false);

            if (enabled) {
                List<String> worlds = getConfig().getStringList(path + ".blocked_worlds");
                if (!worlds.isEmpty()) {
                    // Store plugin name in lowercase for robust checking
                    blockedPluginWorlds.put(pluginName.toLowerCase(), worlds);
                    getLogger().info("Blocking rules loaded for plugin: " + pluginName +
                            " on worlds: " + String.join(", ", worlds));
                }
            }
        }
        getLogger().info("Plugin World Blocker loaded " + blockedPluginWorlds.size() + " blocking rules.");
    }

    /**
     * Checks if a specific plugin is configured to be blocked on the player's current world.
     * @param player The player performing the action.
     * @param pluginName The name of the plugin whose action might be blocked.
     * @return true if the plugin should be blocked in the player's world, false otherwise.
     */
    private boolean isBlockedOnWorld(Player player, String pluginName) {
        if (blockedPluginWorlds.isEmpty()) {
            return false;
        }

        String currentWorld = player.getWorld().getName();
        String pluginNameLower = pluginName.toLowerCase();

        // Check if we have blocking rules for this plugin
        if (blockedPluginWorlds.containsKey(pluginNameLower)) {
            List<String> blockedWorlds = blockedPluginWorlds.get(pluginNameLower);

            // Check if the current world is in the list of blocked worlds
            if (blockedWorlds.contains(currentWorld)) {
                return true;
            }
        }
        return false;
    }

    // --- COMMAND BLOCKING LOGIC (Using Reflection for Plugin Owner) ---

    /**
     * Intercepts player commands before they are processed.
     */
    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unchecked")
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().substring(1); // Removes the leading '/'
        String commandLabel = message.split(" ")[0].toLowerCase();

        Plugin owningPlugin = null;

        try {
            // 1. Get the CommandMap instance from the Server
            org.bukkit.Server server = Bukkit.getServer();
            Field commandMapField = server.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) commandMapField.get(server);

            // 2. Get the known commands map from the CommandMap
            Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);

            // 3. Find the command using the label (including aliases)
            Command command = knownCommands.get(commandLabel);

            // 4. Check if the found command is a PluginCommand to get its owner
            if (command instanceof PluginCommand) {
                owningPlugin = ((PluginCommand) command).getPlugin();
            } else if (command != null) {
                // Command is a generic command (e.g., /version) or server internal. Skip.
                return;
            }

        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Handle exceptions if the server's internal structure changes.
            // Log this warning sparingly as it pollutes the console.
            // getLogger().warning("Failed to resolve owning plugin for command: " + commandLabel + " - " + e.getMessage());
            return;
        }

        // We only care if the command is owned by a plugin we are managing
        if (owningPlugin == null) return;

        if (isBlockedOnWorld(player, owningPlugin.getName())) {

            // Command belongs to a managed plugin AND the player is on a blocked world. BLOCK!
            event.setCancelled(true);
            player.sendMessage(prefix + ChatColor.RED + "You cannot use commands from " +
                    owningPlugin.getName() + " in world '" + player.getWorld().getName() + "'.");
        }
    }

    // --- INTERACTION BLOCKING LOGIC ---

    /**
     * Blocks ALL player interactions (right/left-click) in worlds where any managed plugin
     * is configured to be blocked.
     * NOTE: This is an aggressive block used as a general safety measure against plugin features.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Iterate over all plugins we have blocking rules for
        for (String blockedPluginNameLower : blockedPluginWorlds.keySet()) {

            // Check if the player is in a blocked world for this plugin
            if (isBlockedOnWorld(player, blockedPluginNameLower)) {

                // Interaction belongs to a world blocked for this plugin. BLOCK!
                event.setCancelled(true);
                player.sendMessage(prefix + ChatColor.RED +
                        "Interactions related to managed plugins are blocked in world '" + player.getWorld().getName() + "'.");
                return; // Block and exit.
            }
        }
    }

    // Command for reloading the configuration
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("wblr")) {
            if (sender.hasPermission("worldpluginblocker.reload")) {
                reloadConfig();
                loadConfiguration();
                sender.sendMessage(prefix + ChatColor.GREEN + "Configuration reloaded successfully!");
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            }
            return true;
        }
        return false;
    }
}