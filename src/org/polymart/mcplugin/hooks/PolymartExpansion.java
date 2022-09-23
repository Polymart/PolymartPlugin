package org.polymart.mcplugin.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polymart.mcplugin.Main;

public class PolymartExpansion extends PlaceholderExpansion {

    private final Main plugin;

    public PolymartExpansion(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "polymart";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Polymart";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getServer().getVersion();
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        String[] args = params.split("_");

        if (args.length == 0) {
            return null;
        }

        switch (args[0].toLowerCase()) {
            case "version":
                if (args.length != 2) {
                    return null;
                }
                Plugin otherPlugin = plugin.getServer().getPluginManager().getPlugin(args[1]);
                if (otherPlugin == null) {
                    return null;
                }
                return otherPlugin.getDescription().getVersion();
            case "description":
                if (args.length != 2) {
                    return null;
                }
                Plugin otherPlugin1 = plugin.getServer().getPluginManager().getPlugin(args[1]);
                if (otherPlugin1 == null) {
                    return null;
                }
                return otherPlugin1.getDescription().getDescription();
            case "author":
                if (args.length != 2) {
                    return null;
                }
                Plugin otherPlugin2 = plugin.getServer().getPluginManager().getPlugin(args[1]);
                if (otherPlugin2 == null) {
                    return null;
                }
                return String.join(", ", otherPlugin2.getDescription().getAuthors());
        }

        return null;
    }
}
